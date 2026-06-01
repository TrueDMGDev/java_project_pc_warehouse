package com.pcwarehouse.repository;

import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AuthRepository {

    public UserRecord findByUsername(Connection connection, String username) throws SQLException {
        String sql = """
                SELECT u.username,
                       u.password_hash,
                       u.full_name,
                       u.active,
                       r.role_name,
                       w.warehouse_code,
                       w.warehouse_name,
                       w.city
                FROM users u
                JOIN roles r ON r.role_id = u.role_id
                LEFT JOIN warehouses w ON w.warehouse_id = u.warehouse_id
                WHERE u.username = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                Warehouse warehouse = null;
                String warehouseCode = resultSet.getString("warehouse_code");
                if (warehouseCode != null) {
                    warehouse = new Warehouse(
                            warehouseCode,
                            resultSet.getString("warehouse_name"),
                            resultSet.getString("city")
                    );
                }

                return new UserRecord(
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("full_name"),
                        resultSet.getBoolean("active"),
                        Role.valueOf(resultSet.getString("role_name")),
                        warehouse
                );
            }
        }
    }

    public void updatePasswordHash(Connection connection, String username, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, username);
            statement.executeUpdate();
        }
    }

    public record UserRecord(
            String username,
            String passwordHash,
            String fullName,
            boolean active,
            Role role,
            Warehouse warehouse
    ) {
        public UserSession toSession() {
            return new UserSession(username, fullName, role, warehouse);
        }
    }
}
