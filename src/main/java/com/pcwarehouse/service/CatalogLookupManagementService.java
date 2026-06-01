package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.CatalogLookupChangeResult;
import com.pcwarehouse.model.CatalogLookupRecord;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.repository.CatalogLookupRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class CatalogLookupManagementService {

    private final CatalogLookupRepository repository = new CatalogLookupRepository();

    public List<CatalogLookupRecord> loadCategories(String searchTerm) {
        try (Connection connection = DatabaseConnection.open()) {
            return repository.findCategories(connection, searchTerm);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<CatalogLookupRecord> loadManufacturers(String searchTerm) {
        try (Connection connection = DatabaseConnection.open()) {
            return repository.findManufacturers(connection, searchTerm);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public CatalogLookupChangeResult createCategory(UserSession session, String name) {
        return createLookup(session, name, true);
    }

    public CatalogLookupChangeResult createManufacturer(UserSession session, String name) {
        return createLookup(session, name, false);
    }

    public CatalogLookupChangeResult updateCategory(UserSession session, String originalName, String newName) {
        return updateLookup(session, originalName, newName, true);
    }

    public CatalogLookupChangeResult updateManufacturer(UserSession session, String originalName, String newName) {
        return updateLookup(session, originalName, newName, false);
    }

    public CatalogLookupChangeResult deleteCategory(UserSession session, CatalogLookupRecord record) {
        return deleteLookup(session, record, true);
    }

    public CatalogLookupChangeResult deleteManufacturer(UserSession session, CatalogLookupRecord record) {
        return deleteLookup(session, record, false);
    }

    public CatalogLookupChangeResult forceDeleteCategory(UserSession session, CatalogLookupRecord record) {
        return forceDeleteLookup(session, record, true);
    }

    public CatalogLookupChangeResult forceDeleteManufacturer(UserSession session, CatalogLookupRecord record) {
        return forceDeleteLookup(session, record, false);
    }

    private CatalogLookupChangeResult createLookup(UserSession session, String name, boolean category) {
        if (!canManageCatalog(session)) {
            return new CatalogLookupChangeResult(false, "Only admin can manage catalog reference data.");
        }
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return new CatalogLookupChangeResult(false, label(category) + " name is required.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            if (category) {
                repository.insertCategory(connection, normalizedName);
            } else {
                repository.insertManufacturer(connection, normalizedName);
            }
            return new CatalogLookupChangeResult(true, "Added " + label(category).toLowerCase() + " " + normalizedName + ".");
        } catch (SQLException exception) {
            return new CatalogLookupChangeResult(false, "Create failed: " + describeConstraintIssue(exception, category));
        }
    }

    private CatalogLookupChangeResult updateLookup(UserSession session, String originalName, String newName, boolean category) {
        if (!canManageCatalog(session)) {
            return new CatalogLookupChangeResult(false, "Only admin can manage catalog reference data.");
        }
        if (originalName == null || originalName.isBlank()) {
            return new CatalogLookupChangeResult(false, "Select a " + label(category).toLowerCase() + " row to update.");
        }
        String normalizedName = normalizeName(newName);
        if (normalizedName == null) {
            return new CatalogLookupChangeResult(false, label(category) + " name is required.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            if (category) {
                repository.updateCategory(connection, originalName, normalizedName);
            } else {
                repository.updateManufacturer(connection, originalName, normalizedName);
            }
            return new CatalogLookupChangeResult(true, "Updated " + label(category).toLowerCase() + " to " + normalizedName + ".");
        } catch (SQLException exception) {
            return new CatalogLookupChangeResult(false, "Update failed: " + describeConstraintIssue(exception, category));
        }
    }

    private CatalogLookupChangeResult deleteLookup(UserSession session, CatalogLookupRecord record, boolean category) {
        if (!canManageCatalog(session)) {
            return new CatalogLookupChangeResult(false, "Only admin can manage catalog reference data.");
        }
        if (record == null) {
            return new CatalogLookupChangeResult(false, "Select a " + label(category).toLowerCase() + " row to delete.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            if (category) {
                repository.deleteCategory(connection, record.name());
            } else {
                repository.deleteManufacturer(connection, record.name());
            }
            return new CatalogLookupChangeResult(true, "Deleted " + label(category).toLowerCase() + " " + record.name() + ".");
        } catch (SQLException exception) {
            return new CatalogLookupChangeResult(false, "Delete failed: " + describeConstraintIssue(exception, category));
        }
    }

    private CatalogLookupChangeResult forceDeleteLookup(UserSession session, CatalogLookupRecord record, boolean category) {
        if (!canManageCatalog(session)) {
            return new CatalogLookupChangeResult(false, "Only admin can manage catalog reference data.");
        }
        if (record == null) {
            return new CatalogLookupChangeResult(false, "Select a " + label(category).toLowerCase() + " row to force delete.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                if (category) {
                    repository.forceDeleteCategory(connection, record.name());
                } else {
                    repository.forceDeleteManufacturer(connection, record.name());
                }
                connection.commit();
                return new CatalogLookupChangeResult(
                        true,
                        "Force deleted " + label(category).toLowerCase() + " " + record.name() + " and related records."
                );
            } catch (SQLException exception) {
                connection.rollback();
                return new CatalogLookupChangeResult(false, "Force delete failed: " + describeConstraintIssue(exception, category));
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new CatalogLookupChangeResult(false, "Force delete failed: " + describeConstraintIssue(exception, category));
        }
    }

    private boolean canManageCatalog(UserSession session) {
        return session != null && session.role() == Role.ADMIN;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private String label(boolean category) {
        return category ? "Category" : "Manufacturer";
    }

    private String describeConstraintIssue(SQLException exception, boolean category) {
        return switch (exception.getSQLState()) {
            case "23505" -> "that " + label(category).toLowerCase() + " already exists.";
            case "23503" -> "that " + label(category).toLowerCase() + " is already used by products.";
            default -> exception.getMessage();
        };
    }
}
