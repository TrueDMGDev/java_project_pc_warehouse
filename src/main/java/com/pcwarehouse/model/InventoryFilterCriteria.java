package com.pcwarehouse.model;

public record InventoryFilterCriteria(
        String searchTerm,
        String category,
        String manufacturer,
        String warehouseCode
) {
}
