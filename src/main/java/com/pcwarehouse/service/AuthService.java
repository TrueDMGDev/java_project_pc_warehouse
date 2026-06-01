package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.LoginResult;
import com.pcwarehouse.repository.AuthRepository;
import com.pcwarehouse.security.PasswordHasher;

import java.sql.Connection;
import java.sql.SQLException;

public final class AuthService {

    private final AuthRepository authRepository = new AuthRepository();

    public LoginResult authenticate(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedPassword = password == null ? "" : password;

        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            return new LoginResult(false, null, "Enter username and password.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            AuthRepository.UserRecord userRecord = authRepository.findByUsername(connection, normalizedUsername);
            if (userRecord == null) {
                return new LoginResult(false, null, "Unknown username.");
            }
            if (!userRecord.active()) {
                return new LoginResult(false, null, "This account is inactive.");
            }
            if (!PasswordHasher.verify(normalizedPassword, userRecord.passwordHash())) {
                return new LoginResult(false, null, "Incorrect password.");
            }
            if (userRecord.role() != com.pcwarehouse.model.Role.ADMIN && userRecord.warehouse() == null) {
                return new LoginResult(false, null, "This account is missing a warehouse assignment.");
            }
            if (PasswordHasher.needsUpgrade(userRecord.passwordHash())) {
                authRepository.updatePasswordHash(connection, userRecord.username(), PasswordHasher.hash(normalizedPassword));
            }

            return new LoginResult(true, userRecord.toSession(), "Login successful.");
        } catch (SQLException exception) {
            return new LoginResult(false, null, "Cannot connect to the application database. Check application.properties.");
        }
    }
}
