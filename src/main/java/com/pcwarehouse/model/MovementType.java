package com.pcwarehouse.model;

public enum MovementType {
    SUPPLY("Supply"),
    DISPATCH("Dispatch"),
    TRANSFER_OUT("Transfer Out"),
    TRANSFER_IN("Transfer In");

    private final String displayName;

    MovementType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
