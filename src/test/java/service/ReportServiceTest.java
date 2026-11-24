package service;

import model.UsageReportRow;
import org.junit.jupiter.api.Test;
import support.TestDatabase;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {
    @Test void lowStockIncludesThresholdAndSortsByQuantityThenId() throws Exception {
        var db = new TestDatabase();
        db.shop.addPart("P3", "Battery", 5);
        db.shop.addPart("P2", "Port", 1);
        db.shop.addPart("P1", "Cable", 0);
        var reports = new ReportService(db.shop);
        assertEquals(List.of("P1", "P2"), reports.lowStock(1).stream().map(p -> p.id()).toList());
        assertThrows(IllegalArgumentException.class, () -> reports.lowStock(-1));
    }

    @Test void repeatedUsageIsAggregatedByJobAndPart() throws Exception {
        var db = new TestDatabase();
        db.seed();
        db.shop.recordUsage("J101", "P101", 1);
        db.shop.recordUsage("J101", "P101", 1);
        assertEquals(List.of(new UsageReportRow("J101", "P101", 2)), new ReportService(db.shop).usageByJob());
    }
}
