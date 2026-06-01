package com.pcwarehouse.model;

public record InventorySummary(
        String warehouseLabel,
        String productCode,
        String productName,
        String manufacturer,
        String category,
        int quantity
) {
}
