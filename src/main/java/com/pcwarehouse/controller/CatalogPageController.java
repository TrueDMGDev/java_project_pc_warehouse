package com.pcwarehouse.controller;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.CatalogLookupChangeResult;
import com.pcwarehouse.model.CatalogLookupRecord;
import com.pcwarehouse.model.ProductChangeResult;
import com.pcwarehouse.model.ProductRecord;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.service.CatalogLookupManagementService;
import com.pcwarehouse.service.ProductManagementService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.Supplier;

final class CatalogPageController {

    private final View view;
    private final Supplier<UserSession> sessionSupplier;
    private final Runnable refreshAll;
    private final ProductManagementService productManagementService = new ProductManagementService();
    private final CatalogLookupManagementService catalogLookupManagementService = new CatalogLookupManagementService();

    CatalogPageController(View view, Supplier<UserSession> sessionSupplier, Runnable refreshAll) {
        this.view = view;
        this.sessionSupplier = sessionSupplier;
        this.refreshAll = refreshAll;
    }

    void initialize() {
        configureProductTable();
        configureCategoryTable();
        configureManufacturerTable();
    }

    void handleProductSearch() {
        refreshProductsOnly();
    }

    void handleClearProductSearch() {
        view.productSearchField.clear();
        refreshProductsOnly();
    }

    void handleCategorySearch() {
        refreshCategoriesOnly();
    }

    void handleClearCategorySearch() {
        view.categorySearchField.clear();
        refreshCategoriesOnly();
    }

    void handleManufacturerSearch() {
        refreshManufacturersOnly();
    }

    void handleClearManufacturerSearch() {
        view.manufacturerSearchField.clear();
        refreshManufacturersOnly();
    }

    void handleCreateProduct() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        ProductChangeResult result = productManagementService.createProduct(
                session,
                view.productCodeField.getText(),
                view.productModelField.getText(),
                view.productManufacturerSelector.getSelectionModel().getSelectedItem(),
                view.productCategorySelector.getSelectionModel().getSelectedItem(),
                view.productUnitField.getText(),
                view.productDescriptionField.getText()
        );

