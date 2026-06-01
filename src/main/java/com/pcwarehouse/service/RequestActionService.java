package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.ProductOption;
import com.pcwarehouse.model.RequestActionResult;
import com.pcwarehouse.model.RequestDraftLine;
import com.pcwarehouse.model.RequestStatus;
import com.pcwarehouse.model.RequestSummary;
import com.pcwarehouse.model.RequestViewMode;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.repository.CatalogRepository;
import com.pcwarehouse.repository.RequestRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class RequestActionService {

    private final CatalogRepository catalogRepository = new CatalogRepository();
    private final RequestRepository requestRepository = new RequestRepository();

    public List<Warehouse> loadSourceWarehouses(UserSession session) {
        try (Connection connection = DatabaseConnection.open()) {
            return catalogRepository.findOperationWarehouses(connection, session);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<Warehouse> loadAllWarehouses() {
        try (Connection connection = DatabaseConnection.open()) {
            return catalogRepository.findAllWarehouses(connection);
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

    public List<InventorySummary> loadTargetInventory(Warehouse targetWarehouse, String searchTerm) {
        if (targetWarehouse == null) {
            return List.of();
        }

        try (Connection connection = DatabaseConnection.open()) {
            return requestRepository.findRequestableInventory(connection, targetWarehouse.code(), searchTerm);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<RequestSummary> loadRequests(UserSession session, RequestViewMode viewMode) {
        if (session == null) {
            return List.of();
        }

        try (Connection connection = DatabaseConnection.open()) {
            return requestRepository.findVisibleRequests(connection, session, viewMode);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public RequestActionResult createRequest(
            UserSession session,
            Warehouse sourceWarehouse,
            Warehouse targetWarehouse,
            ProductOption product,
            int quantity,
            String note
    ) {
        if (product == null) {
            return new RequestActionResult(false, "Select a product to add to the request.");
        }
        return createRequest(
                session,
                sourceWarehouse,
                targetWarehouse,
                List.of(new RequestDraftLine(
                        product.productCode(),
                        product.productName(),
                        product.manufacturer(),
                        product.category(),
                        quantity
                )),
                note
        );
    }

    public RequestActionResult createRequest(
            UserSession session,
            Warehouse sourceWarehouse,
            Warehouse targetWarehouse,
            List<RequestDraftLine> lines,
            String note
    ) {
        if (!session.role().canCreateRequests()) {
            return new RequestActionResult(false, "This session cannot create requests.");
        }
        if (sourceWarehouse == null || targetWarehouse == null) {
            return new RequestActionResult(false, "Select source warehouse and target warehouse.");
        }
        if (sourceWarehouse.code().equals(targetWarehouse.code())) {
            return new RequestActionResult(false, "Source and target warehouse must be different.");
        }
        if (!session.isAdmin() && !sourceWarehouse.code().equals(session.warehouse().code())) {
            return new RequestActionResult(false, "This session can only create requests for its own warehouse.");
        }
        if (lines == null || lines.isEmpty()) {
            return new RequestActionResult(false, "Add at least one request line before submitting.");
        }
        boolean hasInvalidQuantity = lines.stream().anyMatch(line -> line.quantity() <= 0);
        if (hasInvalidQuantity) {
            return new RequestActionResult(false, "All request lines must have a positive quantity.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                long sourceWarehouseId = findWarehouseId(connection, sourceWarehouse.code());
                long targetWarehouseId = findWarehouseId(connection, targetWarehouse.code());
                long userId = findUserId(connection, session.username());
                String requestNumber = nextRequestNumber(connection);
                validateRequestedQuantitiesAvailable(connection, targetWarehouseId, lines);

                long requestId = insertRequest(connection, requestNumber, sourceWarehouseId, targetWarehouseId, userId, note);
                for (RequestDraftLine line : lines) {
                    long productId = findProductId(connection, line.productCode());
                    insertRequestItem(connection, requestId, productId, line.quantity());
                }
                connection.commit();

                return new RequestActionResult(true, "Created request " + requestNumber + " with " + lines.size() + " line(s).");
            } catch (SQLException exception) {
                connection.rollback();
                return new RequestActionResult(false, "Request creation failed: " + exception.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new RequestActionResult(false, "Live database unavailable. Update application.properties and try again.");
        }
    }

    public RequestActionResult approveRequest(UserSession session, RequestSummary requestSummary) {
        if (!session.role().canApproveRequests()) {
            return new RequestActionResult(false, "This session cannot approve requests.");
        }
        if (requestSummary == null) {
            return new RequestActionResult(false, "Select a request to approve.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                RequestContext requestContext = loadRequestContext(connection, requestSummary.requestNumber());
                if (requestContext == null) {
                    connection.rollback();
                    return new RequestActionResult(false, "The selected request no longer exists.");
                }
                if (requestContext.status() != RequestStatus.PENDING) {
                    connection.rollback();
                    return new RequestActionResult(false, "Only pending requests can be approved.");
                }
                if (!session.isAdmin() && !requestContext.destinationWarehouseCode().equals(session.warehouse().code())) {
                    connection.rollback();
                    return new RequestActionResult(false, "Only the target warehouse can approve this request.");
                }

                long actingUserId = findUserId(connection, session.username());
                List<RequestItemContext> requestItems = loadRequestItems(connection, requestContext.requestId());
                for (RequestItemContext item : requestItems) {
                    int availableQuantity = findInventoryState(connection, requestContext.destinationWarehouseId(), item.productId()).quantity();
                    if (availableQuantity < item.quantityRequested()) {
                        connection.rollback();
                        return new RequestActionResult(false,
                                "Not enough stock in " + requestContext.destinationWarehouseCode() + " for " + item.productName() + ".");
                    }
                }

                for (RequestItemContext item : requestItems) {
                    moveInventory(connection, requestContext.destinationWarehouseId(), item.productId(), -item.quantityRequested());
                    moveInventory(connection, requestContext.sourceWarehouseId(), item.productId(), item.quantityRequested());
                    insertTransferMovement(
                            connection,
                            item.productId(),
                            requestContext.destinationWarehouseId(),
                            requestContext.sourceWarehouseId(),
                            requestContext.requestId(),
                            actingUserId,
                            item.quantityRequested(),
                            "Approved " + requestContext.requestNumber(),
                            "TRANSFER_OUT"
                    );
                    insertTransferMovement(
                            connection,
                            item.productId(),
                            requestContext.destinationWarehouseId(),
                            requestContext.sourceWarehouseId(),
                            requestContext.requestId(),
                            actingUserId,
                            item.quantityRequested(),
                            "Approved " + requestContext.requestNumber(),
                            "TRANSFER_IN"
                    );
                    updateApprovedQuantity(connection, item.requestItemId(), item.quantityRequested());
                }

                updateRequestStatus(connection, requestContext.requestId(), RequestStatus.APPROVED, actingUserId);
                connection.commit();
                return new RequestActionResult(true, "Approved " + requestContext.requestNumber() + " and moved the stock.");
            } catch (SQLException exception) {
                connection.rollback();
                return new RequestActionResult(false, "Approval failed: " + exception.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new RequestActionResult(false, "Live database unavailable. Update application.properties and try again.");
        }
    }

    public RequestActionResult rejectRequest(UserSession session, RequestSummary requestSummary) {
        if (!session.role().canApproveRequests()) {
            return new RequestActionResult(false, "This session cannot reject requests.");
        }
        if (requestSummary == null) {
            return new RequestActionResult(false, "Select a request to reject.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                RequestContext requestContext = loadRequestContext(connection, requestSummary.requestNumber());
                if (requestContext == null) {
                    connection.rollback();
                    return new RequestActionResult(false, "The selected request no longer exists.");
                }
                if (requestContext.status() != RequestStatus.PENDING) {
                    connection.rollback();
                    return new RequestActionResult(false, "Only pending requests can be rejected.");
                }
                if (!session.isAdmin() && !requestContext.destinationWarehouseCode().equals(session.warehouse().code())) {
                    connection.rollback();
                    return new RequestActionResult(false, "Only the target warehouse can reject this request.");
                }

                updateRequestStatus(connection, requestContext.requestId(), RequestStatus.REJECTED, findUserId(connection, session.username()));
                connection.commit();
                return new RequestActionResult(true, "Rejected " + requestContext.requestNumber() + ".");
            } catch (SQLException exception) {
                connection.rollback();
                return new RequestActionResult(false, "Reject failed: " + exception.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new RequestActionResult(false, "Live database unavailable. Update application.properties and try again.");
        }
    }

    public RequestActionResult updatePendingRequest(
            UserSession session,
            String requestNumber,
            Warehouse sourceWarehouse,
            Warehouse targetWarehouse,
            List<RequestDraftLine> lines,
            String note
    ) {
        if (session == null) {
            return new RequestActionResult(false, "Sign in before editing requests.");
        }
        if (requestNumber == null || requestNumber.isBlank()) {
            return new RequestActionResult(false, "Load a pending outgoing request before saving edits.");
        }

        RequestActionResult validation = validateRequestDraft(session, sourceWarehouse, targetWarehouse, lines);
        if (!validation.success()) {
            return validation;
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                RequestContext requestContext = loadRequestContext(connection, requestNumber);
                if (requestContext == null) {
                    connection.rollback();
                    return new RequestActionResult(false, "The selected request no longer exists.");
                }
                if (requestContext.status() != RequestStatus.PENDING) {
                    connection.rollback();
                    return new RequestActionResult(false, "Only pending requests can be edited.");
                }
                if (!canModifyPendingRequest(session, requestContext)) {
                    connection.rollback();
                    return new RequestActionResult(false, "Only the manager who created this pending request or admin can edit it.");
                }

                long sourceWarehouseId = findWarehouseId(connection, sourceWarehouse.code());
                long targetWarehouseId = findWarehouseId(connection, targetWarehouse.code());
                validateRequestedQuantitiesAvailable(connection, targetWarehouseId, lines);
                updatePendingRequestRow(connection, requestContext.requestId(), sourceWarehouseId, targetWarehouseId, note);
                deleteRequestItems(connection, requestContext.requestId());
                for (RequestDraftLine line : lines) {
                    long productId = findProductId(connection, line.productCode());
                    insertRequestItem(connection, requestContext.requestId(), productId, line.quantity());
                }

                connection.commit();
                return new RequestActionResult(true, "Updated " + requestContext.requestNumber() + ".");
            } catch (SQLException exception) {
                connection.rollback();
                return new RequestActionResult(false, "Request update failed: " + exception.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new RequestActionResult(false, "Live database unavailable. Update application.properties and try again.");
        }
    }

    public RequestActionResult cancelRequest(UserSession session, RequestSummary requestSummary) {
        if (session == null) {
            return new RequestActionResult(false, "Sign in before cancelling requests.");
        }
        if (requestSummary == null) {
            return new RequestActionResult(false, "Select a pending request to cancel.");
        }

        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                RequestContext requestContext = loadRequestContext(connection, requestSummary.requestNumber());
                if (requestContext == null) {
                    connection.rollback();
                    return new RequestActionResult(false, "The selected request no longer exists.");
                }
                if (requestContext.status() != RequestStatus.PENDING) {
                    connection.rollback();
                    return new RequestActionResult(false, "Only pending requests can be cancelled.");
                }
                if (!canModifyPendingRequest(session, requestContext)) {
                    connection.rollback();
                    return new RequestActionResult(false, "Only the manager who created this pending request or admin can cancel it.");
                }

                updateRequestStatus(connection, requestContext.requestId(), RequestStatus.CANCELLED, findUserId(connection, session.username()));
                connection.commit();
                return new RequestActionResult(true, "Cancelled " + requestContext.requestNumber() + ".");
            } catch (SQLException exception) {
                connection.rollback();
                return new RequestActionResult(false, "Cancel failed: " + exception.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return new RequestActionResult(false, "Live database unavailable. Update application.properties and try again.");
        }
    }

    private RequestActionResult validateRequestDraft(
            UserSession session,
            Warehouse sourceWarehouse,
            Warehouse targetWarehouse,
            List<RequestDraftLine> lines
    ) {
        if (!session.role().canCreateRequests()) {
            return new RequestActionResult(false, "This session cannot create or edit requests.");
        }
        if (sourceWarehouse == null || targetWarehouse == null) {
            return new RequestActionResult(false, "Select source warehouse and target warehouse.");
        }
        if (sourceWarehouse.code().equals(targetWarehouse.code())) {
            return new RequestActionResult(false, "Source and target warehouse must be different.");
        }
        if (!session.isAdmin() && !sourceWarehouse.code().equals(session.warehouse().code())) {
            return new RequestActionResult(false, "This session can only use its own warehouse as the request source.");
        }
        if (lines == null || lines.isEmpty()) {
            return new RequestActionResult(false, "Add at least one request line before saving.");
        }
        boolean hasInvalidQuantity = lines.stream().anyMatch(line -> line.quantity() <= 0);
        if (hasInvalidQuantity) {
            return new RequestActionResult(false, "All request lines must have a positive quantity.");
        }
        return new RequestActionResult(true, "");
    }

    private long insertRequest(Connection connection, String requestNumber, long sourceWarehouseId, long targetWarehouseId, long userId, String note)
            throws SQLException {
        String sql = """
                INSERT INTO requests (
                    request_number,
                    from_warehouse_id,
                    to_warehouse_id,
                    requested_by_user_id,
                    request_status,
                    request_note
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, requestNumber);
            statement.setLong(2, sourceWarehouseId);
            statement.setLong(3, targetWarehouseId);
            statement.setLong(4, userId);
            statement.setString(5, RequestStatus.PENDING.name());
            statement.setString(6, note == null || note.isBlank() ? null : note.trim());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to create request.");
    }

    private void insertRequestItem(Connection connection, long requestId, long productId, int quantity) throws SQLException {
        String sql = """
                INSERT INTO request_items (request_id, product_id, quantity_requested, quantity_approved)
                VALUES (?, ?, ?, NULL)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.executeUpdate();
        }
    }

    private String nextRequestNumber(Connection connection) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(CAST(SUBSTRING(request_number FROM 5) AS INTEGER)), 1000) + 1 AS next_number
                FROM requests
                WHERE request_number LIKE 'REQ-%'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return "REQ-" + resultSet.getInt("next_number");
            }
        }
        throw new SQLException("Unable to generate request number.");
    }

    private RequestContext loadRequestContext(Connection connection, String requestNumber) throws SQLException {
        String sql = """
                SELECT r.request_id,
                       r.request_number,
                       r.request_status,
                       r.from_warehouse_id,
                       r.to_warehouse_id,
                       src.warehouse_code AS source_code,
                       dst.warehouse_code AS destination_code,
                       requester.username AS requested_by_username
                FROM requests r
                JOIN warehouses src ON src.warehouse_id = r.from_warehouse_id
                JOIN warehouses dst ON dst.warehouse_id = r.to_warehouse_id
                JOIN users requester ON requester.user_id = r.requested_by_user_id
                WHERE r.request_number = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new RequestContext(
                            resultSet.getLong("request_id"),
                            resultSet.getString("request_number"),
                            RequestStatus.valueOf(resultSet.getString("request_status")),
                            resultSet.getLong("from_warehouse_id"),
                            resultSet.getLong("to_warehouse_id"),
                            resultSet.getString("source_code"),
                            resultSet.getString("destination_code"),
                            resultSet.getString("requested_by_username")
                    );
                }
            }
        }
        return null;
    }

    private List<RequestItemContext> loadRequestItems(Connection connection, long requestId) throws SQLException {
        String sql = """
                SELECT ri.request_item_id,
                       ri.product_id,
                       ri.quantity_requested,
                       p.model_name
                FROM request_items ri
                JOIN products p ON p.product_id = ri.product_id
                WHERE ri.request_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestItemContext> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(new RequestItemContext(
                            resultSet.getLong("request_item_id"),
                            resultSet.getLong("product_id"),
                            resultSet.getInt("quantity_requested"),
                            resultSet.getString("model_name")
                    ));
                }
                return items;
            }
        }
    }

    private void updateApprovedQuantity(Connection connection, long requestItemId, int quantity) throws SQLException {
        String sql = "UPDATE request_items SET quantity_approved = ? WHERE request_item_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, requestItemId);
            statement.executeUpdate();
        }
    }

    private void updateRequestStatus(Connection connection, long requestId, RequestStatus status, long actingUserId) throws SQLException {
        String sql = """
                UPDATE requests
                SET request_status = ?,
                    approved_by_user_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE request_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, actingUserId);
            statement.setLong(3, requestId);
            statement.executeUpdate();
        }
    }

    private void updatePendingRequestRow(
            Connection connection,
            long requestId,
            long sourceWarehouseId,
            long targetWarehouseId,
            String note
    ) throws SQLException {
        String sql = """
                UPDATE requests
                SET from_warehouse_id = ?,
                    to_warehouse_id = ?,
                    request_note = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE request_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sourceWarehouseId);
            statement.setLong(2, targetWarehouseId);
            statement.setString(3, note == null || note.isBlank() ? null : note.trim());
            statement.setLong(4, requestId);
            statement.executeUpdate();
        }
    }

    private void deleteRequestItems(Connection connection, long requestId) throws SQLException {
        String sql = "DELETE FROM request_items WHERE request_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            statement.executeUpdate();
        }
    }

    private boolean canModifyPendingRequest(UserSession session, RequestContext requestContext) {
        return session.isAdmin()
                || (session.role() == Role.WAREHOUSE_MANAGER
                && requestContext.requestedByUsername().equals(session.username()));
    }

    private void validateRequestedQuantitiesAvailable(Connection connection, long targetWarehouseId, List<RequestDraftLine> lines)
            throws SQLException {
        for (RequestDraftLine line : lines) {
            long productId = findProductId(connection, line.productCode());
            int availableQuantity = findInventoryState(connection, targetWarehouseId, productId).quantity();
            if (line.quantity() > availableQuantity) {
                throw new SQLException("Requested quantity for " + line.productName()
                        + " is higher than available target stock (" + availableQuantity + ").");
            }
        }
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

    private void moveInventory(Connection connection, long warehouseId, long productId, int delta) throws SQLException {
        InventoryState inventoryState = findInventoryState(connection, warehouseId, productId);
        int currentQuantity = inventoryState.quantity();
        int updatedQuantity = currentQuantity + delta;
        if (updatedQuantity < 0) {
            throw new SQLException("Inventory would become negative.");
        }

        if (!inventoryState.exists() && delta > 0) {
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

    private void insertTransferMovement(
            Connection connection,
            long productId,
            long sourceWarehouseId,
            long destinationWarehouseId,
            long requestId,
            long actingUserId,
            int quantity,
            String details,
            String movementType
    ) throws SQLException {
        String sql = """
                INSERT INTO stock_movements (
                    product_id,
                    movement_type,
                    quantity,
                    source_warehouse_id,
                    destination_warehouse_id,
                    request_id,
                    performed_by_user_id,
                    details
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            statement.setString(2, movementType);
            statement.setInt(3, quantity);
            statement.setLong(4, sourceWarehouseId);
            statement.setLong(5, destinationWarehouseId);
            statement.setLong(6, requestId);
            statement.setLong(7, actingUserId);
            statement.setString(8, details);
            statement.executeUpdate();
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

    private record RequestContext(
            long requestId,
            String requestNumber,
            RequestStatus status,
            long sourceWarehouseId,
            long destinationWarehouseId,
            String sourceWarehouseCode,
            String destinationWarehouseCode,
            String requestedByUsername
    ) {
    }

    private record RequestItemContext(
            long requestItemId,
            long productId,
            int quantityRequested,
            String productName
    ) {
    }

    private record InventoryState(boolean exists, int quantity) {
    }
}
