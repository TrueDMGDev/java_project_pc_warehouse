package com.pcwarehouse.model;

public record RequestDraftLine(
        String productCode,
        String productName,
        String manufacturer,
        String category,
        int quantity
) {
}
