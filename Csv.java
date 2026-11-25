import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Character-stream CSV, including quoted commas, quotes and multiline fields. */
class Csv {
    public static List<String[]> read(String path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false, closed = false, started = false;
        try (PushbackReader reader = new PushbackReader(new FileReader(path, StandardCharsets.UTF_8), 1)) {
            int value;
            while ((value = reader.read()) != -1) {
                char c = (char) value;
                started = true;
                if (quoted) {
                    if (c == '"') {
                        int next = reader.read();
                        if (next == '"') field.append('"');
                        else {
                            quoted = false;
                            closed = true;
                            if (next != -1) reader.unread(next);
                        }
                    } else field.append(c);
                } else if (c == ',' || c == '\n' || c == '\r') {
                    row.add(field.toString());
                    field.setLength(0);
                    closed = false;
                    if (c != ',') {
                        rows.add(row.toArray(new String[0]));
                        row = new ArrayList<>();
                        started = false;
                        if (c == '\r') {
                            int next = reader.read();
                            if (next != '\n' && next != -1) reader.unread(next);
                        }
                    }
                } else if (closed) {
                    throw new IOException("Unexpected text after closing CSV quote.");
                } else if (c == '"') {
                    if (field.length() != 0) throw new IOException("Quote inside unquoted field.");
                    quoted = true;
                } else field.append(c);
            }
        }
        if (quoted) throw new IOException("Unclosed CSV quote.");
        if (started) {
            row.add(field.toString());
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }

    public static void write(String path, List<String[]> rows) throws IOException {
        try (FileWriter writer = new FileWriter(path, StandardCharsets.UTF_8)) {
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) writer.write(',');
                    writer.write('"' + row[i].replace("\"", "\"\"") + '"');
                }
                writer.write(System.lineSeparator());
            }
        }
        System.out.println("Exported: " + path);
    }
}
