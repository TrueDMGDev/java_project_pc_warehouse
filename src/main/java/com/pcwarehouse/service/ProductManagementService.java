package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.ProductChangeResult;
import com.pcwarehouse.model.ProductRecord;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.repository.ProductRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

public final class ProductManagementService {

    private final ProductRepository productRepository = new ProductRepository();

    public List<ProductRecord> loadProducts(String searchTerm) {
        try (Connection connection = DatabaseConnection.open()) {
            return productRepository.findProducts(connection, searchTerm);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<String> loadManufacturers() {
        try (Connection connection = DatabaseConnection.open()) {
            return productRepository.findManufacturers(connection);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<String> loadCategories() {
        try (Connection connection = DatabaseConnection.open()) {
            return productRepository.findCategories(connection);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public ProductChangeResult createProduct(
            UserSession session,
            String productCode,
            String modelName,
            String manufacturer,
            String category,
            String unitName,
            String description
    ) {
        if (!canManageProducts(session)) {
            return new ProductChangeResult(false, "Only admin can add or edit products.");
        }

        ValidationResult validation = validate(productCode, modelName, manufacturer, category, unitName);
        if (!validation.valid()) {
            return new ProductChangeResult(false, validation.message());
        }

        try (Connection connection = DatabaseConnection.open()) {
            productRepository.insertProduct(
                    connection,
                    productCode.trim(),
                    modelName.trim(),
                    manufacturer,
                    category,
                    unitName.trim(),
                    normalizeDescription(description)
            );
            return new ProductChangeResult(true, "Added product " + productCode.trim() + ".");
        } catch (SQLIntegrityConstraintViolationException exception) {
            return new ProductChangeResult(false, "Product code already exists or references are invalid.");
        } catch (SQLException exception) {
            return new ProductChangeResult(false, "Create failed: " + exception.getMessage());
        }
    }

    public ProductChangeResult updateProduct(
            UserSession session,
            String originalProductCode,
            String productCode,
            String modelName,
            String manufacturer,
            String category,
            String unitName,
            String description
    ) {
        if (!canManageProducts(session)) {
            return new ProductChangeResult(false, "Only admin can add or edit products.");
        }
        if (originalProductCode == null || originalProductCode.isBlank()) {
            return new ProductChangeResult(false, "Select a product row to update.");
        }

        ValidationResult validation = validate(productCode, modelName, manufacturer, category, unitName);
        if (!validation.valid()) {
            return new ProductChangeResult(false, validation.message());
        }

        try (Connection connection = DatabaseConnection.open()) {
            productRepository.updateProduct(
                    connection,
                    originalProductCode,
                    productCode.trim(),
                    modelName.trim(),
                    manufacturer,
                    category,
                    unitName.trim(),
                    normalizeDescription(description)
            );
            return new ProductChangeResult(true, "Updated product " + productCode.trim() + ".");
        } catch (SQLIntegrityConstraintViolationException exception) {
            return new ProductChangeResult(false, "Update failed because the new code already exists or data is in use.");
        } catch (SQLException exception) {
            return new ProductChangeResult(false, "Update failed: " + exception.getMessage());
        }
    }

    public ProductChangeResult deleteProduct(UserSession session, ProductRecord product) {
        if (!canManageProducts(session)) {
            return new ProductChangeResult(false, "Only admin can delete products.");
        }
        if (product == null) {
            return new ProductChangeResult(false, "Select a product row to delete.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            productRepository.deleteProduct(connection, product.productCode());
            return new ProductChangeResult(true, "Deleted product " + product.productCode() + ".");
        } catch (SQLIntegrityConstraintViolationException exception) {
            return new ProductChangeResult(false, "Delete blocked because this product is referenced by inventory, requests, or movement history.");
        } catch (SQLException exception) {
            return new ProductChangeResult(false, "Delete failed: " + exception.getMessage());
        }
    }

    private boolean canManageProducts(UserSession session) {
        return session != null && session.role() == Role.ADMIN;
    }

    private ValidationResult validate(String productCode, String modelName, String manufacturer, String category, String unitName) {
        if (productCode == null || productCode.isBlank()) {
            return new ValidationResult(false, "Product code is required.");
        }
        if (modelName == null || modelName.isBlank()) {
            return new ValidationResult(false, "Model name is required.");
        }
        if (manufacturer == null || manufacturer.isBlank()) {
            return new ValidationResult(false, "Choose a manufacturer.");
        }
        if (category == null || category.isBlank()) {
            return new ValidationResult(false, "Choose a category.");
        }
        if (unitName == null || unitName.isBlank()) {
            return new ValidationResult(false, "Unit is required.");
        }
        return new ValidationResult(true, "");
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private record ValidationResult(boolean valid, String message) {
    }
}
