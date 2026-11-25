import java.sql.*;
import java.util.*;

/** JDBC only: one connection, with synchronized methods for safe thread access. */
class Database implements AutoCloseable {
    private final Connection connection;

    public Database() throws SQLException {
        Map<String, String> env = System.getenv();
        String password = env.get("REPAIR_DB_PASSWORD");
        if (password == null) throw new ShopException("Set REPAIR_DB_PASSWORD first.");
        connection = DriverManager.getConnection(
                env.getOrDefault("REPAIR_DB_URL", "jdbc:mysql://localhost:3306/repair_shop?connectTimeout=5000&socketTimeout=15000"),
                env.getOrDefault("REPAIR_DB_USER", "repair_app"), password);
        try (Statement statement = connection.createStatement()) {
            String suffix = " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin";
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS spare_parts ("
                    + "id VARCHAR(64) PRIMARY KEY, name VARCHAR(255) NOT NULL, "
                    + "quantity INT NOT NULL CHECK(quantity >= 0))" + suffix);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS repair_jobs ("
                    + "id VARCHAR(64) PRIMARY KEY, device VARCHAR(255) NOT NULL, issue TEXT NOT NULL)" + suffix);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS part_usage ("
                    + "usage_id BIGINT AUTO_INCREMENT PRIMARY KEY, job_id VARCHAR(64) NOT NULL, "
                    + "part_id VARCHAR(64) NOT NULL, quantity INT NOT NULL CHECK(quantity > 0), "
                    + "FOREIGN KEY(job_id) REFERENCES repair_jobs(id), "
                    + "FOREIGN KEY(part_id) REFERENCES spare_parts(id))" + suffix);
            try (ResultSet rows = statement.executeQuery("SELECT ENGINE FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('spare_parts','repair_jobs','part_usage')")) {
                while (rows.next()) {
                    if (!"InnoDB".equalsIgnoreCase(rows.getString(1))) throw new SQLException("Tables must use InnoDB.");
                }
            }
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }

    public synchronized List<Part> parts() throws SQLException {
        List<Part> parts = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id,name,quantity FROM spare_parts")) {
            while (rows.next()) parts.add(new Part(rows.getString(1), rows.getString(2), rows.getInt(3)));
        }
        Collections.sort(parts);
        return parts;
    }

    public synchronized void addParts(List<Part> parts) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement query = connection.prepareStatement("INSERT INTO spare_parts VALUES (?,?,?)")) {
            for (Part part : parts) {
                query.setString(1, part.getId());
                query.setString(2, part.getName());
                query.setInt(3, part.getQuantity());
                query.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public synchronized void addJob(RepairJob job) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("INSERT INTO repair_jobs VALUES (?,?,?)")) {
            query.setString(1, job.getId());
            query.setString(2, job.getDevice());
            query.setString(3, job.getIssue());
            query.executeUpdate();
        }
    }

    public synchronized RepairJob job(String id) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT device,issue FROM repair_jobs WHERE id=?")) {
            query.setString(1, id);
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) throw new ShopException("Unknown job: " + id);
                return new RepairJob(id, rows.getString(1), rows.getString(2));
            }
        }
    }

    public synchronized int usePart(String job, String part, int quantity) throws SQLException {
        Entity.positive(quantity);
        connection.setAutoCommit(false);
        try {
            int available;
            try (PreparedStatement query = connection.prepareStatement("SELECT quantity FROM spare_parts WHERE id=? FOR UPDATE")) {
                query.setString(1, part);
                try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next()) throw new ShopException("Unknown part: " + part);
                    available = rows.getInt(1);
                }
            }
            if (quantity > available) throw new ShopException("Insufficient stock: " + available);
            try (PreparedStatement query = connection.prepareStatement("UPDATE spare_parts SET quantity=? WHERE id=?")) {
                query.setInt(1, available - quantity);
                query.setString(2, part);
                query.executeUpdate();
            }
            try (PreparedStatement query = connection.prepareStatement("INSERT INTO part_usage(job_id,part_id,quantity) VALUES (?,?,?)")) {
                query.setString(1, job);
                query.setString(2, part);
                query.setInt(3, quantity);
                query.executeUpdate();
            }
            connection.commit();
            return available - quantity;
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public synchronized List<String[]> usageRows() throws SQLException {
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{"job_id", "part_id", "quantity"});
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT job_id,part_id,SUM(quantity) FROM part_usage "
                     + "GROUP BY job_id,part_id ORDER BY job_id,part_id")) {
            while (rows.next()) result.add(new String[]{rows.getString(1), rows.getString(2), rows.getString(3)});
        }
        return result;
    }

    public synchronized void restock(String id, int amount) throws SQLException {
        Entity.positive(amount);
        try (PreparedStatement query = connection.prepareStatement("UPDATE spare_parts SET quantity=quantity+? WHERE id=? AND quantity<=?")) {
            query.setInt(1, amount);
            query.setString(2, id);
            query.setInt(3, Integer.MAX_VALUE - amount);
            if (query.executeUpdate() != 1) throw new ShopException("Unknown part or stock exceeds integer range.");
        }
    }

    public synchronized void deletePart(String id) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("DELETE FROM spare_parts WHERE id=?")) {
            query.setString(1, id);
            if (query.executeUpdate() != 1) throw new ShopException("Unknown part: " + id);
        }
    }

    @Override public synchronized void close() throws SQLException { connection.close(); }
}
