package repository;

import java.sql.Connection;
import java.sql.SQLException;

final class Transactions {
    private Transactions() { }

    @FunctionalInterface
    interface Work<T> { T run(Connection connection) throws SQLException; }

    static <T> T run(ConnectionFactory connections, Work<T> work) throws SQLException {
        try (var connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
