package com.pcwarehouse.repository;

import com.pcwarehouse.model.CatalogLookupRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CatalogLookupRepository {

    public List<CatalogLookupRecord> findCategories(Connection connection, String searchTerm) throws SQLException {
        return findLookupRecords(connection, "categories", "category_name", searchTerm);
    }

    public List<CatalogLookupRecord> findManufacturers(Connection connection, String searchTerm) throws SQLException {
        return findLookupRecords(connection, "manufacturers", "manufacturer_name", searchTerm);
    }

    public void insertCategory(Connection connection, String name) throws SQLException {
        insertLookupRecord(connection, "categories", "category_name", name);
    }

    public void insertManufacturer(Connection connection, String name) throws SQLException {
        insertLookupRecord(connection, "manufacturers", "manufacturer_name", name);
    }

    public void updateCategory(Connection connection, String originalName, String newName) throws SQLException {
        updateLookupRecord(connection, "categories", "category_name", originalName, newName);
    }

    public void updateManufacturer(Connection connection, String originalName, String newName) throws SQLException {
        updateLookupRecord(connection, "manufacturers", "manufacturer_name", originalName, newName);
    }

    public void deleteCategory(Connection connection, String name) throws SQLException {
        deleteLookupRecord(connection, "categories", "category_name", name);
    }

    public void deleteManufacturer(Connection connection, String name) throws SQLException {
        deleteLookupRecord(connection, "manufacturers", "manufacturer_name", name);
    }

    public void forceDeleteCategory(Connection connection, String name) throws SQLException {
        forceDeleteProductsForLookup(
                connection,
                "categories",
                "category_id",
                "category_name",
                "category_id",
                name
        );
        deleteCategory(connection, name);
    }

    public void forceDeleteManufacturer(Connection connection, String name) throws SQLException {
        forceDeleteProductsForLookup(
                connection,
                "manufacturers",
                "manufacturer_id",
                "manufacturer_name",
                "manufacturer_id",
                name
        );
        deleteManufacturer(connection, name);
    }

    private List<CatalogLookupRecord> findLookupRecords(
            Connection connection,
            String tableName,
            String columnName,
            String searchTerm
    ) throws SQLException {
        String sql = """
                SELECT %s AS value
                FROM %s
                WHERE (? = '' OR LOWER(%s) LIKE ?)
                ORDER BY %s
                """.formatted(columnName, tableName, columnName, columnName);

        String normalizedSearch = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
        String wildcard = "%" + normalizedSearch + "%";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedSearch);
            statement.setString(2, wildcard);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<CatalogLookupRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(new CatalogLookupRecord(resultSet.getString("value")));
                }
                return records;
            }
        }
    }

    private void insertLookupRecord(Connection connection, String tableName, String columnName, String name) throws SQLException {
        String sql = "INSERT INTO " + tableName + " (" + columnName + ") VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private void updateLookupRecord(
            Connection connection,
            String tableName,
            String columnName,
            String originalName,
            String newName
    ) throws SQLException {
        String sql = "UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + columnName + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newName);
            statement.setString(2, originalName);
            statement.executeUpdate();
        }
    }

    private void deleteLookupRecord(Connection connection, String tableName, String columnName, String name) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + columnName + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private void forceDeleteProductsForLookup(
            Connection connection,
            String lookupTable,
            String lookupIdColumn,
            String lookupNameColumn,
            String productLookupColumn,
            String name
    ) throws SQLException {
        String productIdQuery = """
                SELECT p.product_id
                FROM products p
                JOIN %s l ON l.%s = p.%s
                WHERE l.%s = ?
                """.formatted(lookupTable, lookupIdColumn, productLookupColumn, lookupNameColumn);

        deleteByLookupName(connection, "DELETE FROM stock_movements WHERE product_id IN (" + productIdQuery + ")", name);
        deleteByLookupName(connection, "DELETE FROM request_items WHERE product_id IN (" + productIdQuery + ")", name);
        deleteByLookupName(connection, "DELETE FROM inventory WHERE product_id IN (" + productIdQuery + ")", name);
        deleteByLookupName(
                connection,
                "DELETE FROM products WHERE " + productLookupColumn
                        + " = (SELECT " + lookupIdColumn + " FROM " + lookupTable + " WHERE " + lookupNameColumn + " = ?)",
                name
        );
    }

    private void deleteByLookupName(Connection connection, String sql, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }
}
