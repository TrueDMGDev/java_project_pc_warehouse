package com.pcwarehouse.controller;

import com.pcwarehouse.model.InventoryFilterCriteria;
import com.pcwarehouse.model.InventoryReportRow;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.service.InventoryInsightsService;
import com.pcwarehouse.service.ProductManagementService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

final class ReportsPageController {

    private static final String SORT_BY_WAREHOUSE = "Warehouse";
    private static final String SORT_BY_CODE = "Code";
    private static final String SORT_BY_PRODUCT = "Product";
    private static final String SORT_BY_QUANTITY = "Quantity";

    private final View view;
    private final Supplier<UserSession> sessionSupplier;
    private final InventoryInsightsService inventoryInsightsService = new InventoryInsightsService();
    private final ProductManagementService productManagementService = new ProductManagementService();
    private boolean suppressReportFilterRefresh;

    ReportsPageController(View view, Supplier<UserSession> sessionSupplier) {
        this.view = view;
        this.sessionSupplier = sessionSupplier;
    }

    void initialize() {
        view.reportWarehouseSelector.setOnAction(event -> {
            if (!suppressReportFilterRefresh) {
                refreshReportOnly();
            }
        });
        view.reportCategorySelector.setOnAction(event -> {
            if (!suppressReportFilterRefresh) {
                refreshReportOnly();
            }
        });
        view.reportManufacturerSelector.setOnAction(event -> {
            if (!suppressReportFilterRefresh) {
                refreshReportOnly();
            }
        });
        view.reportSortSelector.setItems(FXCollections.observableArrayList(
                SORT_BY_WAREHOUSE,
                SORT_BY_CODE,
                SORT_BY_PRODUCT,
                SORT_BY_QUANTITY
        ));
        view.reportSortSelector.getSelectionModel().select(SORT_BY_WAREHOUSE);
        view.reportSortSelector.setOnAction(event -> {
            if (!suppressReportFilterRefresh) {
                refreshReportOnly();
            }
        });

        configureReportTable();
    }

    void handleRefreshReport() {
        refreshReportOnly();
    }

    void handleClearReportFilters() {
        suppressReportFilterRefresh = true;
        try {
            if (!view.reportWarehouseSelector.getItems().isEmpty()) {
                view.reportWarehouseSelector.getSelectionModel().selectFirst();
            }
            if (!view.reportCategorySelector.getItems().isEmpty()) {
                view.reportCategorySelector.getSelectionModel().selectFirst();
            }
            if (!view.reportManufacturerSelector.getItems().isEmpty()) {
                view.reportManufacturerSelector.getSelectionModel().selectFirst();
            }
            if (!view.reportSortSelector.getItems().isEmpty()) {
                view.reportSortSelector.getSelectionModel().select(SORT_BY_WAREHOUSE);
            }
        } finally {
            suppressReportFilterRefresh = false;
        }
        refreshReportOnly();
    }

    void refreshReportOnly() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }
        updateControls();
        updateView();
    }

    void updateControls() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        Warehouse selectedWarehouse = view.reportWarehouseSelector.getSelectionModel().getSelectedItem();
        String selectedCategory = view.reportCategorySelector.getSelectionModel().getSelectedItem();
        String selectedManufacturer = view.reportManufacturerSelector.getSelectionModel().getSelectedItem();
        String selectedSort = view.reportSortSelector.getSelectionModel().getSelectedItem();

        List<Warehouse> warehouses = new ArrayList<>();
        warehouses.add(new Warehouse("", "All warehouses", ""));
        warehouses.addAll(inventoryInsightsService.loadReportWarehouses(session));

        List<String> categories = new ArrayList<>();
        categories.add("All categories");
        categories.addAll(productManagementService.loadCategories());

        List<String> manufacturers = new ArrayList<>();
        manufacturers.add("All manufacturers");
        manufacturers.addAll(productManagementService.loadManufacturers());

        suppressReportFilterRefresh = true;
        try {
            view.reportWarehouseSelector.setItems(FXCollections.observableArrayList(warehouses));
            if (selectedWarehouse != null) {
                warehouses.stream()
                        .filter(warehouse -> warehouse.code().equals(selectedWarehouse.code()))
                        .findFirst()
                        .ifPresentOrElse(
                                warehouse -> view.reportWarehouseSelector.getSelectionModel().select(warehouse),
                                () -> view.reportWarehouseSelector.getSelectionModel().selectFirst()
                        );
            } else {
                view.reportWarehouseSelector.getSelectionModel().selectFirst();
            }
            view.reportCategorySelector.setItems(FXCollections.observableArrayList(categories));
            view.reportManufacturerSelector.setItems(FXCollections.observableArrayList(manufacturers));
            selectOrFirst(view.reportCategorySelector, selectedCategory);
            selectOrFirst(view.reportManufacturerSelector, selectedManufacturer);
            selectOrFirst(view.reportSortSelector, selectedSort == null ? SORT_BY_WAREHOUSE : selectedSort);
        } finally {
            suppressReportFilterRefresh = false;
        }
    }

    void updateView() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        Warehouse selectedWarehouse = view.reportWarehouseSelector.getSelectionModel().getSelectedItem();
        InventoryFilterCriteria criteria = new InventoryFilterCriteria(
                "",
                normalizeAllSelection(view.reportCategorySelector),
                normalizeAllSelection(view.reportManufacturerSelector),
                selectedWarehouse == null || selectedWarehouse.code().isBlank() ? null : selectedWarehouse.code()
        );
        List<InventoryReportRow> rows = inventoryInsightsService.loadInventoryReport(session, criteria);
        rows = sortReportRows(rows);
        view.reportTable.getItems().setAll(rows);
        view.reportSummaryLabel.setText(rows.size() + " report rows shown");
    }

    private void configureReportTable() {
        view.reportWarehouseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseLabel()));
        view.reportCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.reportProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        view.reportManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        view.reportCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        view.reportQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        view.reportTable.setPlaceholder(new Label("No rows match the current report filters."));
    }

    private List<InventoryReportRow> sortReportRows(List<InventoryReportRow> rows) {
        String selectedSort = view.reportSortSelector.getSelectionModel().getSelectedItem();
        Comparator<InventoryReportRow> comparator = switch (selectedSort == null ? SORT_BY_WAREHOUSE : selectedSort) {
            case SORT_BY_CODE -> Comparator.comparing(InventoryReportRow::productCode);
            case SORT_BY_PRODUCT -> Comparator.comparing(InventoryReportRow::productName);
            case SORT_BY_QUANTITY -> Comparator.comparingInt(InventoryReportRow::quantity).reversed();
            default -> Comparator.comparing(InventoryReportRow::warehouseLabel)
                    .thenComparing(InventoryReportRow::category)
                    .thenComparing(InventoryReportRow::productName);
        };
        return rows.stream().sorted(comparator).toList();
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

    record View(
            ComboBox<Warehouse> reportWarehouseSelector,
            ComboBox<String> reportCategorySelector,
            ComboBox<String> reportManufacturerSelector,
            ComboBox<String> reportSortSelector,
            Label reportSummaryLabel,
            TableView<InventoryReportRow> reportTable,
            TableColumn<InventoryReportRow, String> reportWarehouseColumn,
            TableColumn<InventoryReportRow, String> reportCodeColumn,
            TableColumn<InventoryReportRow, String> reportProductColumn,
            TableColumn<InventoryReportRow, String> reportManufacturerColumn,
            TableColumn<InventoryReportRow, String> reportCategoryColumn,
            TableColumn<InventoryReportRow, String> reportQuantityColumn
    ) {
    }
}
