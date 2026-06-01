package com.pcwarehouse.repository;

import com.pcwarehouse.model.MovementLogEntry;
import com.pcwarehouse.model.MovementType;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.model.WarehouseFilterOption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class MovementLogRepository {

    public List<MovementLogEntry> findMovementLogs(Connection connection, WarehouseFilterOption filterOption) throws SQLException {
        String sql = """
                SELECT sm.movement_timestamp,
                       sm.movement_type,
                       p.model_name,
                       m.manufacturer_name,
                       sm.quantity,
                       sw.warehouse_code AS source_code,
                       sw.warehouse_name AS source_name,
                       sw.city AS source_city,
                       dw.warehouse_code AS destination_code,
                       dw.warehouse_name AS destination_name,
                       dw.city AS destination_city,
                       sm.external_source,
                       sm.external_destination,
                       u.username,
                       sm.details
                FROM stock_movements sm
                JOIN products p ON p.product_id = sm.product_id
                JOIN manufacturers m ON m.manufacturer_id = p.manufacturer_id
                LEFT JOIN warehouses sw ON sw.warehouse_id = sm.source_warehouse_id
                LEFT JOIN warehouses dw ON dw.warehouse_id = sm.destination_warehouse_id
                JOIN users u ON u.user_id = sm.performed_by_user_id
                ORDER BY sm.movement_timestamp DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<MovementLogEntry> results = new ArrayList<>();
            while (resultSet.next()) {
                MovementLogEntry entry = new MovementLogEntry(
                        getTimestamp(resultSet, "movement_timestamp").toLocalDateTime(),
                        MovementType.valueOf(resultSet.getString("movement_type")),
                        resultSet.getString("model_name"),
                        resultSet.getString("manufacturer_name"),
                        resultSet.getInt("quantity"),
                        toWarehouse(resultSet, "source_code", "source_name", "source_city"),
                        toWarehouse(resultSet, "destination_code", "destination_name", "destination_city"),
                        resultSet.getString("external_source"),
                        resultSet.getString("external_destination"),
                        resultSet.getString("username"),
                        resultSet.getString("details")
                );

                if (filterOption.matches(entry)) {
                    results.add(entry);
                }
            }
            return results;
        }
    }

    private Timestamp getTimestamp(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getTimestamp(columnName);
    }

    private Warehouse toWarehouse(ResultSet resultSet, String codeColumn, String nameColumn, String cityColumn) throws SQLException {
        String code = resultSet.getString(codeColumn);
        if (code == null) {
            return null;
        }
        return new Warehouse(code, resultSet.getString(nameColumn), resultSet.getString(cityColumn));
    }
}
