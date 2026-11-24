package service;

import model.PartUsage;
import model.SparePart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.ConnectionFactory;
import support.TestDatabase;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepairShopServiceTest {
    private TestDatabase db;
    private RepairShopService shop;

    @BeforeEach void setup() throws Exception { db = new TestDatabase(); shop = db.shop; }

    @Test void acceptanceAndReloadFromNewConnections() throws Exception {
        db.seed();
        assertEquals(1, shop.recordUsage("J101", "P101", 1));
        assertThrows(IllegalArgumentException.class, () -> shop.recordUsage("J102", "P101", 2));
        var reloaded = new RepairShopService(db.connections);
        assertEquals(1, reloaded.findPart("P101").quantity());
        assertEquals(List.of(new PartUsage("P101", 1)), reloaded.findJob("J101").usages());
        assertTrue(reloaded.findJob("J102").usages().isEmpty());
    }

    @Test void invalidUsageKeepsBothRecordsUnchanged() throws Exception {
        db.seed();
        for (int quantity : new int[]{0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> shop.recordUsage("J101", "P101", quantity));
        }
        assertThrows(IllegalArgumentException.class, () -> shop.recordUsage("missing", "P101", 1));
        assertThrows(IllegalArgumentException.class, () -> shop.recordUsage("J101", "missing", 1));
        assertEquals(2, shop.findPart("P101").quantity());
        assertTrue(shop.findJob("J101").usages().isEmpty());
    }

    @Test void exactStockAndSeparateActions() throws Exception {
        db.seed();
        assertEquals(1, shop.recordUsage("J101", "P101", 1));
        assertEquals(0, shop.recordUsage("J101", "P101", 1));
        assertEquals(2, shop.findJob("J101").usages().size());
        assertThrows(IllegalArgumentException.class, () -> shop.recordUsage("J101", "P101", 1));
    }

    @Test void duplicateAndInvalidFields() throws Exception {
        db.seed();
        assertThrows(IllegalArgumentException.class, () -> shop.addPart(" p101 ", "Other", 5));
        assertThrows(IllegalArgumentException.class, () -> shop.createJob("J101", "Other", "Issue"));
        assertThrows(IllegalArgumentException.class, () -> shop.addPart(" ", "Other", 5));
        assertThrows(IllegalArgumentException.class, () -> shop.addPart("P2", " ", 5));
        assertThrows(IllegalArgumentException.class, () -> shop.addPart("P2", "Other", -1));
        assertThrows(IllegalArgumentException.class, () -> shop.createJob("J2", " ", "Issue"));
        assertThrows(IllegalArgumentException.class, () -> shop.createJob("J2", "Phone", " "));
        assertThrows(IllegalArgumentException.class, () -> shop.addPart("P2", "x".repeat(256), 1));
        assertThrows(IllegalArgumentException.class, () -> shop.addPart("bad,id", "Port", 1));
        assertEquals(1, shop.listParts().size());
    }

    @Test void unicodeAndZeroStockRoundTrip() throws Exception {
        shop.addPart("P0", "पोर्ट | USB\nC", 0);
        shop.createJob("J1", "फोन", "चार्ज नहीं होता");
        assertEquals("पोर्ट | USB\nC", shop.findPart("p0").name());
        assertEquals("फोन", shop.findJob("J1").device());
        assertThrows(IllegalArgumentException.class, () -> shop.recordUsage("J1", "P0", 1));
    }

    @Test void collectionsCannotMutatePersistentState() throws Exception {
        db.seed();
        assertThrows(UnsupportedOperationException.class, () -> shop.listParts().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> shop.findJob("J101").usages().add(new PartUsage("P101", 1)));
    }

    @Test void failedUsageInsertRollsBackAlreadyExecutedStockDeduction() throws Exception {
        db.seed();
        // Deliberately fail the second write, after the UPDATE has executed.
        try (var connection = db.connections.open(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE part_usage ADD CONSTRAINT test_limit CHECK (quantity < 2)");
        }
        assertThrows(SQLException.class, () -> shop.recordUsage("J101", "P101", 2));
        assertEquals(2, shop.findPart("P101").quantity());
        assertTrue(shop.findJob("J101").usages().isEmpty());
    }

    @Test void importIsAllOrNothingAndNeverOverwritesExistingStock() throws Exception {
        db.seed();
        assertThrows(IllegalArgumentException.class, () -> shop.importParts(List.of(
                new SparePart("P200", "Battery", 3), new SparePart("P101", "Duplicate", 99))));
        assertThrows(IllegalArgumentException.class, () -> shop.findPart("P200"));
        assertEquals(2, shop.findPart("P101").quantity());
        assertThrows(IllegalArgumentException.class, () -> shop.importParts(List.of(
                new SparePart("P200", "Battery", 3), new SparePart("p200", "Duplicate", 1))));
        assertEquals(1, shop.importParts(List.of(new SparePart("P200", "Battery", 3))));
    }

    @Test void storageErrorsPropagateWithoutSuccess() {
        ConnectionFactory broken = () -> { throw new SQLException("Simulated outage", "08001"); };
        var unavailable = new RepairShopService(broken);
        assertThrows(SQLException.class, () -> unavailable.addPart("P1", "Port", 1));
        assertThrows(SQLException.class, () -> unavailable.recordUsage("J1", "P1", 1));
    }
}
