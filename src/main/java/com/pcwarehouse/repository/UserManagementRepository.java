package com.pcwarehouse.repository;

import com.pcwarehouse.model.ManagedUserRecord;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.WarehouseAssignmentOption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class UserManagementRepository {

    public List<ManagedUserRecord> findUsers(Connection connection, String searchTerm) throws SQLException {
        String sql = """
                SELECT u.username,
                       u.full_name,
                       r.role_name,
                       w.warehouse_code,
                       w.warehouse_name,
                       u.active
                FROM users u
                JOIN roles r ON r.role_id = u.role_id
                LEFT JOIN warehouses w ON w.warehouse_id = u.warehouse_id
                WHERE (? = ''
                       OR LOWER(u.username) LIKE ?
                       OR LOWER(u.full_name) LIKE ?)
                ORDER BY u.active DESC, u.username
                """;

        String normalizedSearch = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
        String wildcard = "%" + normalizedSearch + "%";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedSearch);
            statement.setString(2, wildcard);
            statement.setString(3, wildcard);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ManagedUserRecord> users = new ArrayList<>();
                while (resultSet.next()) {
                    String warehouseCode = resultSet.getString("warehouse_code");
                    String warehouseName = resultSet.getString("warehouse_name");
                    users.add(new ManagedUserRecord(
                            resultSet.getString("username"),
                            resultSet.getString("full_name"),
                            Role.valueOf(resultSet.getString("role_name")),
                            warehouseCode,
                            warehouseCode == null ? "Global / not assigned" : warehouseCode + " - " + warehouseName,
                            resultSet.getBoolean("active")
                    ));
                }
                return users;
            }
        }
    }

    public List<WarehouseAssignmentOption> findWarehouseAssignments(Connection connection) throws SQLException {
        String sql = """
                SELECT warehouse_code,
                       warehouse_name
                FROM warehouses
                WHERE active = TRUE
                ORDER BY warehouse_code
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<WarehouseAssignmentOption> options = new ArrayList<>();
            options.add(new WarehouseAssignmentOption("Global / not assigned", null));
            while (resultSet.next()) {
                options.add(new WarehouseAssignmentOption(
                        resultSet.getString("warehouse_code") + " - " + resultSet.getString("warehouse_name"),
                        resultSet.getString("warehouse_code")
                ));
            }
            return options;
        }
    }

    public void insertUser(
            Connection connection,
            String username,
            String passwordHash,
            String fullName,
            String roleName,
            String warehouseCode,
            boolean active
    ) throws SQLException {
        String sql = """
                INSERT INTO users (
                    username,
                    password_hash,
                    full_name,
                    role_id,
                    warehouse_id,
                    active
                )
                SELECT ?,
                       ?,
                       ?,
                       r.role_id,
                       w.warehouse_id,
                       ?
                FROM roles r
                LEFT JOIN warehouses w ON w.warehouse_code = ?
                WHERE r.role_name = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, fullName);
            statement.setBoolean(4, active);
            statement.setString(5, warehouseCode);
            statement.setString(6, roleName);
            statement.executeUpdate();
        }
    }

    public void updateUser(
            Connection connection,
            String originalUsername,
            String username,
            String fullName,
            String roleName,
            String warehouseCode,
            boolean active
    ) throws SQLException {
        String sql = """
                UPDATE users
                SET username = ?,
                    full_name = ?,
                    role_id = (SELECT role_id FROM roles WHERE role_name = ?),
                    warehouse_id = (SELECT warehouse_id FROM warehouses WHERE warehouse_code = ?),
                    active = ?
                WHERE username = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, fullName);
            statement.setString(3, roleName);
            statement.setString(4, warehouseCode);
            statement.setBoolean(5, active);
            statement.setString(6, originalUsername);
            statement.executeUpdate();
        }
    }

    public void updateUserWithPassword(
            Connection connection,
            String originalUsername,
            String username,
            String passwordHash,
            String fullName,
            String roleName,
            String warehouseCode,
            boolean active
    ) throws SQLException {
        String sql = """
                UPDATE users
                SET username = ?,
                    password_hash = ?,
                    full_name = ?,
                    role_id = (SELECT role_id FROM roles WHERE role_name = ?),
                    warehouse_id = (SELECT warehouse_id FROM warehouses WHERE warehouse_code = ?),
                    active = ?
                WHERE username = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, fullName);
            statement.setString(4, roleName);
            statement.setString(5, warehouseCode);
            statement.setBoolean(6, active);
            statement.setString(7, originalUsername);
            statement.executeUpdate();
        }
    }

    public void deleteUser(Connection connection, String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.executeUpdate();
        }
    }
}
