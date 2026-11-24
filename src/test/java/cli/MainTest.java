package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.PartRequestProcessor;
import service.RepairShopService;
import support.TestDatabase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @TempDir Path directory;

    private String run(RepairShopService shop, String input) {
        var bytes = new ByteArrayOutputStream();
        try (var requests = new PartRequestProcessor(shop::recordUsage, 2, 100)) {
            new ConsoleMenu(shop, requests, new Scanner(input), new PrintStream(bytes, true, StandardCharsets.UTF_8)).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test void acceptanceWorkflowThroughQueueAndNewService() throws Exception {
        var db = new TestDatabase();
        String output = run(db.shop, String.join("\n", "1", "P101", "USB-C Port", "2",
                "3", "J101", "Phone", "Not charging", "3", "J102", "Tablet", "Loose port",
                "5", "J101", "P101", "1", "11", "5", "J102", "P101", "2", "11", "0", ""));
        assertTrue(output.contains("Queued request #1"));
        assertTrue(output.contains("Request #1 saved. Remaining stock: 1"));
        assertTrue(output.contains("Insufficient stock"));
        String restored = run(new RepairShopService(db.connections), "4\nJ101\n4\nJ102\n2\n0\n");
        assertTrue(restored.contains("P101 | USB-C Port | Used: 1"));
        assertTrue(restored.contains("No parts used."));
        assertTrue(restored.contains("P101 | USB-C Port | Stock: 1"));
    }

    @Test void invalidNumbersOptionsAndEofAreSafe() throws Exception {
        var db = new TestDatabase();
        String output = run(db.shop, "abc\n99\n1\nP1\nPort\n99999999999999\n1\nP2\n");
        assertTrue(output.contains("Invalid number"));
        assertTrue(output.contains("Unknown menu option"));
        assertTrue(output.contains("Input ended"));
        assertTrue(db.shop.listParts().isEmpty());
    }

    @Test void databaseFailureDoesNotPrintSuccess() {
        var shop = new RepairShopService(() -> { throw new SQLException("Simulated outage", "08001"); });
        String output = run(shop, "1\nP1\nPort\n2\n0\n");
        assertTrue(output.contains("Database operation failed"));
        assertFalse(output.contains("Part saved:"));
    }

    @Test void importReportsAndExportsAreAvailableFromMenu() throws Exception {
        var db = new TestDatabase();
        Path input = directory.resolve("parts.csv");
        Path inventory = directory.resolve("inventory.csv");
        Path usage = directory.resolve("usage.csv");
        Path low = directory.resolve("low.csv");
        Files.writeString(input, "id,name,quantity\nP1,Port,2\n");
        String output = run(db.shop, "8\n" + input + "\n6\n2\n7\n9\n" + inventory
                + "\n10\n" + usage + "\n12\n2\n" + low + "\n0\n");
        assertTrue(output.contains("Imported 1 part(s)."));
        assertTrue(output.contains("P1 | Port | Stock: 2"));
        assertTrue(output.contains("No part usage recorded."));
        assertTrue(Files.readString(inventory).contains("P1,Port,2"));
        assertTrue(Files.readString(usage).startsWith("job_id,part_id,quantity_used"));
        assertTrue(Files.readString(low).contains("P1,Port,2"));
    }
}
