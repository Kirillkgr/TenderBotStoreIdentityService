package kirillzhdanov.identityservice.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public static String nullIfBlank(String v) {
        return isBlank(v) ? null : v;
    }

    public static String normalizeSpaces(String v) {
        if (v == null) return null;
        // collapse multiple whitespace characters into single space and trim
        return v.trim().replaceAll("\\s+", " ");
    }

    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    public static boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null) return false;
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }
}
