import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Case changes must not follow the server's locale: in Turkish "pink".toUpperCase()
 * is "PİNK", so enum lookups fail and config values are silently ignored.
 * Use java.util.Locale.ROOT.
 */
class LocaleCaseGuardTest {

    @Test
    @DisplayName("no upper/lower case change depends on the system locale")
    void caseDoesNotFollowTheSystemLocale() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Paths.get("src", "main", "java"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList())) {
                String code = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                if (code.contains(".toUpperCase()") || code.contains(".toLowerCase()")) offenders.add(p.toString());
            }
        }
        assertTrue(offenders.isEmpty(),
            "toUpperCase()/toLowerCase() without a Locale (use java.util.Locale.ROOT) in: " + offenders);
    }
}
