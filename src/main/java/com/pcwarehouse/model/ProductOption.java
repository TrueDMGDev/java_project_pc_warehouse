package com.pcwarehouse.model;

public record ProductOption(String productCode, String productName, String manufacturer, String category) {

    @Override
    public String toString() {
        return productCode + " | " + manufacturer + " | " + productName;
    }
}
