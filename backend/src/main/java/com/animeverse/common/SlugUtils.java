package com.animeverse.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String fromName(String name, String fallbackPrefix) {
        String normalized = name == null
                ? ""
                : name.trim().toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");

        if (normalized.length() >= 2) {
            return truncate(normalized, 50);
        }

        String source = name == null ? "" : name.trim();
        return fallbackPrefix + "-" + digest(source).substring(0, 10);
    }

    public static String normalizeExternalSlug(String value, String name, String fallbackPrefix) {
        if (value == null || value.isBlank()) {
            return fromName(name, fallbackPrefix);
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z0-9-]{2,50}$")) {
            throw BusinessException.badRequest("slug/code 只能包含小写字母、数字和连字符");
        }
        return normalized;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength).replaceAll("-+$", "");
    }

    private static String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
