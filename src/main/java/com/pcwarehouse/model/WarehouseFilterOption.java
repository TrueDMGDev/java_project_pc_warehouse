package com.pcwarehouse.model;

public record WarehouseFilterOption(String label, Warehouse warehouse) {

    public boolean matches(MovementLogEntry entry) {
        return warehouse == null || entry.touchesWarehouse(warehouse);
    }

    @Override
    public String toString() {
        return label;
    }
}
