package com.pcwarehouse.model;

import java.time.LocalDateTime;
import java.util.List;

public record RequestDetailRecord(
        String requestNumber,
        String sourceWarehouseCode,
        String sourceWarehouseLabel,
        String destinationWarehouseCode,
        String destinationWarehouseLabel,
        String requestedByUsername,
        String requestedBy,
        String approvedBy,
        RequestStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<RequestDetailLine> lines
) {
}
