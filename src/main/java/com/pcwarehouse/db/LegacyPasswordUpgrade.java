package com.pcwarehouse.db;

import com.pcwarehouse.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class LegacyPasswordUpgrade {

    private LegacyPasswordUpgrade() {
    }

    static void upgradeSeedPasswords() throws SQLException {
        try (Connection connection = DatabaseConnection.open()) {
            String sql = """
                    SELECT username,
                           password_hash
                    FROM users
                    WHERE password_hash NOT LIKE 'pbkdf2_sha256$%'
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String username = resultSet.getString("username");
                    String storedPassword = resultSet.getString("password_hash");
                    if (username.equals(storedPassword)) {
                        updatePasswordHash(connection, username, PasswordHasher.hash(storedPassword));
                    }
                }
            }
        }
    }

    private static void updatePasswordHash(Connection connection, String username, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, username);
            statement.executeUpdate();
        }
    }
}
