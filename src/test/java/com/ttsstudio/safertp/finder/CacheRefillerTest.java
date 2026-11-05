package com.ttsstudio.safertp.finder;

import com.ttsstudio.safertp.config.WorldConfig;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CacheRefillerTest {

    private static WorldConfig world(String name) {
        return new WorldConfig(name, true, 100, 1000, 0, 0, Set.of(), Set.of(), 50, 0, 0);
    }

    /** Searches that stay open until the test completes them. */
    private static final class PendingSearches {
        final List<CompletableFuture<Location>> started = new ArrayList<>();

        CompletableFuture<Location> start(WorldConfig config) {
            CompletableFuture<Location> f = new CompletableFuture<>();
            started.add(f);
            return f;
        }
    }

    @Test
    void slowSearches_neverExceedCacheSize() {
        LocationCache cache = new LocationCache(6, 2);
        PendingSearches searches = new PendingSearches();
        CacheRefiller refiller = new CacheRefiller(() -> cache, () -> List.of(world("world")),
            () -> 6, searches::start);

        // 10 seconds of ticks while every chunk load is still pending
        for (int i = 0; i < 200; i++) refiller.tick();

        assertEquals(6, searches.started.size());
    }

    @Test
    void finishedSearch_freesItsSlot() {
        LocationCache cache = new LocationCache(2, 1);
        PendingSearches searches = new PendingSearches();
        CacheRefiller refiller = new CacheRefiller(() -> cache, () -> List.of(world("world")),
            () -> 2, searches::start, () -> 0L, 0L);

        for (int i = 0; i < 20; i++) refiller.tick();
        assertEquals(2, searches.started.size());

        searches.started.get(0).complete(new Location(null, 1, 64, 1));
        for (int i = 0; i < 20; i++) refiller.tick();
        // one location cached + one still searching = full
        assertEquals(2, searches.started.size());
        assertEquals(1, cache.size("world"));

        searches.started.get(1).completeExceptionally(new NoSafeLocationException("world", 50));
        for (int i = 0; i < 20; i++) refiller.tick();
        assertEquals(3, searches.started.size());
    }

    @Test
    void worldWithoutSafeSpots_waitsBeforeSearchingAgain() {
        LocationCache cache = new LocationCache(6, 2);
        PendingSearches searches = new PendingSearches();
        AtomicLong now = new AtomicLong(0);
        CacheRefiller refiller = new CacheRefiller(() -> cache, () -> List.of(world("void")),
            () -> 6, searches::start, now::get, 300_000L);

        for (int i = 0; i < 20; i++) refiller.tick();
        searches.started.forEach(f -> f.completeExceptionally(new NoSafeLocationException("void", 50)));
        int afterFailures = searches.started.size();

        now.addAndGet(60_000);
        for (int i = 0; i < 200; i++) refiller.tick();
        assertEquals(afterFailures, searches.started.size());

        now.addAndGet(300_000);
        refiller.tick();
        assertEquals(afterFailures + 1, searches.started.size());
    }

    @Test
    void worldThatIsNotLoaded_doesNotBlockTheOthers() {
        LocationCache cache = new LocationCache(1, 1);
        PendingSearches searches = new PendingSearches();
        CacheRefiller refiller = new CacheRefiller(() -> cache,
            () -> List.of(world("unloaded"), world("world")), () -> 1,
            wc -> wc.worldName().equals("unloaded") ? null : searches.start(wc));

        for (int i = 0; i < 4; i++) refiller.tick();
        assertEquals(1, searches.started.size());
    }
}
