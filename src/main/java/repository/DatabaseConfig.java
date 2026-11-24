package repository;

import java.sql.DriverManager;
import java.util.Map;

public record DatabaseConfig(String url, String user, String password) {
    public static DatabaseConfig fromEnvironment() {
        Map<String, String> env = System.getenv();
        String password = env.get("REPAIR_DB_PASSWORD");
        if (password == null) {
            throw new IllegalArgumentException("Set REPAIR_DB_PASSWORD before starting the application.");
        }
        return new DatabaseConfig(env.getOrDefault("REPAIR_DB_URL",
                "jdbc:mysql://localhost:3306/repair_shop?connectTimeout=5000&socketTimeout=15000"),
                env.getOrDefault("REPAIR_DB_USER", "repair_app"), password);
    }

    public ConnectionFactory connections() {
        return () -> DriverManager.getConnection(url, user, password);
    }

    // Never include credentials in logs or generated record toString().
    @Override public String toString() {
        return "DatabaseConfig[credentials redacted]";
    }
}
