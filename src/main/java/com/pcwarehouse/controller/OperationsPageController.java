package com.pcwarehouse.controller;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.InventoryFilterCriteria;
import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.MovementType;
import com.pcwarehouse.model.ProductOption;
import com.pcwarehouse.model.StockOperationLine;
import com.pcwarehouse.model.StockOperationResult;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.service.InventoryInsightsService;
import com.pcwarehouse.service.ProductManagementService;
import com.pcwarehouse.service.StockOperationService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.Supplier;

final class OperationsPageController {

    private final View view;
    private final Supplier<UserSession> sessionSupplier;
    private final Runnable refreshAll;
    private final StockOperationService stockOperationService = new StockOperationService();
    private final ProductManagementService productManagementService = new ProductManagementService();
    private final InventoryInsightsService inventoryInsightsService = new InventoryInsightsService();
    private final ObservableList<StockOperationLine> supplyDraftLines = FXCollections.observableArrayList();
    private final ObservableList<StockOperationLine> dispatchDraftLines = FXCollections.observableArrayList();
    private List<ProductOption> allSupplyProducts = List.of();
    private boolean suppressSupplyProductSearch;
    private int supplyProductSearchVersion;

    OperationsPageController(View view, Supplier<UserSession> sessionSupplier, Runnable refreshAll) {
        this.view = view;
        this.sessionSupplier = sessionSupplier;
        this.refreshAll = refreshAll;
    }

