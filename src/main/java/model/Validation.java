package model;

/** Shared invariants, checked for user input and loaded records alike. */
public final class Validation {
    private Validation() { }

    public static String id(String value, String label) {
        value = text(value, label, 64);
        if (!value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(label + " must contain only letters, digits, underscores or hyphens.");
        }
        return value.toUpperCase(java.util.Locale.ROOT);
    }

    public static String text(String value, String label) {
        return text(value, label, 10000);
    }

    public static String text(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
        value = value.strip();
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + " must be at most " + maxLength + " characters.");
        }
        return value;
    }
}
