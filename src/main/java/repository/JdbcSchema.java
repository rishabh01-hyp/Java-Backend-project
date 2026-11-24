package repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;

public final class JdbcSchema {
    private JdbcSchema() { }

    /** Non-destructive initialization; MySQL must use transactional InnoDB tables. */
    public static void initialize(ConnectionFactory connections) throws SQLException, IOException {
        String sql;
        try (var stream = Objects.requireNonNull(JdbcSchema.class.getResourceAsStream("/db/schema.sql"))) {
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (var connection = connections.open(); var statement = connection.createStatement()) {
            boolean mysql = connection.getMetaData().getDatabaseProductName().equals("MySQL");
            for (String command : sql.split(";")) {
                if (!command.isBlank()) {
                    statement.execute(command + (mysql
                            ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin" : ""));
                }
            }
            if (mysql) {
                try (var tables = statement.executeQuery("SELECT ENGINE FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('spare_parts','repair_jobs','part_usage')")) {
                    while (tables.next()) {
                        if (!"InnoDB".equalsIgnoreCase(tables.getString(1))) {
                            throw new SQLException("Repair shop tables must use InnoDB for transactions.");
                        }
                    }
                }
            }
        }
    }
}
