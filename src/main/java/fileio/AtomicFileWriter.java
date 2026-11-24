package fileio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Write beside the destination, then replace it atomically. */
public final class AtomicFileWriter {
    private AtomicFileWriter() { }

    @FunctionalInterface
    public interface Content {
        void write(BufferedWriter writer) throws IOException;
    }

    public static void write(Path destination, Content content) throws IOException {
        Path path = destination.toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "repair-report-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                content.write(writer);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
