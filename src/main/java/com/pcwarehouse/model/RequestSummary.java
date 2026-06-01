package com.pcwarehouse.model;

import java.time.LocalDateTime;

public record RequestSummary(
        String requestNumber,
        String sourceWarehouseCode,
        String sourceWarehouseLabel,
        String destinationWarehouseCode,
        String destinationWarehouseLabel,
        String requestedByUsername,
        String requestedBy,
        RequestStatus status,
        int lineCount,
        int totalQuantity,
        LocalDateTime createdAt
) {
}
