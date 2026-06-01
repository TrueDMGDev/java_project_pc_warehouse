package com.pcwarehouse.model;

import java.util.List;

public record DashboardSnapshot(
        boolean usingLiveDatabase,
        String databaseError,
        List<DashboardMetric> metrics,
        List<InventorySummary> inventoryItems,
        List<RequestSummary> requestSummaries,
        List<MovementLogEntry> movementLogEntries
) {
}
