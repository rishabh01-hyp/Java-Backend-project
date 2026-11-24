package service;

import model.SparePart;
import model.UsageReportRow;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public final class ReportService {
    private final RepairShopService shop;
    private record JobPart(String jobId, String partId) { }

    public ReportService(RepairShopService shop) { this.shop = shop; }

    public List<SparePart> lowStock(int threshold) throws SQLException {
        if (threshold < 0) { throw new IllegalArgumentException("Threshold must be zero or greater."); }
        return shop.listParts().stream().filter(part -> part.quantity() <= threshold)
                .sorted(ReportComparators.LOW_STOCK).toList();
    }

    /** Aggregate repeated usage actions per (job, part); long totals avoid integer overflow. */
    public List<UsageReportRow> usageByJob() throws SQLException {
        return shop.listJobs().stream()
                .flatMap(job -> job.usages().stream().map(usage ->
                        new UsageReportRow(job.id(), usage.partId(), usage.quantity())))
                .collect(Collectors.groupingBy(row -> new JobPart(row.jobId(), row.partId()),
                        Collectors.summingLong(UsageReportRow::quantityUsed)))
                .entrySet().stream()
                .map(entry -> new UsageReportRow(entry.getKey().jobId(), entry.getKey().partId(), entry.getValue()))
                .sorted(ReportComparators.USAGE_BY_JOB).toList();
    }
}
