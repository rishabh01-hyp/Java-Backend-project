package service;

import model.SparePart;
import model.UsageReportRow;
import java.util.Comparator;

/** Comparator composition: deterministic ordering for report stream pipelines. */
public final class ReportComparators {
    private ReportComparators() { }

    public static final Comparator<SparePart> LOW_STOCK =
            Comparator.comparingInt(SparePart::quantity).thenComparing(SparePart::id);
    public static final Comparator<UsageReportRow> USAGE_BY_JOB =
            Comparator.comparing(UsageReportRow::jobId).thenComparing(UsageReportRow::partId);
}
