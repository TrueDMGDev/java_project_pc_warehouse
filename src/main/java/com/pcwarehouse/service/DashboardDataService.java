package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.DashboardMetric;
import com.pcwarehouse.model.DashboardSnapshot;
import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.MovementLogEntry;
import com.pcwarehouse.model.MovementType;
import com.pcwarehouse.model.RequestStatus;
import com.pcwarehouse.model.RequestSummary;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.WarehouseFilterOption;
import com.pcwarehouse.repository.InventoryRepository;
import com.pcwarehouse.repository.MovementLogRepository;
import com.pcwarehouse.repository.RequestRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDataService {

    private final InventoryRepository inventoryRepository = new InventoryRepository();
    private final RequestRepository requestRepository = new RequestRepository();
    private final MovementLogRepository movementLogRepository = new MovementLogRepository();

    public DashboardSnapshot load(UserSession session, WarehouseFilterOption filterOption) {
        try (Connection connection = DatabaseConnection.open()) {
            List<InventorySummary> inventory = inventoryRepository.findVisibleInventory(connection, session);
            List<RequestSummary> requests = requestRepository.findVisibleRequests(connection, session);
            List<MovementLogEntry> movements = movementLogRepository.findMovementLogs(connection, filterOption);

            return new DashboardSnapshot(
                    true,
                    "",
                    buildMetrics(session, inventory, requests, movements),
                    inventory,
                    requests,
                    movements
            );
        } catch (SQLException exception) {
            List<InventorySummary> inventory = List.of();
            List<RequestSummary> requests = List.of();
            List<MovementLogEntry> movements = List.of();

            return new DashboardSnapshot(
                    false,
                    DatabaseConnection.describeFailure(exception),
                    buildMetrics(session, inventory, requests, movements),
                    inventory,
                    requests,
                    movements
            );
        }
    }

    private List<DashboardMetric> buildMetrics(
            UserSession session,
            List<InventorySummary> inventory,
            List<RequestSummary> requests,
            List<MovementLogEntry> movements
    ) {
        int trackedStock = inventory.stream()
                .mapToInt(InventorySummary::quantity)
                .sum();

        long pendingRequests = requests.stream()
                .filter(request -> request.status() == RequestStatus.PENDING)
                .count();

        long operationsToday = movements.stream()
                .filter(entry -> entry.timestamp().toLocalDate().equals(LocalDate.now()))
                .filter(entry -> entry.type() == MovementType.SUPPLY || entry.type() == MovementType.DISPATCH)
                .count();

        long visibleMovements = movements.size();

        return List.of(
                new DashboardMetric("Tracked Stock", trackedStock + " units",
                        session.isAdmin() ? "Combined inventory across all warehouses" : "Inventory for " + session.scopeLabel()),
                new DashboardMetric("Pending Requests", Long.toString(pendingRequests),
                        session.isAdmin() ? "Open requests across the network" : "Open requests for " + session.scopeLabel()),
                new DashboardMetric("Supply + Dispatch Today", Long.toString(operationsToday),
                        "Direct supply and dispatch entries today"),
                new DashboardMetric("Visible Movements", Long.toString(visibleMovements),
                        "Movement log entries currently shown")
        );
    }
}
