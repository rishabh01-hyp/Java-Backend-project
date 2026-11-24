package repository;

import model.SparePart;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcPartRepository implements Repository<SparePart, String> {
    private final ConnectionFactory connections;

    public JdbcPartRepository(ConnectionFactory connections) { this.connections = connections; }

    @Override public Optional<SparePart> findById(String id) throws SQLException {
        try (var connection = connections.open();
             var query = connection.prepareStatement("SELECT id, name, quantity FROM spare_parts WHERE id = ?")) {
            query.setString(1, id);
            try (var rows = query.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        }
    }

    @Override public List<SparePart> findAll() throws SQLException {
        var parts = new ArrayList<SparePart>();
        try (var connection = connections.open();
             var query = connection.prepareStatement("SELECT id, name, quantity FROM spare_parts ORDER BY id");
             var rows = query.executeQuery()) {
            while (rows.next()) { parts.add(read(rows)); }
        }
        return List.copyOf(parts);
    }

    @Override public void add(SparePart part) throws SQLException {
        try (var connection = connections.open()) { insert(connection, part); }
    }

    static void insert(Connection connection, SparePart part) throws SQLException {
        try (var query = connection.prepareStatement("INSERT INTO spare_parts (id, name, quantity) VALUES (?, ?, ?)")) {
            query.setString(1, part.id());
            query.setString(2, part.name());
            query.setInt(3, part.quantity());
            query.executeUpdate();
        }
    }

    static SparePart read(ResultSet rows) throws SQLException {
        return new SparePart(rows.getString("id"), rows.getString("name"), rows.getInt("quantity"));
    }
}
