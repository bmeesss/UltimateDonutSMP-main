package com.bx.ultimateDonutSmp.utils;

import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.Permissible;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class PermissionUtils {

    private static final Charset WINDOWS_1252 = Charset.forName("Windows-1252");

    private PermissionUtils() {
    }

    public static boolean isTemporaryPlayer(Permissible permissible) {
        if (permissible == null) {
            return false;
        }
        return permissible.getClass().getName().contains("TemporaryPlayer");
    }

    public static boolean has(Permissible permissible, String permission) {
        if (permissible == null || permission == null || permission.trim().isEmpty() || isTemporaryPlayer(permissible)) {
            return false;
        }

        try {
            String normalized = normalizePermissionNode(permission);
            if (permissible.hasPermission(permission)) {
                return true;
            }
            if (!normalized.equals(permission) && permissible.hasPermission(normalized)) {
                return true;
            }

            return hasEffectivePermissionAlias(permissible, normalized);
        } catch (UnsupportedOperationException ignored) {
            return false;
        }
    }

    public static boolean hasOrUnset(Permissible permissible, String permission) {
        return permission == null || permission.trim().isEmpty() || has(permissible, permission);
    }

    public static boolean hasAny(Permissible permissible, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        for (String permission : permissions) {
            if (has(permissible, permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasExact(Permissible permissible, String permission) {
        if (permissible == null || permission == null || permission.trim().isEmpty() || isTemporaryPlayer(permissible)) {
            return false;
        }

        try {
            String normalized = normalizePermissionNode(permission);
            boolean matchedTrue = false;
            for (PermissionAttachmentInfo info : permissible.getEffectivePermissions()) {
                String normalizedGranted = normalizePermissionNode(info.getPermission());
                if (!normalizedGranted.equals(normalized)) {
                    continue;
                }
                if (!info.getValue()) {
                    return false;
                }
                matchedTrue = true;
            }
            return matchedTrue;
        } catch (UnsupportedOperationException ignored) {
            return false;
        }
    }

    public static int resolveHighestExactNumberedPermission(Permissible permissible, String prefix, int maxValue) {
        if (permissible == null || prefix == null || prefix.trim().isEmpty() || maxValue < 1) {
            return 0;
        }

        String normalizedPrefix = normalizePermissionNode(prefix);
        for (int value = maxValue; value >= 1; value--) {
            if (hasExact(permissible, normalizedPrefix + value)) {
                return value;
            }
        }
        return 0;
    }

    public static String normalizePermissionNode(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return "";
        }

        String value = normalizeRawPermissionNode(permission.trim().toLowerCase(Locale.ROOT));
        String repaired = normalizeRawPermissionNode(repairMojibake(permission.trim()).toLowerCase(Locale.ROOT));
        if (scoreNormalizedPermission(repaired) < scoreNormalizedPermission(value)) {
            return repaired;
        }
        return value;
    }

    private static String normalizeRawPermissionNode(String value) {
        StringBuilder normalized = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> normalized.appendCodePoint(normalizeCodePoint(codePoint)));
        return normalized.toString();
    }

    private static String repairMojibake(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        try {
            return new String(value.getBytes(WINDOWS_1252), StandardCharsets.UTF_8);
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    private static int scoreNormalizedPermission(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }

        int score = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if ((character >= 'a' && character <= 'z')
                    || (character >= '0' && character <= '9')
                    || character == '.'
                    || character == '-'
                    || character == '_'
                    || character == '*') {
                continue;
            }
            score++;
        }
        return score;
    }

    private static boolean hasEffectivePermissionAlias(Permissible permissible, String normalizedPermission) {
        boolean matchedTrue = false;
        for (PermissionAttachmentInfo info : permissible.getEffectivePermissions()) {
            String normalizedGranted = normalizePermissionNode(info.getPermission());
            if (!matches(normalizedGranted, normalizedPermission)) {
                continue;
            }
            if (!info.getValue()) {
                return false;
            }
            matchedTrue = true;
        }
        return matchedTrue;
    }

    private static boolean matches(String granted, String requested) {
        if (granted.equals(requested) || granted.equals("*")) {
            return true;
        }
        if (!granted.endsWith(".*")) {
            return false;
        }
        String prefix = granted.substring(0, granted.length() - 1);
        return requested.startsWith(prefix);
    }

    private static int normalizeCodePoint(int codePoint) {
        switch (codePoint) {
            case '\u1D00':
            case '\u0430':
                return 'a';
            case '\u0299':
                return 'b';
            case '\u1D04':
            case '\u0441':
                return 'c';
            case '\u1D05':
                return 'd';
            case '\u1D07':
            case '\u0435':
                return 'e';
            case '\u0493':
            case '\uA730':
                return 'f';
            case '\u0262':
            case '\u0261':
                return 'g';
            case '\u029C':
                return 'h';
            case '\u026A':
            case '\u0456':
                return 'i';
            case '\u1D0A':
            case '\u0458':
                return 'j';
            case '\u1D0B':
                return 'k';
            case '\u029F':
                return 'l';
            case '\u1D0D':
                return 'm';
            case '\u0274':
                return 'n';
            case '\u1D0F':
            case '\u043E':
                return 'o';
            case '\u1D18':
            case '\u0440':
                return 'p';
            case '\u01EB':
                return 'q';
            case '\u0280':
                return 'r';
            case '\u0455':
                return 's';
            case '\u1D1B':
                return 't';
            case '\u1D1C':
                return 'u';
            case '\u1D20':
                return 'v';
            case '\u1D21':
                return 'w';
            case '\u0445':
                return 'x';
            case '\u028F':
            case '\u0443':
                return 'y';
            case '\u1D22':
                return 'z';
            case '\u00A0':
            case '\u2007':
            case '\u202F':
                return ' ';
            default:
                return codePoint;
        }
    }
}
