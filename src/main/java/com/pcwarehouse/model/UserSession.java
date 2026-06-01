package com.pcwarehouse.model;

public record UserSession(String username, String fullName, Role role, Warehouse warehouse) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public String scopeLabel() {
        return isAdmin() ? "All warehouses" : warehouse.label();
    }

    @Override
    public String toString() {
        return fullName + " | " + role.displayName() + " | " + scopeLabel();
    }
}
