package cli;

import csv.CsvFiles;
import model.SparePart;
import service.PartRequestProcessor;
import service.RepairShopService;
import service.ReportService;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;

public final class ConsoleMenu {
    private final RepairShopService service;
    private final ReportService reports;
    private final PartRequestProcessor requests;
    private final LinkedHashMap<Long, PartRequestProcessor.Submission> pending = new LinkedHashMap<>();
    private final Scanner input;
    private final PrintStream output;

    public ConsoleMenu(RepairShopService service, PartRequestProcessor requests, Scanner input, PrintStream output) {
        this.service = service;
        this.reports = new ReportService(service);
        this.requests = requests;
        this.input = input;
        this.output = output;
    }

    public void run() {
        output.println("Repair Shop Job Card and Spare Parts Manager - JDBC/MySQL");
        while (true) {
            output.println("\n1. Add spare part\n2. List stock\n3. Create repair job\n4. View job"
                    + "\n5. Queue part usage\n6. Low-stock report\n7. Part usage by job"
                    + "\n8. Import inventory CSV\n9. Export inventory CSV\n10. Export usage CSV"
                    + "\n11. Wait for queued request results\n12. Export low-stock CSV\n0. Exit");
            try {
                switch (readInt("Choice: ")) {
                    case 1 -> addPart();
                    case 2 -> printParts(service.listParts());
                    case 3 -> createJob();
                    case 4 -> viewJob();
                    case 5 -> queueUsage();
                    case 6 -> printParts(reports.lowStock(readInt("Low-stock threshold (inclusive): ")));
                    case 7 -> printUsage();
                    case 8 -> importInventory();
                    case 9 -> {
                        Path path = readPath("Export path (existing file replaced): ");
                        CsvFiles.writeInventory(path, service.listParts());
                        output.println("Inventory CSV exported.");
                    }
                    case 10 -> {
                        Path path = readPath("Export path (existing file replaced): ");
                        CsvFiles.writeUsage(path, reports.usageByJob());
                        output.println("Usage CSV exported.");
                    }
                    case 11 -> awaitResults();
                    case 12 -> {
                        int threshold = readInt("Low-stock threshold (inclusive): ");
                        Path path = readPath("Export path (existing file replaced): ");
                        CsvFiles.writeInventory(path, reports.lowStock(threshold));
                        output.println("Low-stock CSV exported.");
                    }
                    case 0 -> {
                        awaitResults();
                        output.println("Goodbye. Accepted requests have finished.");
                        return;
                    }
                    default -> output.println("Unknown menu option. Choose 0 to 12.");
                }
            } catch (NumberFormatException e) {
                output.println("Invalid number. Enter a whole number within the integer range. Operation cancelled.");
            } catch (IllegalArgumentException | RejectedExecutionException e) {
                output.println("Error: " + e.getMessage());
            } catch (SQLException e) {
                storageError(e);
            } catch (IOException e) {
                output.println("CSV file error: " + e.getMessage());
            } catch (NoSuchElementException e) {
                output.println("\nInput ended. Finishing accepted requests; incomplete input was not submitted.");
                awaitResults();
                return;
            }
        }
    }

    private String readLine(String prompt) {
        output.print(prompt);
        output.flush();
        return input.nextLine().strip();
    }

    private int readInt(String prompt) { return Integer.parseInt(readLine(prompt)); }
    private Path readPath(String prompt) { return Path.of(model.Validation.text(readLine(prompt), "CSV path")); }

    private void addPart() throws SQLException {
        String id = readLine("Part ID: ");
        String name = readLine("Part name: ");
        int quantity = readInt("Starting quantity: ");
        var part = service.addPart(id, name, quantity);
        output.println("Part saved: " + part.id() + " | " + part.name() + " | Stock: " + part.quantity());
    }

    private void printParts(List<SparePart> parts) {
        if (parts.isEmpty()) { output.println("No matching spare parts."); }
        for (var part : parts) {
            output.println(part.id() + " | " + part.name() + " | Stock: " + part.quantity());
        }
    }

    private void createJob() throws SQLException {
        String id = readLine("Job ID: ");
        String device = readLine("Device name: ");
        String issue = readLine("Issue description: ");
        output.println("Job saved: " + service.createJob(id, device, issue).id());
    }

    private void viewJob() throws SQLException {
        var job = service.findJob(readLine("Job ID: "));
        output.println("Job: " + job.id() + "\nDevice: " + job.device() + "\nIssue: " + job.issue());
        if (job.usages().isEmpty()) { output.println("No parts used."); }
        for (var usage : job.usages()) {
            output.println(usage.partId() + " | " + service.findPart(usage.partId()).name() + " | Used: " + usage.quantity());
        }
    }

    private void queueUsage() {
        if (pending.size() >= 100) {
            throw new RejectedExecutionException("Check request results (option 11) before submitting more.");
        }
        String jobId = readLine("Job ID: ");
        String partId = readLine("Part ID: ");
        int quantity = readInt("Quantity used: ");
        var submission = requests.submit(jobId, partId, quantity);
        pending.put(submission.id(), submission);
        output.println("Queued request #" + submission.id() + ". Use option 11 for its committed result.");
    }

    private void awaitResults() {
        if (pending.isEmpty()) { output.println("No pending request results."); }
        for (var submission : pending.values()) {
            try {
                int remaining = submission.result().toCompletableFuture().join();
                output.println("Request #" + submission.id() + " saved. Remaining stock: " + remaining);
            } catch (CompletionException e) {
                if (e.getCause() instanceof SQLException sql) { storageError(sql); }
                else { output.println("Request #" + submission.id() + " rejected: " + e.getCause().getMessage()); }
            }
        }
        pending.clear();
    }

    private void printUsage() throws SQLException {
        var rows = reports.usageByJob();
        if (rows.isEmpty()) { output.println("No part usage recorded."); }
        rows.forEach(row -> output.println(row.jobId() + " | " + row.partId() + " | Total used: " + row.quantityUsed()));
    }

    private void importInventory() throws IOException, SQLException {
        Path path = readPath("Inventory CSV path: ");
        var parts = CsvFiles.readInventory(path); // Parse/validate everything before opening a transaction.
        output.println("Imported " + service.importParts(parts) + " part(s).");
    }

    private void storageError(SQLException e) {
        output.println("Database operation failed (SQL state " + e.getSQLState()
                + "). No success confirmed. Check job/stock before retrying if the connection was lost.");
    }
}
