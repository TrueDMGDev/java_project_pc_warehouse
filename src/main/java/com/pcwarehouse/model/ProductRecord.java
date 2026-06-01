package com.pcwarehouse.model;

public record ProductRecord(
        String productCode,
        String modelName,
        String manufacturer,
        String category,
        String unitName,
        String description,
        boolean active
) {
}
