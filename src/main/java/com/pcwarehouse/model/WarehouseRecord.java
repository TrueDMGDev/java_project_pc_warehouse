package com.pcwarehouse.model;

public record WarehouseRecord(
        String warehouseCode,
        String warehouseName,
        String city,
        String addressLine,
        boolean active
) {
}
