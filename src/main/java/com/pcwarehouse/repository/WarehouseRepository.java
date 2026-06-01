package com.pcwarehouse.repository;

import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.model.WarehouseRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WarehouseRepository {

    public List<WarehouseRecord> findWarehouses(Connection connection, String searchTerm) throws SQLException {
        String sql = """
                SELECT warehouse_code,
                       warehouse_name,
                       city,
                       address_line,
                       active
                FROM warehouses
                WHERE (? = ''
                       OR LOWER(warehouse_code) LIKE ?
                       OR LOWER(warehouse_name) LIKE ?
                       OR LOWER(city) LIKE ?)
                ORDER BY active DESC, warehouse_code
                """;

        String normalizedSearch = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
        String wildcard = "%" + normalizedSearch + "%";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedSearch);
            statement.setString(2, wildcard);
            statement.setString(3, wildcard);
            statement.setString(4, wildcard);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<WarehouseRecord> warehouses = new ArrayList<>();
                while (resultSet.next()) {
                    warehouses.add(new WarehouseRecord(
                            resultSet.getString("warehouse_code"),
                            resultSet.getString("warehouse_name"),
                            resultSet.getString("city"),
                            resultSet.getString("address_line"),
                            resultSet.getBoolean("active")
                    ));
                }
                return warehouses;
            }
        }
    }

    public List<Warehouse> findWarehouseOptions(Connection connection) throws SQLException {
        String sql = """
                SELECT warehouse_code,
                       warehouse_name,
                       city
                FROM warehouses
                WHERE active = TRUE
                ORDER BY warehouse_code
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Warehouse> warehouses = new ArrayList<>();
            while (resultSet.next()) {
                warehouses.add(new Warehouse(
                        resultSet.getString("warehouse_code"),
                        resultSet.getString("warehouse_name"),
                        resultSet.getString("city")
                ));
            }
            return warehouses;
        }
    }

    public void insertWarehouse(
            Connection connection,
            String warehouseCode,
            String warehouseName,
            String city,
            String addressLine
    ) throws SQLException {
        String sql = """
                INSERT INTO warehouses (
                    warehouse_code,
                    warehouse_name,
                    city,
                    address_line,
                    active
                ) VALUES (?, ?, ?, ?, TRUE)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, warehouseCode);
            statement.setString(2, warehouseName);
            statement.setString(3, city);
            statement.setString(4, addressLine);
            statement.executeUpdate();
        }
    }

    public void updateWarehouse(
            Connection connection,
            String originalWarehouseCode,
            String warehouseCode,
            String warehouseName,
            String city,
            String addressLine,
            boolean active
    ) throws SQLException {
        String sql = """
                UPDATE warehouses
                SET warehouse_code = ?,
                    warehouse_name = ?,
                    city = ?,
                    address_line = ?,
                    active = ?
                WHERE warehouse_code = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, warehouseCode);
            statement.setString(2, warehouseName);
            statement.setString(3, city);
            statement.setString(4, addressLine);
            statement.setBoolean(5, active);
            statement.setString(6, originalWarehouseCode);
            statement.executeUpdate();
        }
    }

    public void deleteWarehouse(Connection connection, String warehouseCode) throws SQLException {
        String sql = "DELETE FROM warehouses WHERE warehouse_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, warehouseCode);
            statement.executeUpdate();
        }
    }
}
