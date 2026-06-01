package com.pcwarehouse.repository;

import com.pcwarehouse.model.InventoryFilterCriteria;
import com.pcwarehouse.model.InventoryReportRow;
import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class InventoryInsightsRepository {

    public List<InventorySummary> findFilteredInventory(
            Connection connection,
            UserSession session,
            InventoryFilterCriteria criteria
    ) throws SQLException {
        String sql = """
                SELECT w.warehouse_code,
                       w.warehouse_name,
                       p.product_code,
                       p.model_name,
                       m.manufacturer_name,
                       c.category_name,
                       i.quantity
                FROM inventory i
                JOIN warehouses w ON w.warehouse_id = i.warehouse_id
                JOIN products p ON p.product_id = i.product_id
                JOIN manufacturers m ON m.manufacturer_id = p.manufacturer_id
                JOIN categories c ON c.category_id = p.category_id
                WHERE (? = TRUE OR w.warehouse_code = ?)
                  AND (? = '' OR LOWER(p.product_code) LIKE ? OR LOWER(p.model_name) LIKE ?)
                  AND (? = '' OR c.category_name = ?)
                  AND (? = '' OR m.manufacturer_name = ?)
                ORDER BY w.warehouse_code, c.category_name, m.manufacturer_name, p.model_name
                """;

        String normalizedSearch = normalizeSearch(criteria.searchTerm());
        String wildcard = "%" + normalizedSearch + "%";
        String category = normalizeExact(criteria.category());
        String manufacturer = normalizeExact(criteria.manufacturer());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, session.isAdmin());
            statement.setString(2, session.isAdmin() ? "" : session.warehouse().code());
            statement.setString(3, normalizedSearch);
            statement.setString(4, wildcard);
            statement.setString(5, wildcard);
            statement.setString(6, category);
            statement.setString(7, category);
            statement.setString(8, manufacturer);
            statement.setString(9, manufacturer);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<InventorySummary> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new InventorySummary(
                            resultSet.getString("warehouse_code") + " - " + resultSet.getString("warehouse_name"),
                            resultSet.getString("product_code"),
                            resultSet.getString("model_name"),
                            resultSet.getString("manufacturer_name"),
                            resultSet.getString("category_name"),
                            resultSet.getInt("quantity")
                    ));
                }
                return results;
            }
        }
    }

    public List<InventoryReportRow> findInventoryReport(
            Connection connection,
            UserSession session,
            InventoryFilterCriteria criteria
    ) throws SQLException {
        String sql = """
                SELECT w.warehouse_code,
                       w.warehouse_name,
                       p.product_code,
                       p.model_name,
                       m.manufacturer_name,
                       c.category_name,
                       i.quantity
                FROM inventory i
                JOIN warehouses w ON w.warehouse_id = i.warehouse_id
                JOIN products p ON p.product_id = i.product_id
                JOIN manufacturers m ON m.manufacturer_id = p.manufacturer_id
                JOIN categories c ON c.category_id = p.category_id
                WHERE (? = '' OR w.warehouse_code = ?)
                  AND (? = '' OR c.category_name = ?)
                  AND (? = '' OR m.manufacturer_name = ?)
                ORDER BY w.warehouse_code, c.category_name, m.manufacturer_name, p.model_name
                """;

        String reportWarehouse = normalizeExact(criteria.warehouseCode());
        String category = normalizeExact(criteria.category());
        String manufacturer = normalizeExact(criteria.manufacturer());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reportWarehouse);
            statement.setString(2, reportWarehouse);
            statement.setString(3, category);
            statement.setString(4, category);
            statement.setString(5, manufacturer);
            statement.setString(6, manufacturer);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<InventoryReportRow> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new InventoryReportRow(
                            resultSet.getString("warehouse_code") + " - " + resultSet.getString("warehouse_name"),
                            resultSet.getString("product_code"),
                            resultSet.getString("model_name"),
                            resultSet.getString("manufacturer_name"),
                            resultSet.getString("category_name"),
                            resultSet.getInt("quantity")
                    ));
                }
                return results;
            }
        }
    }

    public List<Warehouse> findReportWarehouses(Connection connection, UserSession session) throws SQLException {
        String sql = """
                SELECT warehouse_code,
                       warehouse_name,
                       city
                FROM warehouses
                WHERE active = TRUE
                ORDER BY warehouse_code
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Warehouse> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new Warehouse(
                            resultSet.getString("warehouse_code"),
                            resultSet.getString("warehouse_name"),
                            resultSet.getString("city")
                    ));
                }
                return results;
            }
        }
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeExact(String value) {
        return value == null ? "" : value.trim();
    }
}
