package com.pcwarehouse.model;

public record RequestDetailLine(
        String productCode,
        String productName,
        String manufacturer,
        String category,
        int quantityRequested,
        Integer quantityApproved,
        int availableQuantity
) {
    public boolean hasInsufficientAvailableQuantity() {
        return quantityRequested > availableQuantity;
    }
}
