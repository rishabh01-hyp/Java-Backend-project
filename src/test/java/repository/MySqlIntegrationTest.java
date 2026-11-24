package repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import service.RepairShopService;

import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: point REPAIR_TEST_MYSQL_URL at a dedicated test database, never production. */
@EnabledIfEnvironmentVariable(named = "REPAIR_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
class MySqlIntegrationTest {
    @Test void mysqlRowLocksPreventOversellingAndReloadCommittedState() throws Exception {
        ConnectionFactory connections = () -> DriverManager.getConnection(System.getenv("REPAIR_TEST_MYSQL_URL"),
                System.getenv("REPAIR_TEST_MYSQL_USER"), System.getenv("REPAIR_TEST_MYSQL_PASSWORD"));
        JdbcSchema.initialize(connections);
        var first = new RepairShopService(connections);
        var second = new RepairShopService(connections);
        String suffix = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String part = "P" + suffix;
        String job = "J" + suffix;
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            first.addPart(part, "Integration test port", 1);
            first.createJob(job, "Test phone", "Test issue");
            var a = executor.submit(() -> { start.await(); return claim(first, job, part); });
            var b = executor.submit(() -> { start.await(); return claim(second, job, part); });
            start.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(0, second.findPart(part).quantity());
            assertEquals(1, second.findJob(job).usages().size());
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(15, TimeUnit.SECONDS);
            try (var connection = connections.open()) {
                for (String sql : new String[]{"DELETE FROM part_usage WHERE job_id = ?",
                        "DELETE FROM repair_jobs WHERE id = ?", "DELETE FROM spare_parts WHERE id = ?"}) {
                    try (var statement = connection.prepareStatement(sql)) {
                        statement.setString(1, sql.contains("spare_parts") ? part : job);
                        statement.executeUpdate();
                    }
                }
            }
        }
    }

    private boolean claim(RepairShopService shop, String job, String part) throws Exception {
        try { shop.recordUsage(job, part, 1); return true; }
        catch (IllegalArgumentException e) { return false; }
    }
}
