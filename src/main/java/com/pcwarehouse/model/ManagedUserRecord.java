package com.pcwarehouse.model;

public record ManagedUserRecord(
        String username,
        String fullName,
        Role role,
        String warehouseCode,
        String warehouseLabel,
        boolean active
) {
}
