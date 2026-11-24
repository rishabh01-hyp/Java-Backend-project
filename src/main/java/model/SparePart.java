package model;

/** Immutable stock record; only the service publishes stock changes. */
public record SparePart(String id, String name, int quantity) {
    public SparePart {
        id = Validation.id(id, "Part ID");
        name = Validation.text(name, "Part name", 255);
        if (quantity < 0) {
            throw new IllegalArgumentException("Starting stock must be zero or greater.");
        }
    }
}
