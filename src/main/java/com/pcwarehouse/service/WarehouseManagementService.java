package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.model.WarehouseChangeResult;
import com.pcwarehouse.model.WarehouseFilterOption;
import com.pcwarehouse.model.WarehouseRecord;
import com.pcwarehouse.repository.WarehouseRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WarehouseManagementService {

    private final WarehouseRepository warehouseRepository = new WarehouseRepository();

    public List<WarehouseRecord> loadWarehouses(String searchTerm) {
        try (Connection connection = DatabaseConnection.open()) {
            return warehouseRepository.findWarehouses(connection, searchTerm);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<WarehouseFilterOption> loadWarehouseFilterOptions() {
        try (Connection connection = DatabaseConnection.open()) {
            List<WarehouseFilterOption> options = new ArrayList<>();
            options.add(new WarehouseFilterOption("All warehouses", null));
            for (Warehouse warehouse : warehouseRepository.findWarehouseOptions(connection)) {
                options.add(new WarehouseFilterOption(warehouse.label(), warehouse));
            }
            return options;
        } catch (SQLException exception) {
            return List.of(new WarehouseFilterOption("All warehouses", null));
        }
    }

    public WarehouseChangeResult createWarehouse(
            UserSession session,
            String warehouseCode,
            String warehouseName,
            String city,
            String addressLine
    ) {
        if (!canManageWarehouses(session)) {
            return new WarehouseChangeResult(false, "Only admin can add or edit warehouses.");
        }

        ValidationResult validation = validate(warehouseCode, warehouseName, city, addressLine);
        if (!validation.valid()) {
            return new WarehouseChangeResult(false, validation.message());
        }

        try (Connection connection = DatabaseConnection.open()) {
            warehouseRepository.insertWarehouse(
                    connection,
                    warehouseCode.trim(),
                    warehouseName.trim(),
                    city.trim(),
                    addressLine.trim()
            );
            return new WarehouseChangeResult(true, "Added warehouse " + warehouseCode.trim() + ".");
        } catch (SQLException exception) {
            return new WarehouseChangeResult(false, "Create failed: " + describeConstraintIssue(exception, "warehouse"));
        }
    }

    public WarehouseChangeResult updateWarehouse(
            UserSession session,
            String originalWarehouseCode,
            String warehouseCode,
            String warehouseName,
            String city,
            String addressLine,
            boolean active
    ) {
        if (!canManageWarehouses(session)) {
            return new WarehouseChangeResult(false, "Only admin can add or edit warehouses.");
        }
        if (originalWarehouseCode == null || originalWarehouseCode.isBlank()) {
            return new WarehouseChangeResult(false, "Select a warehouse row to update.");
        }

        ValidationResult validation = validate(warehouseCode, warehouseName, city, addressLine);
        if (!validation.valid()) {
            return new WarehouseChangeResult(false, validation.message());
        }

        try (Connection connection = DatabaseConnection.open()) {
            warehouseRepository.updateWarehouse(
                    connection,
                    originalWarehouseCode,
                    warehouseCode.trim(),
                    warehouseName.trim(),
                    city.trim(),
                    addressLine.trim(),
                    active
            );
            return new WarehouseChangeResult(true, "Updated warehouse " + warehouseCode.trim() + ".");
        } catch (SQLException exception) {
            return new WarehouseChangeResult(false, "Update failed: " + describeConstraintIssue(exception, "warehouse"));
        }
    }

    public WarehouseChangeResult deleteWarehouse(UserSession session, WarehouseRecord warehouse) {
        if (!canManageWarehouses(session)) {
            return new WarehouseChangeResult(false, "Only admin can delete warehouses.");
        }
        if (warehouse == null) {
            return new WarehouseChangeResult(false, "Select a warehouse row to delete.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            warehouseRepository.deleteWarehouse(connection, warehouse.warehouseCode());
            return new WarehouseChangeResult(true, "Deleted warehouse " + warehouse.warehouseCode() + ".");
        } catch (SQLException exception) {
            return new WarehouseChangeResult(false, "Delete failed: " + describeConstraintIssue(exception, "warehouse"));
        }
    }

    public WarehouseChangeResult forceDeleteWarehouse(UserSession session, WarehouseRecord warehouse) {
        if (!canManageWarehouses(session)) {
            return new WarehouseChangeResult(false, "Only admin can force delete warehouses.");
        }
        if (warehouse == null) {
            return new WarehouseChangeResult(false, "Select a warehouse row to force delete.");
        }
        if (session.warehouse() != null && warehouse.warehouseCode().equals(session.warehouse().code())) {
            return new WarehouseChangeResult(false, "Use a different admin account to force delete the current warehouse.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                warehouseRepository.forceDeleteWarehouse(connection, warehouse.warehouseCode());
                connection.commit();
                return new WarehouseChangeResult(
                        true,
                        "Force deleted warehouse " + warehouse.warehouseCode() + " and related records."
                );
            } catch (SQLException exception) {
                connection.rollback();
                return new WarehouseChangeResult(false, "Force delete failed: " + describeConstraintIssue(exception, "warehouse"));
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new WarehouseChangeResult(false, "Force delete failed: " + describeConstraintIssue(exception, "warehouse"));
        }
    }

    private boolean canManageWarehouses(UserSession session) {
        return session != null && session.role() == Role.ADMIN;
    }

    private ValidationResult validate(String warehouseCode, String warehouseName, String city, String addressLine) {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            return new ValidationResult(false, "Warehouse code is required.");
        }
        if (warehouseName == null || warehouseName.isBlank()) {
            return new ValidationResult(false, "Warehouse name is required.");
        }
        if (city == null || city.isBlank()) {
            return new ValidationResult(false, "City is required.");
        }
        if (addressLine == null || addressLine.isBlank()) {
            return new ValidationResult(false, "Address is required.");
        }
        return new ValidationResult(true, "");
    }

    private String describeConstraintIssue(SQLException exception, String subject) {
        return switch (exception.getSQLState()) {
            case "23505" -> "a " + subject + " with the same code or name already exists.";
            case "23503" -> "this " + subject + " is still referenced by users, requests, inventory, or movement history.";
            default -> exception.getMessage();
        };
    }

    private record ValidationResult(boolean valid, String message) {
    }
}
