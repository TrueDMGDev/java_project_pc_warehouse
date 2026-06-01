package com.pcwarehouse.model;

public record WarehouseAssignmentOption(String label, String warehouseCode) {

    public boolean isAssignedWarehouse() {
        return warehouseCode != null && !warehouseCode.isBlank();
    }

    @Override
    public String toString() {
        return label;
    }
}
