package repository;

import model.PartUsage;
import model.RepairJob;
import model.SparePart;
import org.junit.jupiter.api.Test;
import support.TestDatabase;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JdbcRepositoryTest {
    @Test void genericRepositoriesAreTypeSafeAndSchemaInitializationIsIdempotent() throws Exception {
        var db = new TestDatabase();
        Repository<SparePart, String> parts = new JdbcPartRepository(db.connections);
        Repository<RepairJob, String> jobs = new JdbcJobRepository(db.connections);
        assertTrue(parts.findById("missing").isEmpty());
        assertTrue(jobs.findAll().isEmpty());
        parts.add(new SparePart("P1", "Port", 2));
        jobs.add(new RepairJob("J1", "Phone", "Issue", List.of()));
        JdbcSchema.initialize(db.connections);
        assertEquals(2, parts.findById("P1").orElseThrow().quantity());
        assertEquals(1, jobs.findAll().size());
        assertThrows(IllegalArgumentException.class, () -> jobs.add(new RepairJob("J2", "Phone", "Issue",
                List.of(new PartUsage("P1", 1)))));
    }

    @Test void preparedStatementsTreatQuotesAsData() throws Exception {
        var db = new TestDatabase();
        db.shop.addPart("P1", "Port'); DROP TABLE spare_parts; --", 1);
        db.shop.createJob("J1", "Customer's phone", "It won't charge");
        assertEquals(1, db.shop.listParts().size());
        assertEquals("It won't charge", db.shop.findJob("J1").issue());
    }
}
