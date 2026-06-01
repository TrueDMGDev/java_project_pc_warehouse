package com.pcwarehouse.repository;

import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.UserSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class InventoryRepository {

    public List<InventorySummary> findVisibleInventory(Connection connection, UserSession session) throws SQLException {
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
                ORDER BY w.warehouse_code, c.category_name, m.manufacturer_name, p.model_name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, session.isAdmin());
            statement.setString(2, session.isAdmin() ? "" : session.warehouse().code());

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
}
