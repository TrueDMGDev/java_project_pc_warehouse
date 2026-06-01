package com.pcwarehouse.repository;

import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.RequestDetailLine;
import com.pcwarehouse.model.RequestDetailRecord;
import com.pcwarehouse.model.RequestStatus;
import com.pcwarehouse.model.RequestSummary;
import com.pcwarehouse.model.RequestViewMode;
import com.pcwarehouse.model.UserSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class RequestRepository {

    public List<InventorySummary> findRequestableInventory(Connection connection, String warehouseCode, String searchTerm)
            throws SQLException {
        String sql = """
                SELECT w.warehouse_code,
                       w.warehouse_name,
                       p.product_code,
                       p.model_name,
                       m.manufacturer_name,
                       c.category_name,
                       i.quantity
                FROM inventory i
                JOIN warehouses w ON w.warehouse_id = i.warehouse_id
                JOIN products p ON p.product_id = i.product_id
                JOIN manufacturers m ON m.manufacturer_id = p.manufacturer_id
                JOIN categories c ON c.category_id = p.category_id
                WHERE w.active = TRUE
                  AND p.active = TRUE
                  AND w.warehouse_code = ?
                  AND i.quantity > 0
                  AND (? = '' OR LOWER(p.product_code) LIKE ? OR LOWER(p.model_name) LIKE ?)
                ORDER BY c.category_name, m.manufacturer_name, p.model_name
                """;

        String normalizedSearch = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
        String wildcard = "%" + normalizedSearch + "%";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, warehouseCode);
            statement.setString(2, normalizedSearch);
            statement.setString(3, wildcard);
            statement.setString(4, wildcard);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<InventorySummary> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new InventorySummary(
                            resultSet.getString("warehouse_code") + " - " + resultSet.getString("warehouse_name"),
                            resultSet.getString("product_code"),
                            resultSet.getString("model_name"),
                            resultSet.getString("manufacturer_name"),
                            resultSet.getString("category_name"),
                            resultSet.getInt("quantity")
                    ));
                }
                return results;
            }
        }
    }

    public List<RequestSummary> findVisibleRequests(Connection connection, UserSession session) throws SQLException {
        return findVisibleRequests(connection, session, RequestViewMode.INCOMING);
    }

    public List<RequestSummary> findVisibleRequests(Connection connection, UserSession session, RequestViewMode viewMode) throws SQLException {
        String sql = """
                SELECT r.request_number,
                       src.warehouse_code AS source_code,
                       src.warehouse_name AS source_name,
                       dst.warehouse_code AS destination_code,
                       dst.warehouse_name AS destination_name,
                       u.username,
                       u.full_name,
                       r.request_status,
                       r.created_at,
                       COUNT(ri.request_item_id) AS line_count,
                       COALESCE(SUM(ri.quantity_requested), 0) AS total_quantity
                FROM requests r
                JOIN warehouses src ON src.warehouse_id = r.from_warehouse_id
                JOIN warehouses dst ON dst.warehouse_id = r.to_warehouse_id
                JOIN users u ON u.user_id = r.requested_by_user_id
                LEFT JOIN request_items ri ON ri.request_id = r.request_id
                WHERE
                """ + buildRequestViewWhereClause(session, viewMode) + "\n" + """
                GROUP BY r.request_number, src.warehouse_code, src.warehouse_name,
                         dst.warehouse_code, dst.warehouse_name, u.username, u.full_name,
                         r.request_status, r.created_at
                ORDER BY r.created_at DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestSummary> results = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    results.add(new RequestSummary(
                            resultSet.getString("request_number"),
                            resultSet.getString("source_code"),
                            resultSet.getString("source_code") + " - " + resultSet.getString("source_name"),
                            resultSet.getString("destination_code"),
                            resultSet.getString("destination_code") + " - " + resultSet.getString("destination_name"),
                            resultSet.getString("username"),
                            resultSet.getString("full_name"),
                            RequestStatus.valueOf(resultSet.getString("request_status")),
                            resultSet.getInt("line_count"),
                            resultSet.getInt("total_quantity"),
                            createdAt.toLocalDateTime()
                    ));
                }
                return results;
            }
        }
    }

    private String buildRequestViewWhereClause(UserSession session, RequestViewMode viewMode) {
        if (session.isAdmin()) {
            String username = escapeSqlLiteral(session.username());
            return switch (viewMode) {
                case INCOMING -> "r.request_status = 'PENDING'";
                case OUTGOING -> "u.username = '" + username + "' AND r.request_status = 'PENDING'";
                case HISTORY -> "r.request_status <> 'PENDING'";
            };
        }

        String warehouseCode = escapeSqlLiteral(session.warehouse().code());
        String username = escapeSqlLiteral(session.username());
        return switch (viewMode) {
            case INCOMING -> "dst.warehouse_code = '" + warehouseCode + "' AND r.request_status = 'PENDING'";
            case OUTGOING -> "u.username = '" + username + "' AND r.request_status = 'PENDING'";
            case HISTORY -> "(dst.warehouse_code = '" + warehouseCode
                    + "' OR src.warehouse_code = '" + warehouseCode
                    + "' OR u.username = '" + username
                    + "') AND r.request_status <> 'PENDING'";
        };
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    public RequestDetailRecord findRequestDetail(Connection connection, UserSession session, String requestNumber) throws SQLException {
        String sql = """
                SELECT r.request_number,
                       r.request_status,
                       r.request_note,
                       r.created_at,
                       r.updated_at,
                       src.warehouse_code AS source_code,
                       src.warehouse_name AS source_name,
                       dst.warehouse_code AS destination_code,
                       dst.warehouse_name AS destination_name,
                       requester.username AS requested_by_username,
                       requester.full_name AS requested_by,
                       approver.full_name AS approved_by
                FROM requests r
                JOIN warehouses src ON src.warehouse_id = r.from_warehouse_id
                JOIN warehouses dst ON dst.warehouse_id = r.to_warehouse_id
                JOIN users requester ON requester.user_id = r.requested_by_user_id
                LEFT JOIN users approver ON approver.user_id = r.approved_by_user_id
                WHERE r.request_number = ?
                  AND (? = TRUE
                       OR dst.warehouse_code = ?
                       OR src.warehouse_code = ?
                       OR requester.username = ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestNumber);
            statement.setBoolean(2, session.isAdmin());
            statement.setString(3, session.isAdmin() ? "" : session.warehouse().code());
            statement.setString(4, session.isAdmin() ? "" : session.warehouse().code());
            statement.setString(5, session.username());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new RequestDetailRecord(
                        resultSet.getString("request_number"),
                        resultSet.getString("source_code"),
                        resultSet.getString("source_code") + " - " + resultSet.getString("source_name"),
                        resultSet.getString("destination_code"),
                        resultSet.getString("destination_code") + " - " + resultSet.getString("destination_name"),
                        resultSet.getString("requested_by_username"),
                        resultSet.getString("requested_by"),
                        resultSet.getString("approved_by"),
                        RequestStatus.valueOf(resultSet.getString("request_status")),
                        resultSet.getString("request_note"),
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at").toLocalDateTime(),
                        findRequestLines(connection, requestNumber)
                );
            }
        }
    }

    private List<RequestDetailLine> findRequestLines(Connection connection, String requestNumber) throws SQLException {
        String sql = """
                SELECT p.product_code,
                       p.model_name,
                       m.manufacturer_name,
                       c.category_name,
                       ri.quantity_requested,
                       ri.quantity_approved,
                       COALESCE(i.quantity, 0) AS available_quantity
                FROM request_items ri
                JOIN requests r ON r.request_id = ri.request_id
                JOIN products p ON p.product_id = ri.product_id
                JOIN manufacturers m ON m.manufacturer_id = p.manufacturer_id
                JOIN categories c ON c.category_id = p.category_id
                LEFT JOIN inventory i ON i.warehouse_id = r.to_warehouse_id
                                    AND i.product_id = p.product_id
                WHERE r.request_number = ?
                ORDER BY c.category_name, m.manufacturer_name, p.model_name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestDetailLine> lines = new ArrayList<>();
                while (resultSet.next()) {
                    int approvedQuantity = resultSet.getInt("quantity_approved");
                    lines.add(new RequestDetailLine(
                            resultSet.getString("product_code"),
                            resultSet.getString("model_name"),
                            resultSet.getString("manufacturer_name"),
                            resultSet.getString("category_name"),
                            resultSet.getInt("quantity_requested"),
                            resultSet.wasNull() ? null : approvedQuantity,
                            resultSet.getInt("available_quantity")
                    ));
                }
                return lines;
            }
        }
    }
}
