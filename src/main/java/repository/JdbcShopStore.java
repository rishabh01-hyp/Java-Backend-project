package repository;

import model.PartUsage;
import model.SparePart;
import model.Validation;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

/** Cross-repository operations belong in a single database transaction. */
public final class JdbcShopStore {
    private final ConnectionFactory connections;
    private final Object[] stockLocks = new Object[64];

    public JdbcShopStore(ConnectionFactory connections) {
        this.connections = connections;
        for (int i = 0; i < stockLocks.length; i++) { stockLocks[i] = new Object(); }
    }

    public int recordUsage(String jobId, String partId, int quantity) throws SQLException {
        String job = Validation.id(jobId, "Job ID");
        var usage = new PartUsage(partId, quantity);
        // Bounded lock striping: local workers cannot claim the same stock concurrently.
        Object lock = stockLocks[Math.floorMod(usage.partId().hashCode(), stockLocks.length)];
        synchronized (lock) {
            return Transactions.run(connections, connection -> {
                try (var query = connection.prepareStatement("SELECT id FROM repair_jobs WHERE id = ?")) {
                    query.setString(1, job);
                    try (var rows = query.executeQuery()) {
                        if (!rows.next()) { throw new IllegalArgumentException("Unknown job ID: " + job); }
                    }
                }
                int available;
                // Database row locking also protects against other JVMs/store instances.
                try (var query = connection.prepareStatement("SELECT quantity FROM spare_parts WHERE id = ? FOR UPDATE")) {
                    query.setString(1, usage.partId());
                    try (var rows = query.executeQuery()) {
                        if (!rows.next()) { throw new IllegalArgumentException("Unknown part ID: " + usage.partId()); }
                        available = rows.getInt(1);
                    }
                }
                if (quantity > available) {
                    throw new IllegalArgumentException("Insufficient stock for " + usage.partId()
                            + ": requested " + quantity + ", available " + available + ".");
                }
                int remaining = available - quantity;
                try (var update = connection.prepareStatement("UPDATE spare_parts SET quantity = ? WHERE id = ?")) {
                    update.setInt(1, remaining);
                    update.setString(2, usage.partId());
                    if (update.executeUpdate() != 1) { throw new SQLException("Stock record disappeared."); }
                }
                try (var insert = connection.prepareStatement(
                        "INSERT INTO part_usage (job_id, part_id, quantity) VALUES (?, ?, ?)")) {
                    insert.setString(1, job);
                    insert.setString(2, usage.partId());
                    insert.setInt(3, quantity);
                    insert.executeUpdate();
                }
                return remaining;
            });
        }
    }

    /** Import adds new IDs only, and rolls back the entire batch on any error. */
    public int importParts(List<SparePart> input) throws SQLException {
        var parts = List.copyOf(input);
        var ids = new HashSet<String>();
        for (var part : parts) {
            if (!ids.add(part.id())) { throw new IllegalArgumentException("Duplicate part ID in import: " + part.id()); }
        }
        return Transactions.run(connections, connection -> {
            for (var part : parts) { JdbcPartRepository.insert(connection, part); }
            return parts.size();
        });
    }
}
