package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.InventoryFilterCriteria;
import com.pcwarehouse.model.InventoryReportRow;
import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.repository.InventoryInsightsRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class InventoryInsightsService {

    private final InventoryInsightsRepository inventoryInsightsRepository = new InventoryInsightsRepository();

    public List<InventorySummary> loadInventory(UserSession session, InventoryFilterCriteria criteria) {
        try (Connection connection = DatabaseConnection.open()) {
            return inventoryInsightsRepository.findFilteredInventory(connection, session, criteria);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<InventoryReportRow> loadInventoryReport(UserSession session, InventoryFilterCriteria criteria) {
        try (Connection connection = DatabaseConnection.open()) {
            return inventoryInsightsRepository.findInventoryReport(connection, session, criteria);
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public List<Warehouse> loadReportWarehouses(UserSession session) {
        try (Connection connection = DatabaseConnection.open()) {
            return inventoryInsightsRepository.findReportWarehouses(connection, session);
        } catch (SQLException exception) {
            return List.of();
        }
    }
}
