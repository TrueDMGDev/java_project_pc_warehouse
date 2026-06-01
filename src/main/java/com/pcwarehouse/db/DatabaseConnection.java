package com.pcwarehouse.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection open() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.getUrl(),
                DatabaseConfig.getUsername(),
                DatabaseConfig.getPassword()
        );
    }

    public static boolean canConnect() {
        return checkStatus().connected();
    }

    public static ConnectionStatus checkStatus() {
        try (Connection ignored = open()) {
            return new ConnectionStatus(true, "Database online", "Connected to " + DatabaseConfig.getUrl());
        } catch (SQLException exception) {
            return new ConnectionStatus(false, "Database offline", describeFailure(exception));
        }
    }

    public static String describeFailure(SQLException exception) {
        String state = exception.getSQLState() == null ? "n/a" : exception.getSQLState();
        return exception.getMessage() + " | SQLState: " + state;
    }

    public record ConnectionStatus(boolean connected, String message, String detail) {
    }
}
