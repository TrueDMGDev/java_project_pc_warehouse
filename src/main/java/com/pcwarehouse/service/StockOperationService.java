package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.MovementType;
import com.pcwarehouse.model.ProductOption;
import com.pcwarehouse.model.StockOperationLine;
import com.pcwarehouse.model.StockOperationResult;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.repository.CatalogRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class StockOperationService {

    private final CatalogRepository catalogRepository = new CatalogRepository();

    public List<Warehouse> loadWarehouses(UserSession session) {
        try (Connection connection = DatabaseConnection.open()) {
            return catalogRepository.findOperationWarehouses(connection, session);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<ProductOption> loadProducts() {
        try (Connection connection = DatabaseConnection.open()) {
            return catalogRepository.findActiveProducts(connection);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public boolean isLiveDatabaseAvailable() {
        return DatabaseConnection.canConnect();
    }

    public StockOperationResult supply(UserSession session, Warehouse warehouse, ProductOption product, int quantity, String details) {
        if (!session.role().canSupply()) {
            return new StockOperationResult(false, "This session cannot record supply operations.");
        }
        return execute(session, warehouse, product, quantity, null, null, details, MovementType.SUPPLY);
    }

    public StockOperationResult dispatch(UserSession session, Warehouse warehouse, ProductOption product, int quantity, String details) {
        if (!session.role().canDispatch()) {
            return new StockOperationResult(false, "This session cannot record dispatch operations.");
        }
        return execute(session, warehouse, product, quantity, null, null, details, MovementType.DISPATCH);
    }

    public StockOperationResult supplyLines(UserSession session, Warehouse warehouse, List<StockOperationLine> lines, String externalSource, String details) {
        if (!session.role().canSupply()) {
            return new StockOperationResult(false, "This session cannot record supply operations.");
        }
        return executeLines(session, warehouse, lines, externalSource, null, details, MovementType.SUPPLY);
    }

    public StockOperationResult dispatchLines(UserSession session, Warehouse warehouse, List<StockOperationLine> lines, String externalDestination, String details) {
        if (!session.role().canDispatch()) {
            return new StockOperationResult(false, "This session cannot record dispatch operations.");
        }
        return executeLines(session, warehouse, lines, null, externalDestination, details, MovementType.DISPATCH);
    }

    private StockOperationResult executeLines(
            UserSession session,
            Warehouse warehouse,
            List<StockOperationLine> lines,
            String externalSource,
            String externalDestination,
            String details,
            MovementType movementType
    ) {
        if (warehouse == null) {
            return new StockOperationResult(false, "Select a warehouse first.");
        }
        if (lines == null || lines.isEmpty()) {
            return new StockOperationResult(false, "Add at least one product line first.");
        }
        if (lines.stream().anyMatch(line -> line.quantity() <= 0)) {
            return new StockOperationResult(false, "All quantities must be positive numbers.");
        }
        if (!session.isAdmin() && !warehouse.code().equals(session.warehouse().code())) {
            return new StockOperationResult(false, "This session can only change stock in its own warehouse.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                long warehouseId = findWarehouseId(connection, warehouse.code());
                long userId = findUserId(connection, session.username());

                for (StockOperationLine line : lines) {
                    long productId = line.newProduct()
                            ? insertNewProduct(connection, line)
                            : findProductId(connection, line.productCode());
                    InventoryState inventoryState = findInventoryState(connection, warehouseId, productId);
                    int updatedQuantity = movementType == MovementType.SUPPLY
                            ? inventoryState.quantity() + line.quantity()
                            : inventoryState.quantity() - line.quantity();

                    if (movementType == MovementType.DISPATCH && updatedQuantity < 0) {
                        connection.rollback();
                        return new StockOperationResult(false, "Dispatch exceeds available stock for " + line.productName() + ".");
                    }

                    upsertInventory(connection, warehouseId, productId, updatedQuantity, inventoryState.exists());
                    insertMovement(connection, productId, warehouseId, userId, line.quantity(), externalSource, externalDestination, details, movementType);
                }

                connection.commit();
                String action = movementType == MovementType.SUPPLY ? "Supplied" : "Dispatched";
                return new StockOperationResult(true, action + " " + lines.size() + " product line(s).");
            } catch (SQLException exception) {
                connection.rollback();
                return new StockOperationResult(false, "Operation failed: " + exception.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new StockOperationResult(false, "Live database unavailable. Update application.properties and try again.");
        }
    }

    private StockOperationResult execute(
            UserSession session,
            Warehouse warehouse,
            ProductOption product,
            int quantity,
            String externalSource,
            String externalDestination,
            String details,
            MovementType movementType
    ) {
        if (warehouse == null || product == null) {
            return new StockOperationResult(false, "Select a warehouse and a product first.");
        }
        if (quantity <= 0) {
            return new StockOperationResult(false, "Quantity must be a positive number.");
        }
        if (!session.isAdmin() && !warehouse.code().equals(session.warehouse().code())) {
            return new StockOperationResult(false, "This session can only change stock in its own warehouse.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                long warehouseId = findWarehouseId(connection, warehouse.code());
                long productId = findProductId(connection, product.productCode());
                long userId = findUserId(connection, session.username());

                InventoryState inventoryState = findInventoryState(connection, warehouseId, productId);
                int currentQuantity = inventoryState.quantity();
                int updatedQuantity = movementType == MovementType.SUPPLY
                        ? currentQuantity + quantity
                        : currentQuantity - quantity;

                if (movementType == MovementType.DISPATCH && updatedQuantity < 0) {
                    connection.rollback();
                    return new StockOperationResult(false, "Dispatch exceeds available stock for the selected warehouse.");
                }

                upsertInventory(connection, warehouseId, productId, updatedQuantity, inventoryState.exists());
                insertMovement(connection, productId, warehouseId, userId, quantity, externalSource, externalDestination, details, movementType);
                connection.commit();

                String action = movementType == MovementType.SUPPLY ? "Supplied" : "Dispatched";
                return new StockOperationResult(true, action + " " + quantity + " units of " + product.productName() + ".");
            } catch (SQLException exception) {
                connection.rollback();
                return new StockOperationResult(false, "Operation failed: " + exception.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new StockOperationResult(false, "Live database unavailable. Update application.properties and try again.");
        }
    }

    private long findWarehouseId(Connection connection, String warehouseCode) throws SQLException {
        String sql = "SELECT warehouse_id FROM warehouses WHERE warehouse_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, warehouseCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("warehouse_id");
                }
            }
        }
        throw new SQLException("Warehouse not found: " + warehouseCode);
    }

    private long findProductId(Connection connection, String productCode) throws SQLException {
        String sql = "SELECT product_id FROM products WHERE product_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("product_id");
                }
            }
        }
        throw new SQLException("Product not found: " + productCode);
    }

    private long insertNewProduct(Connection connection, StockOperationLine line) throws SQLException {
        if (line.productCode() == null || line.productCode().isBlank()
                || line.productName() == null || line.productName().isBlank()
                || line.manufacturer() == null || line.manufacturer().isBlank()
                || line.category() == null || line.category().isBlank()) {
            throw new SQLException("New product lines need code, name, manufacturer, and category.");
        }

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
                RETURNING product_id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, line.productCode().trim());
            statement.setString(2, line.productName().trim());
            statement.setString(3, line.description() == null ? "" : line.description().trim());
            statement.setString(4, line.unitName() == null || line.unitName().isBlank() ? "pcs" : line.unitName().trim());
            statement.setString(5, line.category());
            statement.setString(6, line.manufacturer());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("product_id");
                }
            }
        }
        throw new SQLException("Unable to add new product " + line.productCode() + ". Check manufacturer/category.");
    }

    private long findUserId(Connection connection, String username) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("user_id");
                }
            }
        }
        throw new SQLException("User not found: " + username);
    }

    private InventoryState findInventoryState(Connection connection, long warehouseId, long productId) throws SQLException {
        String sql = "SELECT quantity FROM inventory WHERE warehouse_id = ? AND product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new InventoryState(true, resultSet.getInt("quantity"));
                }
            }
        }
        return new InventoryState(false, 0);
    }

    private void upsertInventory(Connection connection, long warehouseId, long productId, int updatedQuantity, boolean exists) throws SQLException {
        if (!exists) {
            String insertSql = "INSERT INTO inventory (warehouse_id, product_id, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setLong(1, warehouseId);
                statement.setLong(2, productId);
                statement.setInt(3, updatedQuantity);
                statement.executeUpdate();
            }
            return;
        }

        String updateSql = "UPDATE inventory SET quantity = ? WHERE warehouse_id = ? AND product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setInt(1, updatedQuantity);
            statement.setLong(2, warehouseId);
            statement.setLong(3, productId);
            statement.executeUpdate();
        }
    }

    private record InventoryState(boolean exists, int quantity) {
    }

    private void insertMovement(
            Connection connection,
            long productId,
            long warehouseId,
            long userId,
            int quantity,
            String externalSource,
            String externalDestination,
            String details,
            MovementType movementType
    ) throws SQLException {
        String sql = """
                INSERT INTO stock_movements (
                    product_id,
                    movement_type,
                    quantity,
                    source_warehouse_id,
                    destination_warehouse_id,
                    external_source,
                    external_destination,
                    performed_by_user_id,
                    details
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            statement.setString(2, movementType.name());
            statement.setInt(3, quantity);
            if (movementType == MovementType.SUPPLY) {
                statement.setNull(4, java.sql.Types.BIGINT);
                statement.setLong(5, warehouseId);
            } else {
                statement.setLong(4, warehouseId);
                statement.setNull(5, java.sql.Types.BIGINT);
            }
            statement.setString(6, movementType == MovementType.SUPPLY ? normalizeRouteLabel(externalSource) : null);
            statement.setString(7, movementType == MovementType.DISPATCH ? normalizeRouteLabel(externalDestination) : null);
            statement.setLong(8, userId);
            statement.setString(9, normalizeDetails(details));
            statement.executeUpdate();
        }
    }

    private String normalizeDetails(String details) {
        return normalizeText(details, 255);
    }

    private String normalizeRouteLabel(String label) {
        return normalizeText(label, 120);
    }

    private String normalizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.length() <= maxLength ? normalizedValue : normalizedValue.substring(0, maxLength);
    }
}
