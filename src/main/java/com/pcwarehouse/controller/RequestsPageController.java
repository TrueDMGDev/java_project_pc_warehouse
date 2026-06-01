package com.pcwarehouse.controller;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.InventoryFilterCriteria;
import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.MovementLogEntry;
import com.pcwarehouse.model.MovementType;
import com.pcwarehouse.model.RequestActionResult;
import com.pcwarehouse.model.RequestDetailLine;
import com.pcwarehouse.model.RequestDetailRecord;
import com.pcwarehouse.model.RequestDraftLine;
import com.pcwarehouse.model.RequestStatus;
import com.pcwarehouse.model.RequestSummary;
import com.pcwarehouse.model.RequestViewMode;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.model.WarehouseFilterOption;
import com.pcwarehouse.service.InventoryInsightsService;
import com.pcwarehouse.service.ProductManagementService;
import com.pcwarehouse.service.RequestActionService;
import com.pcwarehouse.service.RequestDetailService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class RequestsPageController {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FILTER_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String ALL_MOVEMENT_TYPES_LABEL = "All types";

    private final View view;
    private final Supplier<UserSession> sessionSupplier;
    private final Runnable refreshAll;
    private final RequestActionService requestActionService = new RequestActionService();
    private final RequestDetailService requestDetailService = new RequestDetailService();
    private final InventoryInsightsService inventoryInsightsService = new InventoryInsightsService();
    private final ProductManagementService productManagementService = new ProductManagementService();
    private final ObservableList<RequestDraftLine> requestDraftLines = FXCollections.observableArrayList();

    private List<Warehouse> allRequestWarehouses = List.of();
    private List<MovementLogEntry> currentMovementLogEntries = List.of();
    private WarehouseFilterOption currentMovementWarehouseFilter;
    private RequestDetailRecord currentRequestDetail;
    private RequestViewMode activeRequestViewMode = RequestViewMode.INCOMING;
    private String editingRequestNumber;
    private boolean suppressInventoryFilterRefresh;

    RequestsPageController(View view, Supplier<UserSession> sessionSupplier, Runnable refreshAll) {
        this.view = view;
        this.sessionSupplier = sessionSupplier;
        this.refreshAll = refreshAll;
    }

    void initialize() {
        configureSelectors();
        configureInventoryTable();
        configureRequestInventoryTable();
        configureRequestDraftTable();
        configureRequestTable();
        configureRequestDetailTable();
        configureMovementLogTable();
    }

    void handleInventorySearch() {
        refreshInventoryOnly();
    }

    void handleClearInventoryFilters() {
        view.inventorySearchField.clear();
        if (!view.inventoryCategoryFilterSelector.getItems().isEmpty()) {
            view.inventoryCategoryFilterSelector.getSelectionModel().selectFirst();
        }
        if (!view.inventoryManufacturerFilterSelector.getItems().isEmpty()) {
            view.inventoryManufacturerFilterSelector.getSelectionModel().selectFirst();
        }
        refreshInventoryOnly();
    }

    void handleApplyMovementFilters() {
        refreshMovementLogView();
    }

    void handleClearMovementFilters() {
        if (!view.movementTypeFilterSelector.getItems().isEmpty()) {
            view.movementTypeFilterSelector.getSelectionModel().selectFirst();
        }
        view.movementFromDatePicker.setValue(null);
        view.movementToDatePicker.setValue(null);
        refreshMovementLogView();
    }

    void handleAddRequestLine() {
        InventorySummary selectedInventory = view.requestInventoryTable.getSelectionModel().getSelectedItem();
        int quantity = parseQuantity(view.requestQuantityField.getText());
        if (selectedInventory == null) {
            view.requestActionStatusLabel.setText("Select an item from the target warehouse inventory before adding a request line.");
            return;
        }
        if (quantity <= 0) {
            view.requestActionStatusLabel.setText("Enter a positive quantity before adding a request line.");
            return;
        }
        int remainingQuantity = getRemainingAvailableQuantity(selectedInventory);
        if (quantity > remainingQuantity) {
            view.requestActionStatusLabel.setText("Only " + remainingQuantity + " additional unit(s) are available for "
                    + selectedInventory.productName() + " in the target warehouse.");
            return;
        }

        int existingIndex = findDraftLineIndex(selectedInventory.productCode());
        if (existingIndex >= 0) {
            RequestDraftLine existingLine = requestDraftLines.get(existingIndex);
            int updatedQuantity = existingLine.quantity() + quantity;
            requestDraftLines.set(existingIndex, new RequestDraftLine(
                    existingLine.productCode(),
                    existingLine.productName(),
                    existingLine.manufacturer(),
                    existingLine.category(),
                    updatedQuantity
            ));
        } else {
            requestDraftLines.add(new RequestDraftLine(
                    selectedInventory.productCode(),
                    selectedInventory.productName(),
                    selectedInventory.manufacturer(),
                    selectedInventory.category(),
                    quantity
            ));
        }

        view.requestQuantityField.clear();
        view.requestQuantityField.setPromptText("Max " + getRemainingAvailableQuantity(selectedInventory));
        view.requestInventoryTable.refresh();
        updateRequestDraftButtons();
        view.requestActionStatusLabel.setText("Added " + selectedInventory.productName() + " to the request draft.");
    }

    void handleRemoveRequestLine() {
        RequestDraftLine selectedLine = view.requestDraftTable.getSelectionModel().getSelectedItem();
        if (selectedLine == null) {
            view.requestActionStatusLabel.setText("Select a request draft line to remove.");
            return;
        }

        requestDraftLines.remove(selectedLine);
        view.requestInventoryTable.refresh();
        updateRequestDraftButtons();
        view.requestActionStatusLabel.setText("Removed " + selectedLine.productName() + " from the request draft.");
    }

    void handleClearRequestDraft() {
        clearRequestDraftState();
        view.requestInventoryTable.refresh();
        view.requestActionStatusLabel.setText("Request draft cleared.");
    }

    void handleShowIncomingRequests() {
        setActiveRequestView(RequestViewMode.INCOMING);
    }

    void handleShowOutgoingRequests() {
        setActiveRequestView(RequestViewMode.OUTGOING);
    }

    void handleShowRequestHistory() {
        setActiveRequestView(RequestViewMode.HISTORY);
    }

    void handleCreateRequest() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }
        if (editingRequestNumber != null) {
            view.requestActionStatusLabel.setText("Finish or clear the current edit before creating a new request.");
            return;
        }

        Warehouse sourceWarehouse = view.requestSourceWarehouseSelector.getSelectionModel().getSelectedItem();
        Warehouse targetWarehouse = view.requestTargetWarehouseSelector.getSelectionModel().getSelectedItem();
        if (requestDraftLines.isEmpty()) {
            view.requestActionStatusLabel.setText("Add at least one line to the request draft before submitting.");
            return;
        }

        RequestActionResult result = requestActionService.createRequest(
                session,
                sourceWarehouse,
                targetWarehouse,
                new ArrayList<>(requestDraftLines),
                view.requestNoteField.getText()
        );

        if (result.success()) {
            clearRequestDraftState();
            refreshAll.run();
            view.requestActionStatusLabel.setText(result.message());
            return;
        }

        view.requestActionStatusLabel.setText(result.message());
    }

    void handleApproveRequest() {
        executeRequestAction(true);
    }

    void handleRejectRequest() {
        executeRequestAction(false);
    }

    void handleEditSelectedRequest() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        RequestSummary selectedRequest = view.requestTable.getSelectionModel().getSelectedItem();
        if (!canEditSelectedPendingRequest(selectedRequest)) {
            view.requestActionStatusLabel.setText("Only pending requests created by this manager or admin can be edited.");
            return;
        }

        RequestDetailRecord detail = requestDetailService.load(session, selectedRequest.requestNumber());
        if (detail == null) {
            view.requestActionStatusLabel.setText("Could not load the selected request details.");
            return;
        }

        editingRequestNumber = detail.requestNumber();
        selectWarehouseByCode(view.requestSourceWarehouseSelector, detail.sourceWarehouseCode());
        refreshRequestTargetOptions();
        selectWarehouseByCode(view.requestTargetWarehouseSelector, detail.destinationWarehouseCode());
        view.requestNoteField.setText(detail.note() == null ? "" : detail.note());
        view.requestQuantityField.clear();
        requestDraftLines.setAll(detail.lines().stream()
                .map(line -> new RequestDraftLine(
                        line.productCode(),
                        line.productName(),
                        line.manufacturer(),
                        line.category(),
                        line.quantityRequested()
                ))
                .toList());
        view.requestInventoryTable.refresh();
        updateRequestActionButtons(DatabaseConnection.canConnect());
        view.requestActionStatusLabel.setText("Editing " + detail.requestNumber() + ". Save pending edits when ready.");
    }

    void handleSaveRequestEdits() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }
        if (editingRequestNumber == null) {
            view.requestActionStatusLabel.setText("Load a pending outgoing request before saving edits.");
            return;
        }

        RequestActionResult result = requestActionService.updatePendingRequest(
                session,
                editingRequestNumber,
                view.requestSourceWarehouseSelector.getSelectionModel().getSelectedItem(),
                view.requestTargetWarehouseSelector.getSelectionModel().getSelectedItem(),
                new ArrayList<>(requestDraftLines),
                view.requestNoteField.getText()
        );

        if (result.success()) {
            clearRequestDraftState();
            refreshAll.run();
            view.requestActionStatusLabel.setText(result.message());
            return;
        }

        view.requestActionStatusLabel.setText(result.message());
    }

    void handleCancelSelectedRequest() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        RequestSummary selectedRequest = view.requestTable.getSelectionModel().getSelectedItem();
        RequestActionResult result = requestActionService.cancelRequest(session, selectedRequest);

        if (result.success()) {
            if (editingRequestNumber != null && selectedRequest != null
                    && editingRequestNumber.equals(selectedRequest.requestNumber())) {
                clearRequestDraftState();
            }
            refreshAll.run();
            view.requestActionStatusLabel.setText(result.message());
            return;
        }

        view.requestActionStatusLabel.setText(result.message());
    }

    void updateControls(UserSession session, boolean usingLiveDatabase) {
        Warehouse selectedSource = view.requestSourceWarehouseSelector.getSelectionModel().getSelectedItem();
        Warehouse selectedTarget = view.requestTargetWarehouseSelector.getSelectionModel().getSelectedItem();

        List<Warehouse> sourceWarehouses = requestActionService.loadSourceWarehouses(session);
        allRequestWarehouses = requestActionService.loadAllWarehouses();

        view.requestSourceWarehouseSelector.setItems(FXCollections.observableArrayList(sourceWarehouses));

        if (selectedSource != null && sourceWarehouses.stream().anyMatch(warehouse -> warehouse.code().equals(selectedSource.code()))) {
            view.requestSourceWarehouseSelector.getSelectionModel().select(
                    sourceWarehouses.stream().filter(warehouse -> warehouse.code().equals(selectedSource.code())).findFirst().orElse(null)
            );
        } else if (!sourceWarehouses.isEmpty()) {
            view.requestSourceWarehouseSelector.getSelectionModel().selectFirst();
        }

        refreshRequestTargetOptions();
        if (selectedTarget != null) {
            view.requestTargetWarehouseSelector.getItems().stream()
                    .filter(warehouse -> warehouse.code().equals(selectedTarget.code()))
                    .findFirst()
                    .ifPresent(warehouse -> view.requestTargetWarehouseSelector.getSelectionModel().select(warehouse));
        }

        refreshRequestTargetInventory();

        boolean canCreateRequests = session.role().canCreateRequests() && usingLiveDatabase;
        boolean canApproveRequests = session.role().canApproveRequests() && usingLiveDatabase;

        view.requestSourceWarehouseSelector.setDisable(!usingLiveDatabase || !session.isAdmin());
        view.requestTargetWarehouseSelector.setDisable(!canCreateRequests);
        view.requestInventorySearchField.setDisable(!canCreateRequests);
        view.requestInventoryTable.setDisable(!canCreateRequests);
        view.requestQuantityField.setDisable(!canCreateRequests);
        view.requestNoteField.setDisable(!canCreateRequests);
        view.addRequestLineButton.setDisable(!canCreateRequests
                || view.requestInventoryTable.getSelectionModel().getSelectedItem() == null
                || getRemainingAvailableQuantity(view.requestInventoryTable.getSelectionModel().getSelectedItem()) <= 0);
        view.removeRequestLineButton.setDisable(!canCreateRequests || requestDraftLines.isEmpty());
        view.clearRequestDraftButton.setDisable(!canCreateRequests || requestDraftLines.isEmpty());
        updateRequestActionButtons(usingLiveDatabase);
        updateRequestViewButtonStates();

        if (!usingLiveDatabase) {
            view.requestActionStatusLabel.setText("Request actions need a live database connection.");
        } else if (!canCreateRequests && !canApproveRequests) {
            view.requestActionStatusLabel.setText("This session is read-only for warehouse requests.");
        } else if (view.requestActionStatusLabel.getText() == null || view.requestActionStatusLabel.getText().isBlank()) {
            view.requestActionStatusLabel.setText(canApproveRequests
                    ? "Create requests or approve/reject the selected pending request."
                    : "Create requests for another warehouse from this form.");
        }
    }

    void refreshInventoryOnly() {
        if (sessionSupplier.get() == null) {
            return;
        }
        updateInventoryFilters();
        updateInventoryView();
    }

    void refreshRequestsOnly() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        updateRequests(requestActionService.loadRequests(session, activeRequestViewMode));
        refreshSelectedRequestDetail();
    }

    void updateMovementLogs(List<MovementLogEntry> entries, WarehouseFilterOption filterOption) {
        currentMovementLogEntries = entries;
        currentMovementWarehouseFilter = filterOption;
        refreshMovementLogView();
    }

    private void configureSelectors() {
        view.inventoryCategoryFilterSelector.setOnAction(event -> {
            if (!suppressInventoryFilterRefresh) {
                refreshInventoryOnly();
            }
        });
        view.inventoryManufacturerFilterSelector.setOnAction(event -> {
            if (!suppressInventoryFilterRefresh) {
                refreshInventoryOnly();
            }
        });

        List<String> movementTypes = new ArrayList<>();
        movementTypes.add(ALL_MOVEMENT_TYPES_LABEL);
        for (MovementType movementType : MovementType.values()) {
            movementTypes.add(movementType.displayName());
        }
        view.movementTypeFilterSelector.setItems(FXCollections.observableArrayList(movementTypes));
        view.movementTypeFilterSelector.getSelectionModel().selectFirst();
        view.movementTypeFilterSelector.setOnAction(event -> refreshMovementLogView());
        configureDatePicker(view.movementFromDatePicker);
        configureDatePicker(view.movementToDatePicker);
        view.movementFromDatePicker.setOnAction(event -> refreshMovementLogView());
        view.movementToDatePicker.setOnAction(event -> refreshMovementLogView());

        view.requestSourceWarehouseSelector.setOnAction(event -> {
            refreshRequestTargetOptions();
            refreshRequestTargetInventory();
        });
        view.requestTargetWarehouseSelector.setOnAction(event -> refreshRequestTargetInventory());
        view.requestInventorySearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshRequestTargetInventory());
    }

    private void configureDatePicker(DatePicker datePicker) {
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_FILTER_FORMATTER.format(date);
            }

            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(value.trim(), DATE_FILTER_FORMATTER);
                } catch (DateTimeParseException exception) {
                    return null;
                }
            }
        });
    }

    private void configureInventoryTable() {
        view.inventoryWarehouseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseLabel()));
        view.inventoryCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.inventoryProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        view.inventoryManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        view.inventoryCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        view.inventoryQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        view.inventoryTable.setPlaceholder(new Label("No inventory rows are available for this session."));
    }

    private void configureRequestInventoryTable() {
        view.requestInventoryCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.requestInventoryProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        view.requestInventoryManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        view.requestInventoryCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        view.requestInventoryQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        view.requestInventoryQuantityColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                InventorySummary inventoryItem = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || inventoryItem == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label availableLabel = new Label(Integer.toString(inventoryItem.quantity()));
                HBox quantityBox = new HBox(8.0, availableLabel);
                int draftedQuantity = getDraftQuantity(inventoryItem.productCode());
                if (draftedQuantity > 0) {
                    Label deductionLabel = new Label("-" + draftedQuantity);
                    deductionLabel.getStyleClass().add("inventory-deduction");
                    quantityBox.getChildren().add(deductionLabel);
                }
                setText(null);
                setGraphic(quantityBox);
            }
        });
        view.requestInventoryTable.setPlaceholder(new Label("Select a target warehouse, then search by code or product name."));
        view.requestInventoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedItem) -> {
            view.requestQuantityField.setPromptText(selectedItem == null ? "Qty" : "Max " + getRemainingAvailableQuantity(selectedItem));
            updateRequestDraftButtons();
        });
    }

    private void configureRequestDraftTable() {
        view.requestDraftCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.requestDraftProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        view.requestDraftQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        view.requestDraftTable.setItems(requestDraftLines);
        view.requestDraftTable.setPlaceholder(new Label("Add products above to build a multi-line warehouse request."));
    }

    private void configureRequestTable() {
        view.requestNumberColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().requestNumber()));
        view.requestSourceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().sourceWarehouseLabel()));
        view.requestDestinationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().destinationWarehouseLabel()));
        view.requestRequestedByColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().requestedBy()));
        view.requestStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().status().displayName()));
        view.requestLineCountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().lineCount())));
        view.requestTotalQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().totalQuantity())));
        view.requestCreatedAtColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().createdAt().format(TIMESTAMP_FORMATTER)));
        view.requestTable.setPlaceholder(new Label("No warehouse requests are available for this view."));
        view.requestTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            refreshSelectedRequestDetail();
            updateRequestActionButtons(DatabaseConnection.canConnect());
        });
    }

    private void configureRequestDetailTable() {
        view.requestDetailCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.requestDetailProductColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().productName() + " | " + cellData.getValue().manufacturer() + " | " + cellData.getValue().category()));
        view.requestDetailRequestedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantityRequested())));
        view.requestDetailApprovedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().quantityApproved() == null ? "-" : Integer.toString(cellData.getValue().quantityApproved())));
        view.requestDetailLineTable.setPlaceholder(new Label("Select a request to see its requested items."));
        view.requestDetailLineTable.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(RequestDetailLine line, boolean empty) {
                super.updateItem(line, empty);
                getStyleClass().remove("request-detail-insufficient");
                if (!empty && line != null && shouldHighlightInsufficientRequestLine(line)) {
                    getStyleClass().add("request-detail-insufficient");
                }
            }
        });
    }

    private void configureMovementLogTable() {
        view.timestampColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().timestamp().format(TIMESTAMP_FORMATTER)));
        view.movementTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().type().displayName()));
        view.productColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().product()));
        view.manufacturerColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().manufacturer()));
        view.quantityColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        view.sourceWarehouseColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().sourceWarehouseLabel()));
        view.destinationWarehouseColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().destinationWarehouseLabel()));
        view.userColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().performedBy()));
        view.detailsColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().details()));
        view.movementLogTable.setPlaceholder(new Label("No movement logs match the selected filter."));
    }

    private void updateRequestDraftButtons() {
        UserSession session = sessionSupplier.get();
        boolean canCreateRequests = session != null && session.role().canCreateRequests() && DatabaseConnection.canConnect();
        InventorySummary selectedInventory = view.requestInventoryTable.getSelectionModel().getSelectedItem();
        view.addRequestLineButton.setDisable(!canCreateRequests || selectedInventory == null || getRemainingAvailableQuantity(selectedInventory) <= 0);
        view.removeRequestLineButton.setDisable(!canCreateRequests || requestDraftLines.isEmpty());
        view.clearRequestDraftButton.setDisable(!canCreateRequests || (requestDraftLines.isEmpty() && editingRequestNumber == null));
        updateRequestActionButtons(DatabaseConnection.canConnect());
    }

    private void updateInventoryFilters() {
        String selectedCategory = view.inventoryCategoryFilterSelector.getSelectionModel().getSelectedItem();
        String selectedManufacturer = view.inventoryManufacturerFilterSelector.getSelectionModel().getSelectedItem();

        List<String> categories = new ArrayList<>();
        categories.add("All categories");
        categories.addAll(productManagementService.loadCategories());

        List<String> manufacturers = new ArrayList<>();
        manufacturers.add("All manufacturers");
        manufacturers.addAll(productManagementService.loadManufacturers());

        suppressInventoryFilterRefresh = true;
        try {
            view.inventoryCategoryFilterSelector.setItems(FXCollections.observableArrayList(categories));
            view.inventoryManufacturerFilterSelector.setItems(FXCollections.observableArrayList(manufacturers));
            selectOrFirst(view.inventoryCategoryFilterSelector, selectedCategory);
            selectOrFirst(view.inventoryManufacturerFilterSelector, selectedManufacturer);
        } finally {
            suppressInventoryFilterRefresh = false;
        }
    }

    private void updateInventoryView() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        InventoryFilterCriteria criteria = new InventoryFilterCriteria(
                view.inventorySearchField == null ? "" : view.inventorySearchField.getText(),
                normalizeAllSelection(view.inventoryCategoryFilterSelector),
                normalizeAllSelection(view.inventoryManufacturerFilterSelector),
                null
        );
        updateInventory(inventoryInsightsService.loadInventory(session, criteria));
    }

    private void refreshRequestTargetOptions() {
        Warehouse selectedSource = view.requestSourceWarehouseSelector.getSelectionModel().getSelectedItem();
        Warehouse currentTarget = view.requestTargetWarehouseSelector.getSelectionModel().getSelectedItem();

        List<Warehouse> targets = allRequestWarehouses.stream()
                .filter(warehouse -> selectedSource == null || !warehouse.code().equals(selectedSource.code()))
                .toList();

        view.requestTargetWarehouseSelector.setItems(FXCollections.observableArrayList(targets));

        if (currentTarget != null && targets.stream().anyMatch(warehouse -> warehouse.code().equals(currentTarget.code()))) {
            view.requestTargetWarehouseSelector.getSelectionModel().select(
                    targets.stream().filter(warehouse -> warehouse.code().equals(currentTarget.code())).findFirst().orElse(null)
            );
        } else if (!targets.isEmpty()) {
            view.requestTargetWarehouseSelector.getSelectionModel().selectFirst();
        }

        refreshRequestTargetInventory();
    }

    private void refreshRequestTargetInventory() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            view.requestInventoryTable.getItems().clear();
            view.requestInventorySummaryLabel.setText("Sign in to view request inventory.");
            updateRequestDraftButtons();
            return;
        }

        Warehouse selectedTarget = view.requestTargetWarehouseSelector.getSelectionModel().getSelectedItem();
        if (selectedTarget == null) {
            view.requestInventoryTable.getItems().clear();
            view.requestInventorySummaryLabel.setText("Select a target warehouse.");
            updateRequestDraftButtons();
            return;
        }

        InventorySummary currentSelection = view.requestInventoryTable.getSelectionModel().getSelectedItem();
        String currentProductCode = currentSelection == null ? null : currentSelection.productCode();
        List<InventorySummary> targetInventory = requestActionService.loadTargetInventory(
                selectedTarget,
                view.requestInventorySearchField.getText()
        );

        view.requestInventoryTable.getItems().setAll(targetInventory);
        if (currentProductCode != null) {
            targetInventory.stream()
                    .filter(item -> item.productCode().equals(currentProductCode))
                    .findFirst()
                    .ifPresent(item -> view.requestInventoryTable.getSelectionModel().select(item));
        }

        String searchTerm = view.requestInventorySearchField.getText() == null ? "" : view.requestInventorySearchField.getText().trim();
        view.requestInventorySummaryLabel.setText(targetInventory.size() + " available item(s) in " + selectedTarget.label()
                + (searchTerm.isBlank() ? "" : " matching \"" + searchTerm + "\""));
        updateRequestDraftButtons();
    }

    private void updateInventory(List<InventorySummary> inventoryItems) {
        view.inventoryTable.getItems().setAll(inventoryItems);
        view.inventorySummaryLabel.setText(inventoryItems.size() + " visible stock rows");
    }

    private void updateRequests(List<RequestSummary> requestSummaries) {
        RequestSummary currentSelection = view.requestTable.getSelectionModel().getSelectedItem();
        String currentRequestNumber = currentSelection == null ? null : currentSelection.requestNumber();

        view.requestTable.getItems().setAll(requestSummaries);
        if (currentRequestNumber != null) {
            requestSummaries.stream()
                    .filter(summary -> summary.requestNumber().equals(currentRequestNumber))
                    .findFirst()
                    .ifPresent(summary -> view.requestTable.getSelectionModel().select(summary));
        }

        long pendingCount = requestSummaries.stream().filter(summary -> summary.status() == RequestStatus.PENDING).count();
        view.requestSummaryLabel.setText(requestSummaries.size() + " " + activeRequestViewMode.summaryLabel()
                + " | " + pendingCount + " pending");
        updateRequestActionButtons(DatabaseConnection.canConnect());
    }

    private void setActiveRequestView(RequestViewMode viewMode) {
        activeRequestViewMode = viewMode;
        updateRequestViewButtonStates();
        refreshRequestsOnly();
        view.requestActionStatusLabel.setText("Showing " + viewMode.summaryLabel() + ".");
    }

    private void updateRequestViewButtonStates() {
        applyRequestViewButtonState(view.requestIncomingViewButton, activeRequestViewMode == RequestViewMode.INCOMING);
        applyRequestViewButtonState(view.requestOutgoingViewButton, activeRequestViewMode == RequestViewMode.OUTGOING);
        applyRequestViewButtonState(view.requestHistoryViewButton, activeRequestViewMode == RequestViewMode.HISTORY);
    }

    private boolean shouldHighlightInsufficientRequestLine(RequestDetailLine line) {
        return currentRequestDetail != null
                && activeRequestViewMode == RequestViewMode.INCOMING
                && currentRequestDetail.status() == RequestStatus.PENDING
                && line.hasInsufficientAvailableQuantity();
    }

    private void applyRequestViewButtonState(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().remove("request-view-button-active");
        if (active) {
            button.getStyleClass().add("request-view-button-active");
        }
    }

    private void updateRequestActionButtons(boolean usingLiveDatabase) {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        RequestSummary selectedRequest = view.requestTable.getSelectionModel().getSelectedItem();
        boolean canCreateRequests = session.role().canCreateRequests() && usingLiveDatabase;
        boolean canApproveSelected = canApproveSelectedPendingRequest(selectedRequest) && usingLiveDatabase;
        boolean canEditSelected = canEditSelectedPendingRequest(selectedRequest) && usingLiveDatabase;
        boolean editing = editingRequestNumber != null;

        if (view.requestEditorTitleLabel != null) {
            view.requestEditorTitleLabel.setText(editing
                    ? "Editing " + editingRequestNumber
                    : "Create / Edit Request");
        }

        view.requestSourceWarehouseSelector.setDisable(!usingLiveDatabase || !session.isAdmin());
        view.requestTargetWarehouseSelector.setDisable(!canCreateRequests);
        view.requestInventorySearchField.setDisable(!canCreateRequests);
        view.requestInventoryTable.setDisable(!canCreateRequests);
        view.requestQuantityField.setDisable(!canCreateRequests);
        view.requestNoteField.setDisable(!canCreateRequests);
        view.addRequestLineButton.setDisable(!canCreateRequests
                || view.requestInventoryTable.getSelectionModel().getSelectedItem() == null
                || getRemainingAvailableQuantity(view.requestInventoryTable.getSelectionModel().getSelectedItem()) <= 0);
        view.removeRequestLineButton.setDisable(!canCreateRequests || requestDraftLines.isEmpty());
        view.clearRequestDraftButton.setDisable(!canCreateRequests || (requestDraftLines.isEmpty() && !editing));
        view.createRequestButton.setDisable(!canCreateRequests || editing);
        view.saveRequestEditsButton.setDisable(!canCreateRequests || !editing);
        view.approveRequestButton.setDisable(!canApproveSelected);
        view.rejectRequestButton.setDisable(!canApproveSelected);
        view.editSelectedRequestButton.setDisable(!canEditSelected);
        view.cancelSelectedRequestButton.setDisable(!canEditSelected);
    }

    private boolean canApproveSelectedPendingRequest(RequestSummary requestSummary) {
        UserSession session = sessionSupplier.get();
        if (requestSummary == null || requestSummary.status() != RequestStatus.PENDING || session == null) {
            return false;
        }
        return session.isAdmin()
                || (session.role().canApproveRequests()
                && requestSummary.destinationWarehouseCode().equals(session.warehouse().code()));
    }

    private boolean canEditSelectedPendingRequest(RequestSummary requestSummary) {
        UserSession session = sessionSupplier.get();
        if (requestSummary == null || requestSummary.status() != RequestStatus.PENDING || session == null) {
            return false;
        }
        return session.isAdmin()
                || (session.role() == Role.WAREHOUSE_MANAGER
                && requestSummary.requestedByUsername().equals(session.username()));
    }

    private void refreshMovementLogView() {
        if (view.movementLogTable == null || currentMovementLogEntries == null) {
            return;
        }

        String selectedType = view.movementTypeFilterSelector == null ? ALL_MOVEMENT_TYPES_LABEL
                : view.movementTypeFilterSelector.getSelectionModel().getSelectedItem();
        LocalDate fromDate = view.movementFromDatePicker == null ? null : view.movementFromDatePicker.getValue();
        LocalDate toDate = view.movementToDatePicker == null ? null : view.movementToDatePicker.getValue();

        List<MovementLogEntry> filteredEntries = currentMovementLogEntries.stream()
                .filter(entry -> selectedType == null
                        || ALL_MOVEMENT_TYPES_LABEL.equals(selectedType)
                        || entry.type().displayName().equals(selectedType))
                .filter(entry -> fromDate == null || !entry.timestamp().toLocalDate().isBefore(fromDate))
                .filter(entry -> toDate == null || !entry.timestamp().toLocalDate().isAfter(toDate))
                .toList();

        view.movementLogTable.getItems().setAll(filteredEntries);
        String warehouseLabel = currentMovementWarehouseFilter == null ? "All warehouses" : currentMovementWarehouseFilter.label();
        String typeLabel = selectedType == null ? ALL_MOVEMENT_TYPES_LABEL : selectedType;
        String dateLabel = (fromDate == null && toDate == null)
                ? "All dates"
                : (fromDate == null ? "Any start" : DATE_FILTER_FORMATTER.format(fromDate))
                + " to "
                + (toDate == null ? "Any end" : DATE_FILTER_FORMATTER.format(toDate));
        view.movementLogSummaryLabel.setText(filteredEntries.size() + " movement records shown | Warehouse: "
                + warehouseLabel + " | Type: " + typeLabel + " | Dates: " + dateLabel);
    }

    private void executeRequestAction(boolean approve) {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        RequestSummary selectedRequest = view.requestTable.getSelectionModel().getSelectedItem();

        RequestActionResult result = approve
                ? requestActionService.approveRequest(session, selectedRequest)
                : requestActionService.rejectRequest(session, selectedRequest);

        if (result.success()) {
            refreshAll.run();
            view.requestActionStatusLabel.setText(result.message());
            return;
        }

        view.requestActionStatusLabel.setText(result.message());
    }

    private void clearRequestDraftState() {
        editingRequestNumber = null;
        requestDraftLines.clear();
        view.requestDraftTable.getSelectionModel().clearSelection();
        view.requestQuantityField.clear();
        view.requestQuantityField.setPromptText("Qty");
        view.requestNoteField.clear();
        view.requestInventoryTable.refresh();
        updateRequestDraftButtons();
    }

    private void selectWarehouseByCode(ComboBox<Warehouse> selector, String warehouseCode) {
        if (warehouseCode == null) {
            return;
        }

        selector.getItems().stream()
                .filter(warehouse -> warehouse.code().equals(warehouseCode))
                .findFirst()
                .ifPresent(warehouse -> selector.getSelectionModel().select(warehouse));
    }

    private void refreshSelectedRequestDetail() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            clearRequestDetail();
            return;
        }

        RequestSummary selectedRequest = view.requestTable.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            clearRequestDetail();
            return;
        }

        RequestDetailRecord detail = requestDetailService.load(session, selectedRequest.requestNumber());
        if (detail == null) {
            clearRequestDetail();
            return;
        }

        currentRequestDetail = detail;
        view.requestDetailNumberLabel.setText(detail.requestNumber());
        view.requestDetailStatusLabel.setText(detail.status().displayName());
        view.requestDetailRouteLabel.setText(detail.sourceWarehouseLabel() + " -> " + detail.destinationWarehouseLabel());
        view.requestDetailUsersLabel.setText("Requested by " + detail.requestedBy()
                + (detail.approvedBy() == null || detail.approvedBy().isBlank() ? "" : " | Handled by " + detail.approvedBy()));
        view.requestDetailNoteLabel.setText(detail.note() == null || detail.note().isBlank() ? "No request note." : detail.note());
        view.requestDetailUpdatedAtLabel.setText("Created " + detail.createdAt().format(TIMESTAMP_FORMATTER)
                + " | Updated " + detail.updatedAt().format(TIMESTAMP_FORMATTER));
        view.requestDetailLineTable.getItems().setAll(detail.lines());
        view.requestDetailLineTable.refresh();
        updateRequestActionButtons(DatabaseConnection.canConnect());
    }

    private void clearRequestDetail() {
        currentRequestDetail = null;
        view.requestDetailNumberLabel.setText("No request selected");
        view.requestDetailStatusLabel.setText("-");
        view.requestDetailRouteLabel.setText("-");
        view.requestDetailUsersLabel.setText("-");
        view.requestDetailNoteLabel.setText("Select a request to inspect its note and requested items.");
        view.requestDetailUpdatedAtLabel.setText("-");
        view.requestDetailLineTable.getItems().clear();
        updateRequestActionButtons(DatabaseConnection.canConnect());
    }

    private int getDraftQuantity(String productCode) {
        return requestDraftLines.stream()
                .filter(line -> line.productCode().equals(productCode))
                .mapToInt(RequestDraftLine::quantity)
                .sum();
    }

    private int getRemainingAvailableQuantity(InventorySummary inventoryItem) {
        if (inventoryItem == null) {
            return 0;
        }
        return Math.max(0, inventoryItem.quantity() - getDraftQuantity(inventoryItem.productCode()));
    }

    private int findDraftLineIndex(String productCode) {
        for (int index = 0; index < requestDraftLines.size(); index++) {
            if (requestDraftLines.get(index).productCode().equals(productCode)) {
                return index;
            }
        }
        return -1;
    }

    private void selectOrFirst(ComboBox<String> selector, String currentValue) {
        if (currentValue != null && selector.getItems().contains(currentValue)) {
            selector.getSelectionModel().select(currentValue);
        } else if (selector.getSelectionModel().isEmpty() && !selector.getItems().isEmpty()) {
            selector.getSelectionModel().selectFirst();
        }
    }

    private String normalizeAllSelection(ComboBox<String> selector) {
        String selectedValue = selector.getSelectionModel().getSelectedItem();
        if (selectedValue == null || selectedValue.startsWith("All ")) {
            return null;
        }
        return selectedValue;
    }

    private int parseQuantity(String rawValue) {
        try {
            return Integer.parseInt(rawValue == null ? "" : rawValue.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    record View(
            Label inventorySummaryLabel,
            TextField inventorySearchField,
            ComboBox<String> inventoryCategoryFilterSelector,
            ComboBox<String> inventoryManufacturerFilterSelector,
            Label requestSummaryLabel,
            Button requestIncomingViewButton,
            Button requestOutgoingViewButton,
            Button requestHistoryViewButton,
            Label movementLogSummaryLabel,
            ComboBox<String> movementTypeFilterSelector,
            DatePicker movementFromDatePicker,
            DatePicker movementToDatePicker,
            TableView<InventorySummary> inventoryTable,
            TableColumn<InventorySummary, String> inventoryWarehouseColumn,
            TableColumn<InventorySummary, String> inventoryCodeColumn,
            TableColumn<InventorySummary, String> inventoryProductColumn,
            TableColumn<InventorySummary, String> inventoryManufacturerColumn,
            TableColumn<InventorySummary, String> inventoryCategoryColumn,
            TableColumn<InventorySummary, String> inventoryQuantityColumn,
            ComboBox<Warehouse> requestSourceWarehouseSelector,
            ComboBox<Warehouse> requestTargetWarehouseSelector,
            TextField requestInventorySearchField,
            Label requestInventorySummaryLabel,
            TableView<InventorySummary> requestInventoryTable,
            TableColumn<InventorySummary, String> requestInventoryCodeColumn,
            TableColumn<InventorySummary, String> requestInventoryProductColumn,
            TableColumn<InventorySummary, String> requestInventoryManufacturerColumn,
            TableColumn<InventorySummary, String> requestInventoryCategoryColumn,
            TableColumn<InventorySummary, String> requestInventoryQuantityColumn,
            TextField requestQuantityField,
            TextField requestNoteField,
            Label requestEditorTitleLabel,
            Button createRequestButton,
            Button saveRequestEditsButton,
            Button approveRequestButton,
            Button rejectRequestButton,
            Button editSelectedRequestButton,
            Button cancelSelectedRequestButton,
            Label requestActionStatusLabel,
            TableView<RequestDraftLine> requestDraftTable,
            TableColumn<RequestDraftLine, String> requestDraftCodeColumn,
            TableColumn<RequestDraftLine, String> requestDraftProductColumn,
            TableColumn<RequestDraftLine, String> requestDraftQuantityColumn,
            Button addRequestLineButton,
            Button removeRequestLineButton,
            Button clearRequestDraftButton,
            TableView<RequestSummary> requestTable,
            TableColumn<RequestSummary, String> requestNumberColumn,
            TableColumn<RequestSummary, String> requestSourceColumn,
            TableColumn<RequestSummary, String> requestDestinationColumn,
            TableColumn<RequestSummary, String> requestRequestedByColumn,
            TableColumn<RequestSummary, String> requestStatusColumn,
            TableColumn<RequestSummary, String> requestLineCountColumn,
            TableColumn<RequestSummary, String> requestTotalQuantityColumn,
            TableColumn<RequestSummary, String> requestCreatedAtColumn,
            Label requestDetailNumberLabel,
            Label requestDetailStatusLabel,
            Label requestDetailRouteLabel,
            Label requestDetailUsersLabel,
            Label requestDetailNoteLabel,
            Label requestDetailUpdatedAtLabel,
            TableView<RequestDetailLine> requestDetailLineTable,
            TableColumn<RequestDetailLine, String> requestDetailCodeColumn,
            TableColumn<RequestDetailLine, String> requestDetailProductColumn,
            TableColumn<RequestDetailLine, String> requestDetailRequestedColumn,
            TableColumn<RequestDetailLine, String> requestDetailApprovedColumn,
            TableView<MovementLogEntry> movementLogTable,
            TableColumn<MovementLogEntry, String> timestampColumn,
            TableColumn<MovementLogEntry, String> movementTypeColumn,
            TableColumn<MovementLogEntry, String> productColumn,
            TableColumn<MovementLogEntry, String> manufacturerColumn,
            TableColumn<MovementLogEntry, String> quantityColumn,
            TableColumn<MovementLogEntry, String> sourceWarehouseColumn,
            TableColumn<MovementLogEntry, String> destinationWarehouseColumn,
            TableColumn<MovementLogEntry, String> userColumn,
            TableColumn<MovementLogEntry, String> detailsColumn
    ) {
    }
}