    void initialize() {
        view.supplyWarehouseSelector.setOnAction(event -> refreshDispatchInventory());
        view.dispatchWarehouseSelector.setOnAction(event -> refreshDispatchInventory());
        view.dispatchInventorySearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshDispatchInventory());
        configureSupplyProductSelector();
        configureSupplyDraftTable();
        configureDispatchInventoryTable();
        configureDispatchDraftTable();
    }

    void handleToggleNewSupplyProduct() {
        refreshSupplyProductMode();
    }

    void handleAddSupplyLine() {
        int quantity = parseQuantity(view.supplyQuantityField.getText());
        if (quantity <= 0) {
            view.supplyStatusLabel.setText("Enter a positive supply quantity.");
            return;
        }

        StockOperationLine line;
        if (view.supplyNewProductCheckBox.isSelected()) {
            line = new StockOperationLine(
                    true,
                    view.supplyNewProductCodeField.getText().trim(),
                    view.supplyNewProductNameField.getText().trim(),
                    view.supplyNewProductManufacturerSelector.getSelectionModel().getSelectedItem(),
                    view.supplyNewProductCategorySelector.getSelectionModel().getSelectedItem(),
                    view.supplyNewProductUnitField.getText().isBlank() ? "pcs" : view.supplyNewProductUnitField.getText().trim(),
                    view.supplyNewProductDescriptionField.getText(),
                    quantity
            );
            if (line.productCode().isBlank() || line.productName().isBlank()
                    || line.manufacturer() == null || line.category() == null) {
                view.supplyStatusLabel.setText("New products need code, name, manufacturer, and category.");
                return;
            }
        } else {
            ProductOption product = getSelectedSupplyProduct();
            if (product == null) {
                view.supplyStatusLabel.setText("Select an existing product, or check New product to catalog.");
                return;
            }
            line = StockOperationLine.existingProduct(product, quantity);
        }

        upsertStockLine(supplyDraftLines, line);
        view.supplyQuantityField.clear();
        view.supplyStatusLabel.setText("Added " + line.productLabel() + " to the supply draft.");
        updateStockOperationButtons(DatabaseConnection.canConnect());
    }

    void handleRemoveSupplyLine() {
        StockOperationLine selectedLine = view.supplyDraftTable.getSelectionModel().getSelectedItem();
        if (selectedLine == null) {
            view.supplyStatusLabel.setText("Select a supply draft line to remove.");
            return;
        }
        supplyDraftLines.remove(selectedLine);
        view.supplyStatusLabel.setText("Removed " + selectedLine.productName() + ".");
        updateStockOperationButtons(DatabaseConnection.canConnect());
    }

    void handleClearSupplyDraft() {
        supplyDraftLines.clear();
        view.supplyDraftTable.getSelectionModel().clearSelection();
        view.supplyStatusLabel.setText("Supply draft cleared.");
        updateStockOperationButtons(DatabaseConnection.canConnect());
    }

    void handleRecordSupply() {
        UserSession session = sessionSupplier.get();
        StockOperationResult result = stockOperationService.supplyLines(
                session,
                view.supplyWarehouseSelector.getSelectionModel().getSelectedItem(),
                List.copyOf(supplyDraftLines),
                readOptionalField(view.supplySupplierField),
                buildSupplyDetails()
        );
        if (result.success()) {
            supplyDraftLines.clear();
            view.supplyQuantityField.clear();
            view.supplySupplierField.clear();
            view.supplyNoteField.clear();
            refreshAll.run();
        }
        view.supplyStatusLabel.setText(result.message());
    }

    void handleAddDispatchLine() {
        InventorySummary selectedInventory = view.dispatchInventoryTable.getSelectionModel().getSelectedItem();
        int quantity = parseQuantity(view.dispatchQuantityField.getText());
        if (selectedInventory == null) {
            view.dispatchStatusLabel.setText("Select an inventory row before adding a dispatch line.");
            return;
        }
        if (quantity <= 0) {
            view.dispatchStatusLabel.setText("Enter a positive dispatch quantity.");
            return;
        }
        int remainingQuantity = getRemainingDispatchQuantity(selectedInventory);
        if (quantity > remainingQuantity) {
            view.dispatchStatusLabel.setText("Only " + remainingQuantity + " unit(s) are still available for "
                    + selectedInventory.productName() + ".");
            return;
        }

        StockOperationLine line = StockOperationLine.inventoryProduct(selectedInventory, quantity);
        upsertStockLine(dispatchDraftLines, line);
        view.dispatchQuantityField.clear();
        view.dispatchQuantityField.setPromptText("Max " + getRemainingDispatchQuantity(selectedInventory));
        view.dispatchInventoryTable.refresh();
        view.dispatchStatusLabel.setText("Added " + line.productLabel() + " to the dispatch draft.");
        updateStockOperationButtons(DatabaseConnection.canConnect());
    }

    void handleRemoveDispatchLine() {
        StockOperationLine selectedLine = view.dispatchDraftTable.getSelectionModel().getSelectedItem();
        if (selectedLine == null) {
            view.dispatchStatusLabel.setText("Select a dispatch draft line to remove.");
            return;
        }
        dispatchDraftLines.remove(selectedLine);
        view.dispatchInventoryTable.refresh();
        view.dispatchStatusLabel.setText("Removed " + selectedLine.productName() + ".");
        updateStockOperationButtons(DatabaseConnection.canConnect());
    }

    void handleClearDispatchDraft() {
        dispatchDraftLines.clear();
        view.dispatchDraftTable.getSelectionModel().clearSelection();
        view.dispatchInventoryTable.refresh();
        view.dispatchStatusLabel.setText("Dispatch draft cleared.");
        updateStockOperationButtons(DatabaseConnection.canConnect());
    }

    void handleRecordDispatch() {
        UserSession session = sessionSupplier.get();
        StockOperationResult result = stockOperationService.dispatchLines(
                session,
                view.dispatchWarehouseSelector.getSelectionModel().getSelectedItem(),
                List.copyOf(dispatchDraftLines),
                readOptionalField(view.dispatchDestinationField),
                buildDispatchDetails()
        );
        if (result.success()) {
            dispatchDraftLines.clear();
            view.dispatchQuantityField.clear();
            view.dispatchDestinationField.clear();
            view.dispatchNoteField.clear();
            refreshAll.run();
        }
        view.dispatchStatusLabel.setText(result.message());
    }

    void updateControls(UserSession session, boolean usingLiveDatabase) {
        Warehouse selectedSupplyWarehouse = view.supplyWarehouseSelector.getSelectionModel().getSelectedItem();
        Warehouse selectedDispatchWarehouse = view.dispatchWarehouseSelector.getSelectionModel().getSelectedItem();
        ProductOption selectedSupplyProduct = view.supplyProductSelector.getValue();

        List<Warehouse> warehouses = stockOperationService.loadWarehouses(session);
        allSupplyProducts = stockOperationService.loadProducts();

        view.supplyWarehouseSelector.setItems(FXCollections.observableArrayList(warehouses));
        view.dispatchWarehouseSelector.setItems(FXCollections.observableArrayList(warehouses));
        setSupplyProductItems(allSupplyProducts);
        view.supplyNewProductManufacturerSelector.setItems(FXCollections.observableArrayList(productManagementService.loadManufacturers()));
        view.supplyNewProductCategorySelector.setItems(FXCollections.observableArrayList(productManagementService.loadCategories()));

        selectWarehouseOrFirst(view.supplyWarehouseSelector, selectedSupplyWarehouse, warehouses);
        selectWarehouseOrFirst(view.dispatchWarehouseSelector, selectedDispatchWarehouse, warehouses);
        if (selectedSupplyProduct != null) {
            allSupplyProducts.stream()
                    .filter(product -> product.productCode().equals(selectedSupplyProduct.productCode()))
                    .findFirst()
                    .ifPresent(product -> view.supplyProductSelector.getSelectionModel().select(product));
        } else if (!allSupplyProducts.isEmpty() && view.supplyProductSelector.getSelectionModel().isEmpty()) {
            view.supplyProductSelector.getSelectionModel().selectFirst();
        }

        boolean canSupply = session.role().canSupply() && usingLiveDatabase;
        boolean canDispatch = session.role().canDispatch() && usingLiveDatabase;
        boolean warehouseEditable = session.isAdmin() && usingLiveDatabase;

        view.supplyWarehouseSelector.setDisable(!warehouseEditable);
        view.dispatchWarehouseSelector.setDisable(!warehouseEditable);
        refreshSupplyProductMode();
        view.supplyQuantityField.setDisable(!canSupply);
        view.supplySupplierField.setDisable(!canSupply);
        view.supplyNoteField.setDisable(!canSupply);
        view.addSupplyLineButton.setDisable(!canSupply);
        view.recordSupplyButton.setDisable(!canSupply || supplyDraftLines.isEmpty());

        view.dispatchInventorySearchField.setDisable(!canDispatch);
        view.dispatchQuantityField.setDisable(!canDispatch);
        view.dispatchDestinationField.setDisable(!canDispatch);
        view.dispatchNoteField.setDisable(!canDispatch);
        updateStockOperationButtons(usingLiveDatabase);
        refreshDispatchInventory();

        if (!usingLiveDatabase) {
            view.supplyStatusLabel.setText("Supply actions need a live database connection.");
            view.dispatchStatusLabel.setText("Dispatch actions need a live database connection.");
        } else {
            if (view.supplyStatusLabel.getText() == null || view.supplyStatusLabel.getText().isBlank()) {
                view.supplyStatusLabel.setText(canSupply ? "Build a supply draft, then record it." : "Supply is read-only for this session.");
            }
            if (view.dispatchStatusLabel.getText() == null || view.dispatchStatusLabel.getText().isBlank()) {
                view.dispatchStatusLabel.setText(canDispatch ? "Select inventory rows to dispatch." : "Dispatch is read-only for this session.");
            }
        }
    }

    void updateStockOperationButtons(boolean usingLiveDatabase) {
        UserSession session = sessionSupplier.get();
        boolean canSupply = session != null && session.role().canSupply() && usingLiveDatabase;
        boolean canDispatch = session != null && session.role().canDispatch() && usingLiveDatabase;

        view.addSupplyLineButton.setDisable(!canSupply);
        view.removeSupplyLineButton.setDisable(!canSupply || supplyDraftLines.isEmpty());
        view.clearSupplyDraftButton.setDisable(!canSupply || supplyDraftLines.isEmpty());
        view.recordSupplyButton.setDisable(!canSupply || supplyDraftLines.isEmpty());

        InventorySummary selectedInventory = view.dispatchInventoryTable.getSelectionModel().getSelectedItem();
        view.addDispatchLineButton.setDisable(!canDispatch || selectedInventory == null || getRemainingDispatchQuantity(selectedInventory) <= 0);
        view.removeDispatchLineButton.setDisable(!canDispatch || dispatchDraftLines.isEmpty());
        view.clearDispatchDraftButton.setDisable(!canDispatch || dispatchDraftLines.isEmpty());
        view.recordDispatchButton.setDisable(!canDispatch || dispatchDraftLines.isEmpty());
    }

    private void configureSupplyProductSelector() {
        view.supplyProductSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductOption product) {
                return product == null ? "" : product.toString();
            }

            @Override
            public ProductOption fromString(String value) {
                return findSupplyProductByExactText(value);
            }
        });
        view.supplyProductSelector.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!suppressSupplyProductSearch && !view.supplyNewProductCheckBox.isSelected()) {
                ProductOption selectedProduct = view.supplyProductSelector.getValue();
                if (selectedProduct != null && selectedProduct.toString().equals(newValue)) {
                    return;
                }
                if (isSupplyProductDisplayText(newValue)) {
                    return;
                }
                scheduleSupplyProductFilter(newValue);
            }
        });
    }

    private void configureSupplyDraftTable() {
        view.supplyDraftCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.supplyDraftProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productLabel()));
        view.supplyDraftQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        view.supplyDraftTable.setItems(supplyDraftLines);
        view.supplyDraftTable.setPlaceholder(new Label("Add supply lines above before recording stock."));
    }

    private void configureDispatchInventoryTable() {
        view.dispatchInventoryCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.dispatchInventoryProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        view.dispatchInventoryManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        view.dispatchInventoryCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        view.dispatchInventoryQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        view.dispatchInventoryTable.setPlaceholder(new Label("Search current warehouse inventory, then select a product to dispatch."));
        view.dispatchInventoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedItem) -> {
            view.dispatchQuantityField.setPromptText(selectedItem == null ? "Qty" : "Max " + getRemainingDispatchQuantity(selectedItem));
            updateStockOperationButtons(DatabaseConnection.canConnect());
        });
    }

    private void configureDispatchDraftTable() {
        view.dispatchDraftCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.dispatchDraftProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productLabel()));
        view.dispatchDraftQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        view.dispatchDraftTable.setItems(dispatchDraftLines);
        view.dispatchDraftTable.setPlaceholder(new Label("Add dispatch lines above before recording stock."));
    }

    private void refreshSupplyProductMode() {
        UserSession session = sessionSupplier.get();
        boolean canSupply = session != null && session.role().canSupply() && DatabaseConnection.canConnect();
        boolean newProduct = view.supplyNewProductCheckBox.isSelected();
        view.supplyProductSelector.setDisable(!canSupply || newProduct);
        view.supplyNewProductFields.setVisible(newProduct);
        view.supplyNewProductFields.setManaged(newProduct);
        view.supplyNewProductCodeField.setDisable(!canSupply || !newProduct);
        view.supplyNewProductNameField.setDisable(!canSupply || !newProduct);
        view.supplyNewProductUnitField.setDisable(!canSupply || !newProduct);
        view.supplyNewProductManufacturerSelector.setDisable(!canSupply || !newProduct);
        view.supplyNewProductCategorySelector.setDisable(!canSupply || !newProduct);
        view.supplyNewProductDescriptionField.setDisable(!canSupply || !newProduct);
    }

    private void setSupplyProductItems(List<ProductOption> products) {
        String editorText = view.supplyProductSelector.getEditor().getText();
        int caretPosition = view.supplyProductSelector.getEditor().getCaretPosition();
        suppressSupplyProductSearch = true;
        try {
            view.supplyProductSelector.setItems(FXCollections.observableArrayList(products));
        } finally {
            suppressSupplyProductSearch = false;
        }
        restoreSupplyProductEditor(editorText, caretPosition);
    }

    private void scheduleSupplyProductFilter(String searchTerm) {
        int version = ++supplyProductSearchVersion;
        Platform.runLater(() -> {
            if (version == supplyProductSearchVersion) {
                filterSupplyProductSelector(searchTerm);
            }
        });
    }

    private void filterSupplyProductSelector(String searchTerm) {
        String normalizedSearch = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
        int caretPosition = view.supplyProductSelector.getEditor().getCaretPosition();
        List<ProductOption> filteredProducts = normalizedSearch.isEmpty()
                ? allSupplyProducts
                : allSupplyProducts.stream()
                .filter(product -> product.productCode().toLowerCase().contains(normalizedSearch)
                        || product.productName().toLowerCase().contains(normalizedSearch)
                        || product.manufacturer().toLowerCase().contains(normalizedSearch)
                        || product.category().toLowerCase().contains(normalizedSearch))
                .toList();

        suppressSupplyProductSearch = true;
        try {
            view.supplyProductSelector.setItems(FXCollections.observableArrayList(filteredProducts));
            view.supplyProductSelector.getEditor().setText(searchTerm == null ? "" : searchTerm);
            view.supplyProductSelector.getEditor().positionCaret(Math.min(caretPosition, view.supplyProductSelector.getEditor().getText().length()));
            view.supplyProductSelector.show();
        } finally {
            suppressSupplyProductSearch = false;
        }
    }

    private ProductOption getSelectedSupplyProduct() {
        ProductOption selectedProduct = view.supplyProductSelector.getValue();
        String editorText = view.supplyProductSelector.getEditor().getText();
        if (selectedProduct != null && selectedProduct.toString().equals(editorText)) {
            return selectedProduct;
        }
        return findSupplyProductByExactText(editorText);
    }

    private boolean isSupplyProductDisplayText(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return allSupplyProducts.stream().anyMatch(product -> product.toString().equals(value));
    }

    private ProductOption findSupplyProductByExactText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return allSupplyProducts.stream()
                .filter(product -> product.toString().equals(value)
                        || product.productCode().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }

    private void restoreSupplyProductEditor(String text, int caretPosition) {
        view.supplyProductSelector.getEditor().setText(text == null ? "" : text);
        view.supplyProductSelector.getEditor().positionCaret(Math.min(caretPosition, view.supplyProductSelector.getEditor().getText().length()));
    }

    private void refreshDispatchInventory() {
        UserSession session = sessionSupplier.get();
        if (session == null || view.dispatchInventoryTable == null) {
            return;
        }

        Warehouse selectedWarehouse = view.dispatchWarehouseSelector.getSelectionModel().getSelectedItem();
        if (selectedWarehouse == null) {
            view.dispatchInventoryTable.getItems().clear();
            view.dispatchInventorySummaryLabel.setText("Select a warehouse to view inventory.");
            updateStockOperationButtons(DatabaseConnection.canConnect());
            return;
        }

        InventoryFilterCriteria criteria = new InventoryFilterCriteria(
                view.dispatchInventorySearchField.getText(),
                null,
                null,
                selectedWarehouse.code()
        );
        List<InventorySummary> items = inventoryInsightsService.loadInventory(session, criteria);
        view.dispatchInventoryTable.getItems().setAll(items);
        view.dispatchInventoryTable.refresh();
        view.dispatchInventorySummaryLabel.setText(items.size() + " item(s) in " + selectedWarehouse.label());
        updateStockOperationButtons(DatabaseConnection.canConnect());
    }

    private void upsertStockLine(ObservableList<StockOperationLine> lines, StockOperationLine newLine) {
        for (int index = 0; index < lines.size(); index++) {
            StockOperationLine existingLine = lines.get(index);
            if (existingLine.productCode().equals(newLine.productCode())) {
                int updatedQuantity = existingLine.quantity() + newLine.quantity();
                StockOperationLine mergedLine = new StockOperationLine(
                        existingLine.newProduct(),
                        existingLine.productCode(),
                        existingLine.productName(),
                        existingLine.manufacturer(),
                        existingLine.category(),
                        existingLine.unitName(),
                        existingLine.description(),
                        updatedQuantity
                );
                lines.set(index, mergedLine);
                return;
            }
        }
        lines.add(newLine);
    }

    private String buildSupplyDetails() {
        return readOptionalField(view.supplyNoteField);
    }

    private String buildDispatchDetails() {
        return readOptionalField(view.dispatchNoteField);
    }

    private String readOptionalField(TextField field) {
        if (field == null || field.getText() == null || field.getText().isBlank()) {
            return null;
        }
        return field.getText().trim();
    }

    private int getDispatchDraftQuantity(String productCode) {
        return dispatchDraftLines.stream()
                .filter(line -> line.productCode().equals(productCode))
                .mapToInt(StockOperationLine::quantity)
                .sum();
    }

    private int getRemainingDispatchQuantity(InventorySummary inventoryItem) {
        if (inventoryItem == null) {
            return 0;
        }
        return Math.max(0, inventoryItem.quantity() - getDispatchDraftQuantity(inventoryItem.productCode()));
    }

    private void selectWarehouseOrFirst(ComboBox<Warehouse> selector, Warehouse preferredWarehouse, List<Warehouse> warehouses) {
        if (preferredWarehouse != null) {
            warehouses.stream()
                    .filter(warehouse -> warehouse.code().equals(preferredWarehouse.code()))
                    .findFirst()
                    .ifPresent(warehouse -> selector.getSelectionModel().select(warehouse));
        }

        if (selector.getSelectionModel().isEmpty() && !warehouses.isEmpty()) {
            selector.getSelectionModel().selectFirst();
        }
    }

    private int parseQuantity(String rawValue) {
        try {
            return Integer.parseInt(rawValue == null ? "" : rawValue.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    record View(
            ComboBox<Warehouse> supplyWarehouseSelector,
            ComboBox<ProductOption> supplyProductSelector,
            CheckBox supplyNewProductCheckBox,
            Node supplyNewProductFields,
            TextField supplyNewProductCodeField,
            TextField supplyNewProductNameField,
            TextField supplyNewProductUnitField,
            ComboBox<String> supplyNewProductManufacturerSelector,
            ComboBox<String> supplyNewProductCategorySelector,
            TextField supplyNewProductDescriptionField,
            TextField supplyQuantityField,
            TextField supplySupplierField,
            TextField supplyNoteField,
            Button addSupplyLineButton,
            Button removeSupplyLineButton,
            Button clearSupplyDraftButton,
            Button recordSupplyButton,
            Label supplyStatusLabel,
            TableView<StockOperationLine> supplyDraftTable,
            TableColumn<StockOperationLine, String> supplyDraftCodeColumn,
            TableColumn<StockOperationLine, String> supplyDraftProductColumn,
            TableColumn<StockOperationLine, String> supplyDraftQuantityColumn,
            ComboBox<Warehouse> dispatchWarehouseSelector,
            TextField dispatchInventorySearchField,
            Label dispatchInventorySummaryLabel,
            TableView<InventorySummary> dispatchInventoryTable,
            TableColumn<InventorySummary, String> dispatchInventoryCodeColumn,
            TableColumn<InventorySummary, String> dispatchInventoryProductColumn,
            TableColumn<InventorySummary, String> dispatchInventoryManufacturerColumn,
            TableColumn<InventorySummary, String> dispatchInventoryCategoryColumn,
            TableColumn<InventorySummary, String> dispatchInventoryQuantityColumn,
            TextField dispatchQuantityField,
            TextField dispatchDestinationField,
            TextField dispatchNoteField,
            Button addDispatchLineButton,
            Button removeDispatchLineButton,
            Button clearDispatchDraftButton,
            Button recordDispatchButton,
            Label dispatchStatusLabel,
            TableView<StockOperationLine> dispatchDraftTable,
            TableColumn<StockOperationLine, String> dispatchDraftCodeColumn,
            TableColumn<StockOperationLine, String> dispatchDraftProductColumn,
            TableColumn<StockOperationLine, String> dispatchDraftQuantityColumn
    ) {
    }
}
