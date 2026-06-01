package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.ManagedUserRecord;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.UserChangeResult;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.WarehouseAssignmentOption;
import com.pcwarehouse.repository.UserManagementRepository;
import com.pcwarehouse.security.PasswordHasher;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class UserManagementService {

    private final UserManagementRepository userManagementRepository = new UserManagementRepository();

    public List<ManagedUserRecord> loadUsers(String searchTerm) {
        try (Connection connection = DatabaseConnection.open()) {
            return userManagementRepository.findUsers(connection, searchTerm);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<WarehouseAssignmentOption> loadWarehouseAssignments() {
        try (Connection connection = DatabaseConnection.open()) {
            return userManagementRepository.findWarehouseAssignments(connection);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public UserChangeResult createUser(
            UserSession session,
            String username,
            String password,
            String fullName,
            Role role,
            WarehouseAssignmentOption warehouseAssignment,
            boolean active
    ) {
        if (!canManageUsers(session)) {
            return new UserChangeResult(false, "Only admin can add or edit users.");
        }

        ValidationResult validation = validate(username, password, fullName, role, warehouseAssignment, true);
        if (!validation.valid()) {
            return new UserChangeResult(false, validation.message());
        }

        try (Connection connection = DatabaseConnection.open()) {
            userManagementRepository.insertUser(
                    connection,
                    username.trim(),
                    PasswordHasher.hash(password.trim()),
                    fullName.trim(),
                    role.name(),
                    normalizeWarehouseCode(role, warehouseAssignment),
                    active
            );
            return new UserChangeResult(true, "Added user " + username.trim() + ".");
        } catch (SQLException exception) {
            return new UserChangeResult(false, "Create failed: " + describeConstraintIssue(exception));
        }
    }

    public UserChangeResult updateUser(
            UserSession session,
            String originalUsername,
            String username,
            String password,
            String fullName,
            Role role,
            WarehouseAssignmentOption warehouseAssignment,
            boolean active
    ) {
        if (!canManageUsers(session)) {
            return new UserChangeResult(false, "Only admin can add or edit users.");
        }
        if (originalUsername == null || originalUsername.isBlank()) {
            return new UserChangeResult(false, "Select a user row to update.");
        }
        if (session.username().equals(originalUsername)) {
            return new UserChangeResult(false, "Use a different admin account to edit the currently signed-in user.");
        }

        ValidationResult validation = validate(username, password, fullName, role, warehouseAssignment, false);
        if (!validation.valid()) {
            return new UserChangeResult(false, validation.message());
        }

        try (Connection connection = DatabaseConnection.open()) {
            String normalizedPassword = password == null ? "" : password.trim();
            if (normalizedPassword.isBlank()) {
                userManagementRepository.updateUser(
                        connection,
                        originalUsername,
                        username.trim(),
                        fullName.trim(),
                        role.name(),
                        normalizeWarehouseCode(role, warehouseAssignment),
                        active
                );
            } else {
                userManagementRepository.updateUserWithPassword(
                        connection,
                        originalUsername,
                        username.trim(),
                        PasswordHasher.hash(normalizedPassword),
                        fullName.trim(),
                        role.name(),
                        normalizeWarehouseCode(role, warehouseAssignment),
                        active
                );
            }
            return new UserChangeResult(true, "Updated user " + username.trim() + ".");
        } catch (SQLException exception) {
            return new UserChangeResult(false, "Update failed: " + describeConstraintIssue(exception));
        }
    }

    public UserChangeResult deleteUser(UserSession session, ManagedUserRecord user) {
        if (!canManageUsers(session)) {
            return new UserChangeResult(false, "Only admin can delete users.");
        }
        if (user == null) {
            return new UserChangeResult(false, "Select a user row to delete.");
        }
        if (session.username().equals(user.username())) {
            return new UserChangeResult(false, "Use a different admin account to delete the currently signed-in user.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            userManagementRepository.deleteUser(connection, user.username());
            return new UserChangeResult(true, "Deleted user " + user.username() + ".");
        } catch (SQLException exception) {
            return new UserChangeResult(false, "Delete failed: " + describeConstraintIssue(exception));
        }
    }

    private boolean canManageUsers(UserSession session) {
        return session != null && session.role().canManageUsers();
    }

    private ValidationResult validate(
            String username,
            String password,
            String fullName,
            Role role,
            WarehouseAssignmentOption warehouseAssignment,
            boolean creating
    ) {
        if (username == null || username.isBlank()) {
            return new ValidationResult(false, "Username is required.");
        }
        if (fullName == null || fullName.isBlank()) {
            return new ValidationResult(false, "Full name is required.");
        }
        if (role == null) {
            return new ValidationResult(false, "Choose a role.");
        }
        if (creating && (password == null || password.isBlank())) {
            return new ValidationResult(false, "Password is required when creating a user.");
        }
        if (role != Role.ADMIN && (warehouseAssignment == null || !warehouseAssignment.isAssignedWarehouse())) {
            return new ValidationResult(false, "Choose a warehouse for non-admin users.");
        }
        return new ValidationResult(true, "");
    }

    private String normalizeWarehouseCode(Role role, WarehouseAssignmentOption warehouseAssignment) {
        if (role == Role.ADMIN) {
            return null;
        }
        return warehouseAssignment == null ? null : warehouseAssignment.warehouseCode();
    }

    private String describeConstraintIssue(SQLException exception) {
        return switch (exception.getSQLState()) {
            case "23505" -> "a user with that username already exists.";
            case "23503" -> "that user record is still referenced elsewhere in the system.";
            default -> exception.getMessage();
        };
    }

    private record ValidationResult(boolean valid, String message) {
    }
}
