package com.pcwarehouse.model;

public record StockOperationLine(
        boolean newProduct,
        String productCode,
        String productName,
        String manufacturer,
        String category,
        String unitName,
        String description,
        int quantity
) {
    public static StockOperationLine existingProduct(ProductOption product, int quantity) {
        return new StockOperationLine(
                false,
                product.productCode(),
                product.productName(),
                product.manufacturer(),
                product.category(),
                "pcs",
                "",
                quantity
        );
    }

    public static StockOperationLine inventoryProduct(InventorySummary inventory, int quantity) {
        return new StockOperationLine(
                false,
                inventory.productCode(),
                inventory.productName(),
                inventory.manufacturer(),
                inventory.category(),
                "pcs",
                "",
                quantity
        );
    }

    public String productLabel() {
        return productName + " | " + manufacturer + " | " + category + (newProduct ? " | New" : "");
    }
}
