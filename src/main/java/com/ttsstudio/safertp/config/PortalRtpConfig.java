package com.ttsstudio.safertp.config;

/**
 * One entry of {@code worlds.yml}'s {@code portal-rtp.worlds} section: redirects a vanilla
 * Nether portal used from {@code sourceWorld} to a random safe spot in {@code targetWorld}
 * instead of the portal's own linked destination.
 */
public record PortalRtpConfig(String sourceWorld, String targetWorld, long cooldownSeconds) {}
