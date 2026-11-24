package model;

import java.util.List;

public record RepairJob(String id, String device, String issue, List<PartUsage> usages) {
    public RepairJob {
        id = Validation.id(id, "Job ID");
        device = Validation.text(device, "Device name", 255);
        issue = Validation.text(issue, "Issue description");
        usages = List.copyOf(usages);
    }
}
