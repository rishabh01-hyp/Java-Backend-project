package service;

import model.*;
import repository.*;

import java.sql.SQLException;
import java.util.List;

public final class RepairShopService {
    private final Repository<SparePart, String> parts;
    private final Repository<RepairJob, String> jobs;
    private final JdbcShopStore store;

    public RepairShopService(ConnectionFactory connections) {
        parts = new JdbcPartRepository(connections);
        jobs = new JdbcJobRepository(connections);
        store = new JdbcShopStore(connections);
    }

    public SparePart addPart(String id, String name, int quantity) throws SQLException {
        var part = new SparePart(id, name, quantity);
        try { parts.add(part); } catch (SQLException e) { throw duplicateOrStorage(e, "Part ID already exists: " + part.id()); }
        return part;
    }

    public RepairJob createJob(String id, String device, String issue) throws SQLException {
        var job = new RepairJob(id, device, issue, List.of());
        try { jobs.add(job); } catch (SQLException e) { throw duplicateOrStorage(e, "Job ID already exists: " + job.id()); }
        return job;
    }

    public RepairJob findJob(String id) throws SQLException {
        String key = Validation.id(id, "Job ID");
        return jobs.findById(key).orElseThrow(() -> new IllegalArgumentException("Unknown job ID: " + key));
    }

    public SparePart findPart(String id) throws SQLException {
        String key = Validation.id(id, "Part ID");
        return parts.findById(key).orElseThrow(() -> new IllegalArgumentException("Unknown part ID: " + key));
    }

    public List<SparePart> listParts() throws SQLException { return parts.findAll(); }
    public List<RepairJob> listJobs() throws SQLException { return jobs.findAll(); }

    public int recordUsage(String jobId, String partId, int quantity) throws SQLException {
        return store.recordUsage(jobId, partId, quantity);
    }

    public int importParts(List<SparePart> parts) throws SQLException {
        try { return store.importParts(parts); }
        catch (SQLException e) { throw duplicateOrStorage(e, "Import contains an existing part ID; no rows imported."); }
    }

    private static SQLException duplicateOrStorage(SQLException e, String message) {
        if (e.getErrorCode() == 1062 || "23505".equals(e.getSQLState())) {
            throw new IllegalArgumentException(message, e);
        }
        return e;
    }
}
