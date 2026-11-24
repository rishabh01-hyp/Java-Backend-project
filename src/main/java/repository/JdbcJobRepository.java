package repository;

import model.PartUsage;
import model.RepairJob;
import java.sql.*;
import java.util.*;

public final class JdbcJobRepository implements Repository<RepairJob, String> {
    private final ConnectionFactory connections;

    public JdbcJobRepository(ConnectionFactory connections) { this.connections = connections; }

    @Override public Optional<RepairJob> findById(String id) throws SQLException {
        return readJobs(id).stream().findFirst();
    }

    @Override public List<RepairJob> findAll() throws SQLException { return readJobs(null); }

    /** One joined query gives a consistent view of each job and its usage entries. */
    private List<RepairJob> readJobs(String id) throws SQLException {
        String sql = "SELECT j.id, j.device, j.issue, u.part_id, u.quantity FROM repair_jobs j "
                + "LEFT JOIN part_usage u ON j.id = u.job_id "
                + (id == null ? "" : "WHERE j.id = ? ") + "ORDER BY j.id, u.usage_id";
        var jobs = new LinkedHashMap<String, RepairJob>();
        var usages = new HashMap<String, List<PartUsage>>();
        try (var connection = connections.open(); var query = connection.prepareStatement(sql)) {
            if (id != null) { query.setString(1, id); }
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    String jobId = rows.getString("id");
                    if (!jobs.containsKey(jobId)) {
                        jobs.put(jobId, new RepairJob(jobId, rows.getString("device"), rows.getString("issue"), List.of()));
                        usages.put(jobId, new ArrayList<>());
                    }
                    String partId = rows.getString("part_id");
                    if (partId != null) { usages.get(jobId).add(new PartUsage(partId, rows.getInt("quantity"))); }
                }
            }
        }
        return jobs.values().stream().map(job -> new RepairJob(job.id(), job.device(), job.issue(), usages.get(job.id()))).toList();
    }

    @Override public void add(RepairJob job) throws SQLException {
        if (!job.usages().isEmpty()) {
            throw new IllegalArgumentException("Create an empty job, then record usage through the stock transaction.");
        }
        try (var connection = connections.open(); var query = connection.prepareStatement(
                "INSERT INTO repair_jobs (id, device, issue) VALUES (?, ?, ?)")) {
            query.setString(1, job.id());
            query.setString(2, job.device());
            query.setString(3, job.issue());
            query.executeUpdate();
        }
    }
}
