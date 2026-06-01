package com.pcwarehouse.model;

public record Warehouse(String code, String name, String city) {

    public String label() {
        return code + " - " + name;
    }

    @Override
    public String toString() {
        return label();
    }
}
