package com.pcwarehouse.repository;

import com.pcwarehouse.model.ProductRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ProductRepository {

    public List<ProductRecord> findProducts(Connection connection, String searchTerm) throws SQLException {
        String sql = """
                SELECT p.product_code,
                       p.model_name,
                       m.manufacturer_name,
                       c.category_name,
                       p.unit_name,
                       COALESCE(p.description, '') AS description,
                       p.active
                FROM products p
                JOIN manufacturers m ON m.manufacturer_id = p.manufacturer_id
                JOIN categories c ON c.category_id = p.category_id
                WHERE (? = '' OR LOWER(p.product_code) LIKE ? OR LOWER(p.model_name) LIKE ?)
                ORDER BY c.category_name, m.manufacturer_name, p.model_name
                """;

        String normalizedSearch = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
        String wildcard = "%" + normalizedSearch + "%";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedSearch);
            statement.setString(2, wildcard);
            statement.setString(3, wildcard);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProductRecord> products = new ArrayList<>();
                while (resultSet.next()) {
                    products.add(new ProductRecord(
                            resultSet.getString("product_code"),
                            resultSet.getString("model_name"),
                            resultSet.getString("manufacturer_name"),
                            resultSet.getString("category_name"),
                            resultSet.getString("unit_name"),
                            resultSet.getString("description"),
                            resultSet.getBoolean("active")
                    ));
                }
                return products;
            }
        }
    }

    public List<String> findManufacturers(Connection connection) throws SQLException {
        return findLookupValues(connection, "SELECT manufacturer_name AS value FROM manufacturers ORDER BY manufacturer_name");
    }

    public List<String> findCategories(Connection connection) throws SQLException {
        return findLookupValues(connection, "SELECT category_name AS value FROM categories ORDER BY category_name");
    }

    public void insertProduct(
            Connection connection,
            String productCode,
            String modelName,
            String manufacturer,
            String category,
            String unitName,
            String description
    ) throws SQLException {
        String sql = """
                INSERT INTO products (
                    product_code,
                    manufacturer_id,
                    category_id,
                    model_name,
                    description,
                    unit_name,
                    active
                )
                SELECT ?,
                       m.manufacturer_id,
                       c.category_id,
                       ?,
                       ?,
                       ?,
                       TRUE
                FROM manufacturers m
                JOIN categories c ON c.category_name = ?
                WHERE m.manufacturer_name = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productCode);
            statement.setString(2, modelName);
            statement.setString(3, description);
            statement.setString(4, unitName);
            statement.setString(5, category);
            statement.setString(6, manufacturer);
            statement.executeUpdate();
        }
    }

    public void updateProduct(
            Connection connection,
            String originalProductCode,
            String newProductCode,
            String modelName,
            String manufacturer,
            String category,
            String unitName,
            String description
    ) throws SQLException {
        String sql = """
                UPDATE products
                SET product_code = ?,
                    manufacturer_id = (SELECT manufacturer_id FROM manufacturers WHERE manufacturer_name = ?),
                    category_id = (SELECT category_id FROM categories WHERE category_name = ?),
                    model_name = ?,
                    description = ?,
                    unit_name = ?
                WHERE product_code = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newProductCode);
            statement.setString(2, manufacturer);
            statement.setString(3, category);
            statement.setString(4, modelName);
            statement.setString(5, description);
            statement.setString(6, unitName);
            statement.setString(7, originalProductCode);
            statement.executeUpdate();
        }
    }

    public void deleteProduct(Connection connection, String productCode) throws SQLException {
        String sql = "DELETE FROM products WHERE product_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productCode);
            statement.executeUpdate();
        }
    }

    private List<String> findLookupValues(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<String> values = new ArrayList<>();
            while (resultSet.next()) {
                values.add(resultSet.getString("value"));
            }
            return values;
        }
    }
}
