package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import support.TestDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class PartRequestProcessorTest {
    @Test void queuedWorkersCannotOversell() throws Exception {
        var db = new TestDatabase();
        db.shop.addPart("P1", "Port", 10);
        db.shop.createJob("J1", "Phone", "Issue");
        var results = new ArrayList<PartRequestProcessor.Submission>();
        try (var requests = new PartRequestProcessor(db.shop::recordUsage, 4, 100)) {
            for (int i = 0; i < 50; i++) { results.add(requests.submit("J1", "P1", 1)); }
        } // close must finish every accepted request.
        int successes = 0;
        int failures = 0;
        for (var result : results) {
            try { result.result().toCompletableFuture().join(); successes++; }
            catch (CompletionException e) { assertInstanceOf(IllegalArgumentException.class, e.getCause()); failures++; }
        }
        assertEquals(10, successes);
        assertEquals(40, failures);
        assertEquals(0, db.shop.findPart("P1").quantity());
        assertEquals(10, db.shop.findJob("J1").usages().size());
    }

    @Test void databaseLockProtectsSeparateServiceInstances() throws Exception {
        var db = new TestDatabase();
        db.shop.addPart("P1", "Port", 1);
        db.shop.createJob("J1", "Phone", "Issue");
        var other = new RepairShopService(db.connections);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = () -> { start.await(); return claim(db.shop); };
            Callable<Boolean> second = () -> { start.await(); return claim(other); };
            var a = executor.submit(first);
            var b = executor.submit(second);
            start.countDown();
            assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
            assertEquals(0, db.shop.findPart("P1").quantity());
            assertEquals(1, db.shop.findJob("J1").usages().size());
        } finally { start.countDown(); executor.shutdownNow(); }
    }

    private boolean claim(RepairShopService shop) throws SQLException {
        try { shop.recordUsage("J1", "P1", 1); return true; }
        catch (IllegalArgumentException e) { return false; }
    }

    @Test void boundedQueueRejectsWithoutLosingAcceptedRequests() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var requests = new PartRequestProcessor((job, part, qty) -> {
            started.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) { throw new SQLException("Timed out in test"); } }
            catch (InterruptedException e) { throw new SQLException(e); }
            return 0;
        }, 1, 1);
        try {
            var first = requests.submit("J1", "P1", 1);
            assertTrue(started.await(5, TimeUnit.SECONDS));
            var second = requests.submit("J1", "P1", 1);
            assertThrows(RejectedExecutionException.class, () -> requests.submit("J1", "P1", 1));
            release.countDown();
            requests.close();
            assertEquals(0, first.result().toCompletableFuture().join());
            assertEquals(0, second.result().toCompletableFuture().join());
            assertThrows(RejectedExecutionException.class, () -> requests.submit("J1", "P1", 1));
        } finally { release.countDown(); requests.close(); }
    }

    @Test void failedRequestDoesNotKillConsumer() {
        var calls = new AtomicInteger();
        PartRequestProcessor.Submission first;
        PartRequestProcessor.Submission second;
        try (var requests = new PartRequestProcessor((j, p, q) -> {
            if (calls.incrementAndGet() == 1) { throw new SQLException("Simulated outage"); }
            return 7;
        }, 1, 2)) {
            first = requests.submit("J1", "P1", 1);
            second = requests.submit("J1", "P1", 1);
            assertThrows(IllegalArgumentException.class, () -> requests.submit("J1", "P1", 0));
        }
        assertInstanceOf(SQLException.class, assertThrows(CompletionException.class,
                () -> first.result().toCompletableFuture().join()).getCause());
        assertEquals(7, second.result().toCompletableFuture().join());
    }
}
