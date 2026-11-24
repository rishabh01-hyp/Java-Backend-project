package model;

/** One usage action, rather than an aggregated total. */
public record PartUsage(String partId, int quantity) {
    public PartUsage {
        partId = Validation.id(partId, "Part ID");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Usage quantity must be positive.");
        }
    }
}
