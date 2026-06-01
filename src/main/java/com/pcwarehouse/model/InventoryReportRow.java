package com.pcwarehouse.model;

public record InventoryReportRow(
        String warehouseLabel,
        String productCode,
        String productName,
        String manufacturer,
        String category,
        int quantity
) {
}
