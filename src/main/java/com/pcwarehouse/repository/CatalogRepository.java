package com.pcwarehouse.repository;

import com.pcwarehouse.model.ProductOption;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CatalogRepository {

    public List<Warehouse> findOperationWarehouses(Connection connection, UserSession session) throws SQLException {
        String sql = """
                SELECT warehouse_code, warehouse_name, city
                FROM warehouses
                WHERE active = TRUE
                  AND (? = TRUE OR warehouse_code = ?)
                ORDER BY warehouse_code
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, session.isAdmin());
            statement.setString(2, session.isAdmin() ? "" : session.warehouse().code());

            try (ResultSet resultSet = statement.executeQuery()) {
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
    }

    public List<Warehouse> findAllWarehouses(Connection connection) throws SQLException {
        String sql = """
                SELECT warehouse_code, warehouse_name, city
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

    public List<ProductOption> findActiveProducts(Connection connection) throws SQLException {
        String sql = """
                SELECT p.product_code,
                       p.model_name,
                       m.manufacturer_name,
                       c.category_name
                FROM products p
                JOIN manufacturers m ON m.manufacturer_id = p.manufacturer_id
                JOIN categories c ON c.category_id = p.category_id
                WHERE p.active = TRUE
                ORDER BY c.category_name, m.manufacturer_name, p.model_name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ProductOption> products = new ArrayList<>();
            while (resultSet.next()) {
                products.add(new ProductOption(
                        resultSet.getString("product_code"),
                        resultSet.getString("model_name"),
                        resultSet.getString("manufacturer_name"),
                        resultSet.getString("category_name")
                ));
            }
            return products;
        }
    }
}