        if (result.success()) {
            refreshProductsOnly();
            clearProductForm();
        }
        view.productStatusLabel.setText(result.message());
    }

    void handleUpdateProduct() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        ProductRecord selectedProduct = view.productTable.getSelectionModel().getSelectedItem();
        String originalCode = selectedProduct == null ? null : selectedProduct.productCode();

        ProductChangeResult result = productManagementService.updateProduct(
                session,
                originalCode,
                view.productCodeField.getText(),
                view.productModelField.getText(),
                view.productManufacturerSelector.getSelectionModel().getSelectedItem(),
                view.productCategorySelector.getSelectionModel().getSelectedItem(),
                view.productUnitField.getText(),
                view.productDescriptionField.getText()
        );

        if (result.success()) {
            refreshProductsOnly();
        }
        view.productStatusLabel.setText(result.message());
    }

    void handleDeleteProduct() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        ProductRecord selectedProduct = view.productTable.getSelectionModel().getSelectedItem();
        ProductChangeResult result = productManagementService.deleteProduct(session, selectedProduct);

        if (result.success()) {
            refreshProductsOnly();
            clearProductForm();
        }
        view.productStatusLabel.setText(result.message());
    }

    void handleForceDeleteProduct() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        ProductRecord selectedProduct = view.productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            view.productStatusLabel.setText("Select a product row to force delete.");
            return;
        }
        if (!UiDialogs.confirmForceDelete(
                "Force Delete Product",
                "Force delete product " + selectedProduct.productCode() + "?",
                "This also deletes related inventory rows, request lines, and stock movement history."
        )) {
            view.productStatusLabel.setText("Force delete cancelled.");
            return;
        }

        ProductChangeResult result = productManagementService.forceDeleteProduct(session, selectedProduct);

        if (result.success()) {
            clearProductForm();
            refreshAll.run();
        }
        view.productStatusLabel.setText(result.message());
    }

    void handleClearProductForm() {
        clearProductForm();
        view.productTable.getSelectionModel().clearSelection();
        view.productStatusLabel.setText("Product form cleared.");
    }

    void handleCreateCategory() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.createCategory(session, view.categoryNameField.getText());
        if (result.success()) {
            clearCategoryForm();
            refreshAll.run();
        }
        view.categoryStatusLabel.setText(result.message());
    }

    void handleUpdateCategory() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        CatalogLookupRecord selectedCategory = view.categoryTable.getSelectionModel().getSelectedItem();
        String originalName = selectedCategory == null ? null : selectedCategory.name();
        CatalogLookupChangeResult result = catalogLookupManagementService.updateCategory(
                session,
                originalName,
                view.categoryNameField.getText()
        );

        if (result.success()) {
            refreshAll.run();
        }
        view.categoryStatusLabel.setText(result.message());
    }

    void handleDeleteCategory() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.deleteCategory(
                session,
                view.categoryTable.getSelectionModel().getSelectedItem()
        );

        if (result.success()) {
            clearCategoryForm();
            refreshAll.run();
        }
        view.categoryStatusLabel.setText(result.message());
    }

    void handleForceDeleteCategory() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        CatalogLookupRecord selectedCategory = view.categoryTable.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            view.categoryStatusLabel.setText("Select a category row to force delete.");
            return;
        }
        if (!UiDialogs.confirmForceDelete(
                "Force Delete Category",
                "Force delete category " + selectedCategory.name() + "?",
                "This also deletes every product in the category, plus related inventory, request lines, and stock movement history."
        )) {
            view.categoryStatusLabel.setText("Force delete cancelled.");
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.forceDeleteCategory(session, selectedCategory);

        if (result.success()) {
            clearCategoryForm();
            refreshAll.run();
        }
        view.categoryStatusLabel.setText(result.message());
    }

    void handleClearCategoryForm() {
        clearCategoryForm();
        view.categoryTable.getSelectionModel().clearSelection();
        view.categoryStatusLabel.setText("Category form cleared.");
    }

    void handleCreateManufacturer() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.createManufacturer(session, view.manufacturerNameField.getText());
        if (result.success()) {
            clearManufacturerForm();
            refreshAll.run();
        }
        view.manufacturerStatusLabel.setText(result.message());
    }

    void handleUpdateManufacturer() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        CatalogLookupRecord selectedManufacturer = view.manufacturerTable.getSelectionModel().getSelectedItem();
        String originalName = selectedManufacturer == null ? null : selectedManufacturer.name();
        CatalogLookupChangeResult result = catalogLookupManagementService.updateManufacturer(
                session,
                originalName,
                view.manufacturerNameField.getText()
        );

        if (result.success()) {
            refreshAll.run();
        }
        view.manufacturerStatusLabel.setText(result.message());
    }

    void handleDeleteManufacturer() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.deleteManufacturer(
                session,
                view.manufacturerTable.getSelectionModel().getSelectedItem()
        );

        if (result.success()) {
            clearManufacturerForm();
            refreshAll.run();
        }
        view.manufacturerStatusLabel.setText(result.message());
    }

    void handleForceDeleteManufacturer() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        CatalogLookupRecord selectedManufacturer = view.manufacturerTable.getSelectionModel().getSelectedItem();
        if (selectedManufacturer == null) {
            view.manufacturerStatusLabel.setText("Select a manufacturer row to force delete.");
            return;
        }
        if (!UiDialogs.confirmForceDelete(
                "Force Delete Manufacturer",
                "Force delete manufacturer " + selectedManufacturer.name() + "?",
                "This also deletes every product from the manufacturer, plus related inventory, request lines, and stock movement history."
        )) {
            view.manufacturerStatusLabel.setText("Force delete cancelled.");
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.forceDeleteManufacturer(session, selectedManufacturer);

        if (result.success()) {
            clearManufacturerForm();
            refreshAll.run();
        }
        view.manufacturerStatusLabel.setText(result.message());
    }

    void handleClearManufacturerForm() {
        clearManufacturerForm();
        view.manufacturerTable.getSelectionModel().clearSelection();
        view.manufacturerStatusLabel.setText("Manufacturer form cleared.");
    }

    void refreshProductsOnly() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }
        updateProductControls(session, DatabaseConnection.canConnect());
    }

    void refreshCategoriesOnly() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }
        updateCategoryControls(session, DatabaseConnection.canConnect());
    }

    void refreshManufacturersOnly() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }
        updateManufacturerControls(session, DatabaseConnection.canConnect());
    }

    void updateControls(UserSession session, boolean usingLiveDatabase) {
        updateProductControls(session, usingLiveDatabase);
        updateCategoryControls(session, usingLiveDatabase);
        updateManufacturerControls(session, usingLiveDatabase);
    }

    List<String> loadManufacturers() {
        return productManagementService.loadManufacturers();
    }

    List<String> loadCategories() {
        return productManagementService.loadCategories();
    }

    private void configureProductTable() {
        view.productCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        view.productModelColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().modelName()));
        view.productManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        view.productCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        view.productUnitColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().unitName()));
        view.productActiveColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().active() ? "Yes" : "No"));
        view.productTable.setPlaceholder(new Label("No products match the current search."));
        view.productTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateProductForm(newValue));
    }

    private void configureCategoryTable() {
        view.categoryNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name()));
        view.categoryTable.setPlaceholder(new Label("No categories match the current search."));
        view.categoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateCategoryForm(newValue));
    }

    private void configureManufacturerTable() {
        view.manufacturerNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name()));
        view.manufacturerTable.setPlaceholder(new Label("No manufacturers match the current search."));
        view.manufacturerTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateManufacturerForm(newValue));
    }

    private void updateProductControls(UserSession session, boolean usingLiveDatabase) {
        ProductRecord selectedProduct = view.productTable.getSelectionModel().getSelectedItem();
        String selectedProductCode = selectedProduct == null ? null : selectedProduct.productCode();
        String selectedManufacturer = view.productManufacturerSelector.getSelectionModel().getSelectedItem();
        String selectedCategory = view.productCategorySelector.getSelectionModel().getSelectedItem();

        List<ProductRecord> products = productManagementService.loadProducts(view.productSearchField.getText());
        List<String> manufacturers = productManagementService.loadManufacturers();
        List<String> categories = productManagementService.loadCategories();

        view.productTable.getItems().setAll(products);
        view.productManufacturerSelector.setItems(FXCollections.observableArrayList(manufacturers));
        view.productCategorySelector.setItems(FXCollections.observableArrayList(categories));

        if (selectedManufacturer != null && manufacturers.contains(selectedManufacturer)) {
            view.productManufacturerSelector.getSelectionModel().select(selectedManufacturer);
        } else if (!manufacturers.isEmpty() && view.productManufacturerSelector.getSelectionModel().isEmpty()) {
            view.productManufacturerSelector.getSelectionModel().selectFirst();
        }

        if (selectedCategory != null && categories.contains(selectedCategory)) {
            view.productCategorySelector.getSelectionModel().select(selectedCategory);
        } else if (!categories.isEmpty() && view.productCategorySelector.getSelectionModel().isEmpty()) {
            view.productCategorySelector.getSelectionModel().selectFirst();
        }

        if (selectedProductCode != null) {
            products.stream()
                    .filter(product -> product.productCode().equals(selectedProductCode))
                    .findFirst()
                    .ifPresent(product -> view.productTable.getSelectionModel().select(product));
        }

        boolean canManageProducts = session.role().name().equals("ADMIN") && usingLiveDatabase;
        view.productCodeField.setDisable(!canManageProducts);
        view.productModelField.setDisable(!canManageProducts);
        view.productManufacturerSelector.setDisable(!canManageProducts);
        view.productCategorySelector.setDisable(!canManageProducts);
        view.productUnitField.setDisable(!canManageProducts);
        view.productDescriptionField.setDisable(!canManageProducts);
        view.createProductButton.setDisable(!canManageProducts);
        view.updateProductButton.setDisable(!canManageProducts);
        view.deleteProductButton.setDisable(!canManageProducts);
        view.forceDeleteProductButton.setDisable(!canManageProducts);

        view.productSummaryLabel.setText(products.size() + " products shown");
        if (!usingLiveDatabase) {
            view.productStatusLabel.setText("Product edits need a live database connection.");
        } else if (!canManageProducts) {
            view.productStatusLabel.setText("Product catalog is read-only for this session.");
        } else if (view.productStatusLabel.getText() == null || view.productStatusLabel.getText().isBlank()) {
            view.productStatusLabel.setText("Search products or select a row to update it.");
        }
    }

    private void updateCategoryControls(UserSession session, boolean usingLiveDatabase) {
        CatalogLookupRecord selectedCategory = view.categoryTable.getSelectionModel().getSelectedItem();
        String selectedCategoryName = selectedCategory == null ? null : selectedCategory.name();

        List<CatalogLookupRecord> categories = catalogLookupManagementService.loadCategories(view.categorySearchField.getText());
        view.categoryTable.getItems().setAll(categories);

        if (selectedCategoryName != null) {
            categories.stream()
                    .filter(category -> category.name().equals(selectedCategoryName))
                    .findFirst()
                    .ifPresent(category -> view.categoryTable.getSelectionModel().select(category));
        }

        boolean canManageCatalog = session.isAdmin() && usingLiveDatabase;
        view.categoryNameField.setDisable(!canManageCatalog);
        view.createCategoryButton.setDisable(!canManageCatalog);
        view.updateCategoryButton.setDisable(!canManageCatalog);
        view.deleteCategoryButton.setDisable(!canManageCatalog);
        view.forceDeleteCategoryButton.setDisable(!canManageCatalog);

        view.categorySummaryLabel.setText(categories.size() + " categories shown");
        if (!usingLiveDatabase) {
            view.categoryStatusLabel.setText("Category edits need a live database connection.");
        } else if (!canManageCatalog) {
            view.categoryStatusLabel.setText("Categories are read-only for this session.");
        } else if (view.categoryStatusLabel.getText() == null || view.categoryStatusLabel.getText().isBlank()) {
            view.categoryStatusLabel.setText("Search categories or select a row to update it.");
        }
    }

    private void updateManufacturerControls(UserSession session, boolean usingLiveDatabase) {
        CatalogLookupRecord selectedManufacturer = view.manufacturerTable.getSelectionModel().getSelectedItem();
        String selectedManufacturerName = selectedManufacturer == null ? null : selectedManufacturer.name();

        List<CatalogLookupRecord> manufacturers = catalogLookupManagementService.loadManufacturers(view.manufacturerSearchField.getText());
        view.manufacturerTable.getItems().setAll(manufacturers);

        if (selectedManufacturerName != null) {
            manufacturers.stream()
                    .filter(manufacturer -> manufacturer.name().equals(selectedManufacturerName))
                    .findFirst()
                    .ifPresent(manufacturer -> view.manufacturerTable.getSelectionModel().select(manufacturer));
        }

        boolean canManageCatalog = session.isAdmin() && usingLiveDatabase;
        view.manufacturerNameField.setDisable(!canManageCatalog);
        view.createManufacturerButton.setDisable(!canManageCatalog);
        view.updateManufacturerButton.setDisable(!canManageCatalog);
        view.deleteManufacturerButton.setDisable(!canManageCatalog);
        view.forceDeleteManufacturerButton.setDisable(!canManageCatalog);

        view.manufacturerSummaryLabel.setText(manufacturers.size() + " manufacturers shown");
        if (!usingLiveDatabase) {
            view.manufacturerStatusLabel.setText("Manufacturer edits need a live database connection.");
        } else if (!canManageCatalog) {
            view.manufacturerStatusLabel.setText("Manufacturers are read-only for this session.");
        } else if (view.manufacturerStatusLabel.getText() == null || view.manufacturerStatusLabel.getText().isBlank()) {
            view.manufacturerStatusLabel.setText("Search manufacturers or select a row to update it.");
        }
    }

    private void populateProductForm(ProductRecord product) {
        if (product == null) {
            return;
        }

        view.productCodeField.setText(product.productCode());
        view.productModelField.setText(product.modelName());
        view.productManufacturerSelector.getSelectionModel().select(product.manufacturer());
        view.productCategorySelector.getSelectionModel().select(product.category());
        view.productUnitField.setText(product.unitName());
        view.productDescriptionField.setText(product.description());
    }

    private void populateCategoryForm(CatalogLookupRecord category) {
        if (category == null) {
            return;
        }
        view.categoryNameField.setText(category.name());
    }

    private void populateManufacturerForm(CatalogLookupRecord manufacturer) {
        if (manufacturer == null) {
            return;
        }
        view.manufacturerNameField.setText(manufacturer.name());
    }

    private void clearProductForm() {
        view.productCodeField.clear();
        view.productModelField.clear();
        view.productUnitField.setText("pcs");
        view.productDescriptionField.clear();
        if (!view.productManufacturerSelector.getItems().isEmpty()) {
            view.productManufacturerSelector.getSelectionModel().selectFirst();
        }
        if (!view.productCategorySelector.getItems().isEmpty()) {
            view.productCategorySelector.getSelectionModel().selectFirst();
        }
    }

    private void clearCategoryForm() {
        view.categoryNameField.clear();
    }

    private void clearManufacturerForm() {
        view.manufacturerNameField.clear();
    }

    record View(
            TextField productSearchField,
            Label productSummaryLabel,
            Label productStatusLabel,
            TextField productCodeField,
            TextField productModelField,
            ComboBox<String> productManufacturerSelector,
            ComboBox<String> productCategorySelector,
            TextField productUnitField,
            TextField productDescriptionField,
            Button createProductButton,
            Button updateProductButton,
            Button deleteProductButton,
            Button forceDeleteProductButton,
            TableView<ProductRecord> productTable,
            TableColumn<ProductRecord, String> productCodeColumn,
            TableColumn<ProductRecord, String> productModelColumn,
            TableColumn<ProductRecord, String> productManufacturerColumn,
            TableColumn<ProductRecord, String> productCategoryColumn,
            TableColumn<ProductRecord, String> productUnitColumn,
            TableColumn<ProductRecord, String> productActiveColumn,
            TextField categorySearchField,
            Label categorySummaryLabel,
            Label categoryStatusLabel,
            TextField categoryNameField,
            Button createCategoryButton,
            Button updateCategoryButton,
            Button deleteCategoryButton,
            Button forceDeleteCategoryButton,
            TableView<CatalogLookupRecord> categoryTable,
            TableColumn<CatalogLookupRecord, String> categoryNameColumn,
            TextField manufacturerSearchField,
            Label manufacturerSummaryLabel,
            Label manufacturerStatusLabel,
            TextField manufacturerNameField,
            Button createManufacturerButton,
            Button updateManufacturerButton,
            Button deleteManufacturerButton,
            Button forceDeleteManufacturerButton,
            TableView<CatalogLookupRecord> manufacturerTable,
            TableColumn<CatalogLookupRecord, String> manufacturerNameColumn
    ) {
    }
}
