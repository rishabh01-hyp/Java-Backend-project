import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/** Console entry point: javac -d out *.java, then java -cp "out;lib/*" Main. */
public class Main {
    private static final Scanner INPUT = new Scanner(System.in);
    private final Database database;
    private final UsageWorker worker = new UsageWorker();

    public Main(Database database) {
        this.database = database;
    }

    public static void main(String[] args) {
        try (Database database = new Database()) {
            new Main(database).run();
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Cannot start: " + e.getMessage());
            System.out.println("Check MySQL and REPAIR_DB_URL/USER/PASSWORD. See README.md.");
        }
    }

    private void run() {
        System.out.println("REPAIR SHOP - Inventory and Job Cards");
        worker.start();
        try {
            boolean running = true;
            while (running) {
                menu();
                try {
                    int choice = number("Choice: ");
                    if (choice != 5) {
                        worker.awaitIdle();
                        worker.printResults();
                    }
                    switch (choice) {
                        case 1: addPart(); break;
                        case 2: showParts(database.parts()); break;
                        case 3: addJob(); break;
                        case 4: viewJob(); break;
                        case 5: queueUsage(); break;
                        case 6: lowStock(); break;
                        case 7: showUsage(); break;
                        case 8: importParts(); break;
                        case 9: Csv.write(text("Export file: "), inventoryRows(database.parts())); break;
                        case 10: Csv.write(text("Export file: "), database.usageRows()); break;
                        case 11: System.out.println("All queued requests finished."); break;
                        case 12: exportLowStock(); break;
                        case 13: restock(); break;
                        case 14: deletePart(); break;
                        case 0: running = false; break;
                        default: System.out.println("Choose 0 to 14.");
                    }
                } catch (SQLException | IOException | IllegalArgumentException e) {
                    System.out.println("Operation failed: " + e.getMessage());
                } catch (NoSuchElementException e) {
                    System.out.println("Input ended.");
                    break;
                }
            }
        } finally {
            worker.finish();
            worker.printResults();
            System.out.println("Goodbye.");
        }
    }

    private static void menu() {
        System.out.println("\n1 Add part       2 List stock       3 Create job");
        System.out.println("4 View job       5 Queue part usage 6 Low-stock report");
        System.out.println("7 Usage report   8 Import CSV       9 Export inventory");
        System.out.println("10 Export usage  11 Wait for queue  12 Export low stock");
        System.out.println("13 Restock       14 Delete unused part   0 Exit");
    }

    private static String text(String prompt) {
        System.out.print(prompt);
        return INPUT.nextLine().trim();
    }

    private static int number(String prompt) {
        return Integer.parseInt(text(prompt));
    }

    private void addPart() throws SQLException {
        Part part = new Part(text("Part ID: "), text("Name: "), number("Stock: "));
        database.addParts(Collections.singletonList(part));
        System.out.println("Part saved.");
    }

    private void addJob() throws SQLException {
        RepairJob job = new RepairJob(text("Job ID: "), text("Device: "), text("Issue: "));
        database.addJob(job);
        System.out.println("Job saved.");
    }

    private void viewJob() throws SQLException {
        String id = Entity.validId(text("Job ID: "));
        System.out.println(database.job(id).description());
        database.usageRows().stream().skip(1).filter(row -> row[0].equals(id))
                .forEach(row -> System.out.println(row[1] + " | Used: " + row[2]));
    }

    private void queueUsage() {
        String job = Entity.validId(text("Job ID: "));
        String part = Entity.validId(text("Part ID: "));
        int quantity = Entity.positive(number("Quantity used: "));
        worker.submit(() -> "Saved " + job + "/" + part + ". Remaining: "
                + database.usePart(job, part, quantity));
        System.out.println("Queued. Option 11 shows the result.");
    }

    private List<Part> lowStockParts() throws SQLException {
        int threshold = Entity.nonnegative(number("Low-stock threshold: "));
        List<Part> result = new ArrayList<>();
        database.parts().stream().filter(part -> part.getQuantity() <= threshold)
                .sorted(Comparator.comparingInt(Part::getQuantity).thenComparing(Part::getId))
                .forEach(result::add);
        return result;
    }

    private void lowStock() throws SQLException {
        showParts(lowStockParts());
    }

    private void exportLowStock() throws SQLException, IOException {
        List<Part> parts = lowStockParts();
        Csv.write(text("Export file: "), inventoryRows(parts));
    }

    private void showUsage() throws SQLException {
        database.usageRows().forEach(row -> System.out.println(String.join(" | ", row)));
    }

    private void importParts() throws IOException, SQLException {
        List<String[]> rows = Csv.read(text("Import file: "));
        if (rows.isEmpty() || !Arrays.equals(rows.remove(0), new String[]{"id", "name", "quantity"})) {
            throw new ShopException("Expected header: id,name,quantity");
        }
        List<Part> parts = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (String[] row : rows) {
            if (row.length != 3) throw new ShopException("Each row needs three columns.");
            Part part = new Part(row[0], row[1], Integer.parseInt(row[2].trim()));
            if (!ids.add(part.getId())) throw new ShopException("Duplicate ID: " + part.getId());
            parts.add(part);
        }
        database.addParts(parts);
        System.out.println("Imported " + parts.size() + " parts.");
    }

    private void restock() throws SQLException {
        database.restock(Entity.validId(text("Part ID: ")), Entity.positive(number("Quantity to add: ")));
        System.out.println("Stock updated.");
    }

    private void deletePart() throws SQLException {
        database.deletePart(Entity.validId(text("Unused part ID: ")));
        System.out.println("Part deleted.");
    }

    private static List<String[]> inventoryRows(List<Part> parts) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"id", "name", "quantity"});
        for (Part part : parts) {
            rows.add(new String[]{part.getId(), part.getName(), String.valueOf(part.getQuantity())});
        }
        return rows;
    }

    private static <T extends Entity> void showParts(List<T> items) {
        if (items.isEmpty()) System.out.println("No matching records.");
        for (T item : items) System.out.println(item.description());
    }
}
