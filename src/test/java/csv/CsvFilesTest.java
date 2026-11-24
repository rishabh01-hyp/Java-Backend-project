package csv;

import model.SparePart;
import model.UsageReportRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.TestDatabase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CsvFilesTest {
    @TempDir Path directory;

    @Test void inventoryRoundTripsQuotesCommasUnicodeAndMultilineNames() throws Exception {
        Path path = directory.resolve("nested/inventory.csv");
        var parts = List.of(new SparePart("P1", "USB, \"C\"\nपोर्ट", 2), new SparePart("P2", "Battery", 0));
        CsvFiles.writeInventory(path, parts);
        assertEquals(parts, CsvFiles.readInventory(path));
        CsvFiles.writeInventory(path, List.of());
        assertTrue(CsvFiles.readInventory(path).isEmpty());
    }

    @Test void acceptsUtf8Bom() throws Exception {
        Path path = directory.resolve("bom.csv");
        Files.writeString(path, "\uFEFFid,name,quantity\r\nP1,Port,2\r\n");
        assertEquals(2, CsvFiles.readInventory(path).get(0).quantity());
    }

    @Test void invalidHeadersFieldsNumbersAndMalformedQuotesAreRejected() throws Exception {
        Path path = directory.resolve("bad.csv");
        for (String content : List.of("name,id,quantity\nPort,P1,1\n", "id,name,quantity\nP1,Port\n",
                "id,name,quantity\nP1,Port,-1\n", "id,name,quantity\nP1,Port,999999999999\n")) {
            Files.writeString(path, content);
            assertThrows(IllegalArgumentException.class, () -> CsvFiles.readInventory(path));
        }
        Files.writeString(path, "id,name,quantity\nP1,\"unclosed,1\n");
        assertThrows(IOException.class, () -> CsvFiles.readInventory(path));
    }

    @Test void invalidLastRecordPreventsAnyImport() throws Exception {
        var db = new TestDatabase();
        Path path = directory.resolve("bad.csv");
        Files.writeString(path, "id,name,quantity\nP1,Port,1\nP2,Battery,nope\n");
        assertThrows(IllegalArgumentException.class, () -> db.shop.importParts(CsvFiles.readInventory(path)));
        assertTrue(db.shop.listParts().isEmpty());
    }

    @Test void usageExportHasHeaderAndTotals() throws Exception {
        Path path = directory.resolve("usage.csv");
        CsvFiles.writeUsage(path, List.of(new UsageReportRow("J1", "P1", 3)));
        assertEquals("job_id,part_id,quantity_used\r\nJ1,P1,3\r\n", Files.readString(path));
    }

    @Test void failedReplacementDoesNotDestroyDestinationAndCleansTemporaryFile() throws Exception {
        Path path = directory.resolve("occupied");
        Files.createDirectory(path);
        Files.writeString(path.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> CsvFiles.writeInventory(path, List.of()));
        assertEquals("keep", Files.readString(path.resolve("keep.txt")));
        try (var entries = Files.list(directory)) { assertEquals(1, entries.count()); }
    }
}
