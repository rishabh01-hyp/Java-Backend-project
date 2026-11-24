package csv;

import fileio.AtomicFileWriter;
import model.SparePart;
import model.UsageReportRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** UTF-8, RFC 4180 CSV including quoted commas, quotes, and multiline fields. */
public final class CsvFiles {
    private static final List<String> INVENTORY_HEADER = List.of("id", "name", "quantity");
    private CsvFiles() { }

    public static List<SparePart> readInventory(Path path) throws IOException {
        var parts = new ArrayList<SparePart>();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            reader.mark(1);
            if (reader.read() != '\uFEFF') { reader.reset(); }
            try (var parser = new CSVParser(reader, CSVFormat.RFC4180.builder()
                    .setHeader().setSkipHeaderRecord(true).build())) {
                if (!parser.getHeaderNames().equals(INVENTORY_HEADER)) {
                    throw new IllegalArgumentException("CSV header must be exactly: id,name,quantity");
                }
                for (var row : parser) {
                    if (!row.isConsistent()) {
                        throw new IllegalArgumentException("CSV record " + row.getRecordNumber() + " must have 3 fields.");
                    }
                    try {
                        parts.add(new SparePart(row.get(0), row.get(1), Integer.parseInt(row.get(2).strip())));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("CSV record " + row.getRecordNumber() + ": " + e.getMessage(), e);
                    }
                    if (parts.size() > 100000) { throw new IllegalArgumentException("CSV import is limited to 100000 parts."); }
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return List.copyOf(parts);
    }

    public static void writeInventory(Path path, List<SparePart> parts) throws IOException {
        write(path, INVENTORY_HEADER.toArray(String[]::new), printer -> {
            for (var part : parts) { printer.printRecord(part.id(), part.name(), part.quantity()); }
        });
    }

    public static void writeUsage(Path path, List<UsageReportRow> rows) throws IOException {
        write(path, new String[]{"job_id", "part_id", "quantity_used"}, printer -> {
            for (var row : rows) { printer.printRecord(row.jobId(), row.partId(), row.quantityUsed()); }
        });
    }

    @FunctionalInterface
    private interface Output { void print(CSVPrinter printer) throws IOException; }

    private static void write(Path destination, String[] header, Output output) throws IOException {
        AtomicFileWriter.write(destination, writer -> {
            try (var printer = new CSVPrinter(writer, CSVFormat.RFC4180.builder().setHeader(header).build())) {
                output.print(printer);
            }
        });
    }
}
