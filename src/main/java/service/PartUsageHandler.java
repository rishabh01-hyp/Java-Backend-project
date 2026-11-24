package service;

import java.sql.SQLException;

/** Functional interface connecting queue workers to the stock transaction. */
@FunctionalInterface
public interface PartUsageHandler {
    int recordUsage(String jobId, String partId, int quantity) throws SQLException;
}
