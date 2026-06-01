package com.pcwarehouse.model;

import java.time.LocalDateTime;

public record MovementLogEntry(
        LocalDateTime timestamp,
        MovementType type,
        String product,
        String manufacturer,
        int quantity,
        Warehouse sourceWarehouse,
        Warehouse destinationWarehouse,
        String externalSource,
        String externalDestination,
        String performedBy,
        String details
) {

    public boolean touchesWarehouse(Warehouse warehouse) {
        if (warehouse == null) {
            return true;
        }

        return warehouse.equals(sourceWarehouse) || warehouse.equals(destinationWarehouse);
    }

    public String sourceWarehouseLabel() {
        if (sourceWarehouse != null) {
            return sourceWarehouse.label();
        }
        return externalSource == null || externalSource.isBlank() ? "-" : externalSource;
    }

    public String destinationWarehouseLabel() {
        if (destinationWarehouse != null) {
            return destinationWarehouse.label();
        }
        return externalDestination == null || externalDestination.isBlank() ? "-" : externalDestination;
    }
}
