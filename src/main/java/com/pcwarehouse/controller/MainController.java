package com.pcwarehouse.controller;

import com.pcwarehouse.model.CatalogLookupChangeResult;
import com.pcwarehouse.model.CatalogLookupRecord;
import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.DashboardMetric;
import com.pcwarehouse.model.DashboardSnapshot;
import com.pcwarehouse.model.InventoryFilterCriteria;
import com.pcwarehouse.model.InventoryReportRow;
import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.ManagedUserRecord;
import com.pcwarehouse.model.MovementLogEntry;
import com.pcwarehouse.model.MovementType;
import com.pcwarehouse.model.ProductChangeResult;
import com.pcwarehouse.model.ProductOption;
import com.pcwarehouse.model.ProductRecord;
import com.pcwarehouse.model.RequestDetailLine;
import com.pcwarehouse.model.RequestDetailRecord;
import com.pcwarehouse.model.RequestDraftLine;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.RequestActionResult;
import com.pcwarehouse.model.RequestStatus;
import com.pcwarehouse.model.RequestSummary;
import com.pcwarehouse.model.RequestViewMode;
import com.pcwarehouse.model.StockOperationLine;
import com.pcwarehouse.model.StockOperationResult;
import com.pcwarehouse.model.UserChangeResult;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.model.WarehouseAssignmentOption;
import com.pcwarehouse.model.WarehouseChangeResult;
import com.pcwarehouse.model.WarehouseFilterOption;
import com.pcwarehouse.model.WarehouseRecord;
import com.pcwarehouse.service.CatalogLookupManagementService;
import com.pcwarehouse.service.DashboardDataService;
import com.pcwarehouse.service.InventoryInsightsService;
import com.pcwarehouse.service.ProductManagementService;
import com.pcwarehouse.service.RequestActionService;
import com.pcwarehouse.service.RequestDetailService;
import com.pcwarehouse.service.StockOperationService;
import com.pcwarehouse.service.UserManagementService;
import com.pcwarehouse.service.WarehouseManagementService;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainController {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FILTER_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String ACTIVE_LABEL = "Active";
    private static final String INACTIVE_LABEL = "Inactive";
    private static final String ALL_MOVEMENT_TYPES_LABEL = "All types";
    private static final String SORT_BY_WAREHOUSE = "Warehouse";
    private static final String SORT_BY_CODE = "Code";
    private static final String SORT_BY_PRODUCT = "Product";
    private static final String SORT_BY_QUANTITY = "Quantity";

    private final DashboardDataService dashboardDataService = new DashboardDataService();
    private final StockOperationService stockOperationService = new StockOperationService();
    private final RequestActionService requestActionService = new RequestActionService();
    private final RequestDetailService requestDetailService = new RequestDetailService();
    private final InventoryInsightsService inventoryInsightsService = new InventoryInsightsService();
    private final ProductManagementService productManagementService = new ProductManagementService();
    private final CatalogLookupManagementService catalogLookupManagementService = new CatalogLookupManagementService();
    private final WarehouseManagementService warehouseManagementService = new WarehouseManagementService();
    private final UserManagementService userManagementService = new UserManagementService();

    private List<Warehouse> allRequestWarehouses = List.of();
    private List<ProductOption> allSupplyProducts = List.of();
    private List<MovementLogEntry> currentMovementLogEntries = List.of();
    private WarehouseFilterOption currentMovementWarehouseFilter;
    private RequestDetailRecord currentRequestDetail;
    private UserSession currentSession;
    private Runnable logoutHandler;
    private boolean suppressWarehouseFilterRefresh;
    private boolean suppressInventoryFilterRefresh;
    private boolean suppressReportFilterRefresh;
    private boolean suppressSupplyProductSearch;
    private int supplyProductSearchVersion;
    private final ObservableList<RequestDraftLine> requestDraftLines = FXCollections.observableArrayList();
    private final ObservableList<StockOperationLine> supplyDraftLines = FXCollections.observableArrayList();
    private final ObservableList<StockOperationLine> dispatchDraftLines = FXCollections.observableArrayList();
    private AppPage activePage = AppPage.OVERVIEW;
    private RequestViewMode activeRequestViewMode = RequestViewMode.INCOMING;
    private String editingRequestNumber;

    private enum AppPage {
        OVERVIEW("Overview"),
        OPERATIONS("Operations"),
        REQUESTS("Requests"),
        CATALOG("Catalog"),
        REPORTS("Reports"),
        ADMIN("Admin");

        private final String title;

        AppPage(String title) {
            this.title = title;
        }
    }

    @FXML
    private ComboBox<WarehouseFilterOption> warehouseFilterSelector;

    @FXML
    private Label activePageLabel;

    @FXML
    private Label currentUserLabel;

    @FXML
    private Label databaseStatusLabel;

    @FXML
    private Label databaseFailureReasonLabel;

    @FXML
    private Button retryConnectionButton;

    @FXML
    private Label dataSourceLabel;

    @FXML
    private Label refreshStatusLabel;

    @FXML
    private Label stockMetricValueLabel;

    @FXML
    private Label stockMetricNoteLabel;

    @FXML
    private Label requestMetricValueLabel;

    @FXML
    private Label requestMetricNoteLabel;

    @FXML
    private Label operationsMetricValueLabel;

    @FXML
    private Label operationsMetricNoteLabel;

    @FXML
    private Label movementMetricValueLabel;

    @FXML
    private Label movementMetricNoteLabel;

    @FXML
    private TextField productSearchField;

    @FXML
    private Label productSummaryLabel;

    @FXML
    private Label productStatusLabel;

    @FXML
    private TextField productCodeField;

    @FXML
    private TextField productModelField;

    @FXML
    private ComboBox<String> productManufacturerSelector;

    @FXML
    private ComboBox<String> productCategorySelector;

    @FXML
    private TextField productUnitField;

    @FXML
    private TextField productDescriptionField;

    @FXML
    private Button createProductButton;

    @FXML
    private Button updateProductButton;

    @FXML
    private Button deleteProductButton;

    @FXML
    private TableView<ProductRecord> productTable;

    @FXML
    private TableColumn<ProductRecord, String> productCodeColumn;

    @FXML
    private TableColumn<ProductRecord, String> productModelColumn;

    @FXML
    private TableColumn<ProductRecord, String> productManufacturerColumn;

    @FXML
    private TableColumn<ProductRecord, String> productCategoryColumn;

    @FXML
    private TableColumn<ProductRecord, String> productUnitColumn;

    @FXML
    private TableColumn<ProductRecord, String> productActiveColumn;

    @FXML
    private TextField categorySearchField;

    @FXML
    private Label categorySummaryLabel;

    @FXML
    private Label categoryStatusLabel;

    @FXML
    private TextField categoryNameField;

    @FXML
    private Button createCategoryButton;

    @FXML
    private Button updateCategoryButton;

    @FXML
    private Button deleteCategoryButton;

    @FXML
    private TableView<CatalogLookupRecord> categoryTable;

    @FXML
    private TableColumn<CatalogLookupRecord, String> categoryNameColumn;

    @FXML
    private TextField manufacturerSearchField;

    @FXML
    private Label manufacturerSummaryLabel;

    @FXML
    private Label manufacturerStatusLabel;

    @FXML
    private TextField manufacturerNameField;

    @FXML
    private Button createManufacturerButton;

    @FXML
    private Button updateManufacturerButton;

    @FXML
    private Button deleteManufacturerButton;

    @FXML
    private TableView<CatalogLookupRecord> manufacturerTable;

    @FXML
    private TableColumn<CatalogLookupRecord, String> manufacturerNameColumn;

    @FXML
    private TextField warehouseSearchField;

    @FXML
    private Label warehouseSummaryLabel;

    @FXML
    private Label warehouseStatusLabel;

    @FXML
    private TextField warehouseCodeField;

    @FXML
    private TextField warehouseNameField;

    @FXML
    private TextField warehouseCityField;

    @FXML
    private TextField warehouseAddressField;

    @FXML
    private ComboBox<String> warehouseActiveSelector;

    @FXML
    private Button createWarehouseButton;

    @FXML
    private Button updateWarehouseButton;

    @FXML
    private Button deleteWarehouseButton;

    @FXML
    private TableView<WarehouseRecord> warehouseTable;

    @FXML
    private TableColumn<WarehouseRecord, String> warehouseCodeColumn;

    @FXML
    private TableColumn<WarehouseRecord, String> warehouseNameColumn;

    @FXML
    private TableColumn<WarehouseRecord, String> warehouseCityColumn;

    @FXML
    private TableColumn<WarehouseRecord, String> warehouseAddressColumn;

    @FXML
    private TableColumn<WarehouseRecord, String> warehouseActiveColumn;

    @FXML
    private TextField userSearchField;

    @FXML
    private Label userSummaryLabel;

    @FXML
    private Label userStatusLabel;

    @FXML
    private TextField userUsernameField;

    @FXML
    private TextField userPasswordField;

    @FXML
    private TextField userFullNameField;

    @FXML
    private ComboBox<Role> userRoleSelector;

    @FXML
    private ComboBox<WarehouseAssignmentOption> userWarehouseSelector;

    @FXML
    private ComboBox<String> userActiveSelector;

    @FXML
    private Button createUserButton;

    @FXML
    private Button updateUserButton;

    @FXML
    private Button deleteUserButton;

    @FXML
    private TableView<ManagedUserRecord> userTable;

    @FXML
    private TableColumn<ManagedUserRecord, String> userUsernameColumn;

    @FXML
    private TableColumn<ManagedUserRecord, String> userFullNameColumn;

    @FXML
    private TableColumn<ManagedUserRecord, String> userRoleColumn;

    @FXML
    private TableColumn<ManagedUserRecord, String> userWarehouseColumn;

    @FXML
    private TableColumn<ManagedUserRecord, String> userActiveColumn;

    @FXML
    private ComboBox<Warehouse> supplyWarehouseSelector;

    @FXML
    private ComboBox<ProductOption> supplyProductSelector;

    @FXML
    private CheckBox supplyNewProductCheckBox;

    @FXML
    private Node supplyNewProductFields;

    @FXML
    private TextField supplyNewProductCodeField;

    @FXML
    private TextField supplyNewProductNameField;

    @FXML
    private TextField supplyNewProductUnitField;

    @FXML
    private ComboBox<String> supplyNewProductManufacturerSelector;

    @FXML
    private ComboBox<String> supplyNewProductCategorySelector;

    @FXML
    private TextField supplyNewProductDescriptionField;

    @FXML
    private TextField supplyQuantityField;

    @FXML
    private TextField supplySupplierField;

    @FXML
    private TextField supplyNoteField;

    @FXML
    private Button addSupplyLineButton;

    @FXML
    private Button removeSupplyLineButton;

    @FXML
    private Button clearSupplyDraftButton;

    @FXML
    private Button recordSupplyButton;

    @FXML
    private Label supplyStatusLabel;

    @FXML
    private TableView<StockOperationLine> supplyDraftTable;

    @FXML
    private TableColumn<StockOperationLine, String> supplyDraftCodeColumn;

    @FXML
    private TableColumn<StockOperationLine, String> supplyDraftProductColumn;

    @FXML
    private TableColumn<StockOperationLine, String> supplyDraftQuantityColumn;

    @FXML
    private ComboBox<Warehouse> dispatchWarehouseSelector;

    @FXML
    private TextField dispatchInventorySearchField;

    @FXML
    private Label dispatchInventorySummaryLabel;

    @FXML
    private TableView<InventorySummary> dispatchInventoryTable;

    @FXML
    private TableColumn<InventorySummary, String> dispatchInventoryCodeColumn;

    @FXML
    private TableColumn<InventorySummary, String> dispatchInventoryProductColumn;

    @FXML
    private TableColumn<InventorySummary, String> dispatchInventoryManufacturerColumn;

    @FXML
    private TableColumn<InventorySummary, String> dispatchInventoryCategoryColumn;

    @FXML
    private TableColumn<InventorySummary, String> dispatchInventoryQuantityColumn;

    @FXML
    private TextField dispatchQuantityField;

    @FXML
    private TextField dispatchDestinationField;

    @FXML
    private TextField dispatchNoteField;

    @FXML
    private Button addDispatchLineButton;

    @FXML
    private Button removeDispatchLineButton;

    @FXML
    private Button clearDispatchDraftButton;

    @FXML
    private Button recordDispatchButton;

    @FXML
    private Label dispatchStatusLabel;

    @FXML
    private TableView<StockOperationLine> dispatchDraftTable;

    @FXML
    private TableColumn<StockOperationLine, String> dispatchDraftCodeColumn;

    @FXML
    private TableColumn<StockOperationLine, String> dispatchDraftProductColumn;

    @FXML
    private TableColumn<StockOperationLine, String> dispatchDraftQuantityColumn;

    @FXML
    private ComboBox<Warehouse> requestSourceWarehouseSelector;

    @FXML
    private ComboBox<Warehouse> requestTargetWarehouseSelector;

    @FXML
    private TextField requestInventorySearchField;

    @FXML
    private Label requestInventorySummaryLabel;

    @FXML
    private TableView<InventorySummary> requestInventoryTable;

    @FXML
    private TableColumn<InventorySummary, String> requestInventoryCodeColumn;

    @FXML
    private TableColumn<InventorySummary, String> requestInventoryProductColumn;

    @FXML
    private TableColumn<InventorySummary, String> requestInventoryManufacturerColumn;

    @FXML
    private TableColumn<InventorySummary, String> requestInventoryCategoryColumn;

    @FXML
    private TableColumn<InventorySummary, String> requestInventoryQuantityColumn;

    @FXML
    private TextField requestQuantityField;

    @FXML
    private TextField requestNoteField;

    @FXML
    private Label requestEditorTitleLabel;

    @FXML
    private Button createRequestButton;

    @FXML
    private Button saveRequestEditsButton;

    @FXML
    private Button approveRequestButton;

    @FXML
    private Button rejectRequestButton;

    @FXML
    private Button editSelectedRequestButton;

    @FXML
    private Button cancelSelectedRequestButton;

    @FXML
    private Label requestActionStatusLabel;

    @FXML
    private Label inventorySummaryLabel;

    @FXML
    private TextField inventorySearchField;

    @FXML
    private ComboBox<String> inventoryCategoryFilterSelector;

    @FXML
    private ComboBox<String> inventoryManufacturerFilterSelector;

    @FXML
    private Label requestSummaryLabel;

    @FXML
    private Button requestIncomingViewButton;

    @FXML
    private Button requestOutgoingViewButton;

    @FXML
    private Button requestHistoryViewButton;

    @FXML
    private Label movementLogSummaryLabel;

    @FXML
    private ComboBox<String> movementTypeFilterSelector;

    @FXML
    private DatePicker movementFromDatePicker;

    @FXML
    private DatePicker movementToDatePicker;

    @FXML
    private TableView<InventorySummary> inventoryTable;

    @FXML
    private TableColumn<InventorySummary, String> inventoryWarehouseColumn;

    @FXML
    private TableColumn<InventorySummary, String> inventoryCodeColumn;

    @FXML
    private TableColumn<InventorySummary, String> inventoryProductColumn;

    @FXML
    private TableColumn<InventorySummary, String> inventoryManufacturerColumn;

    @FXML
    private TableColumn<InventorySummary, String> inventoryCategoryColumn;

    @FXML
    private TableColumn<InventorySummary, String> inventoryQuantityColumn;

    @FXML
    private TableView<RequestDraftLine> requestDraftTable;

    @FXML
    private TableColumn<RequestDraftLine, String> requestDraftCodeColumn;

    @FXML
    private TableColumn<RequestDraftLine, String> requestDraftProductColumn;

    @FXML
    private TableColumn<RequestDraftLine, String> requestDraftQuantityColumn;

    @FXML
    private Button addRequestLineButton;

    @FXML
    private Button removeRequestLineButton;

    @FXML
    private Button clearRequestDraftButton;

    @FXML
    private TableView<RequestSummary> requestTable;

    @FXML
    private TableColumn<RequestSummary, String> requestNumberColumn;

    @FXML
    private TableColumn<RequestSummary, String> requestSourceColumn;

    @FXML
    private TableColumn<RequestSummary, String> requestDestinationColumn;

    @FXML
    private TableColumn<RequestSummary, String> requestRequestedByColumn;

    @FXML
    private TableColumn<RequestSummary, String> requestStatusColumn;

    @FXML
    private TableColumn<RequestSummary, String> requestLineCountColumn;

    @FXML
    private TableColumn<RequestSummary, String> requestTotalQuantityColumn;

    @FXML
    private TableColumn<RequestSummary, String> requestCreatedAtColumn;

    @FXML
    private Label requestDetailNumberLabel;

    @FXML
    private Label requestDetailStatusLabel;

    @FXML
    private Label requestDetailRouteLabel;

    @FXML
    private Label requestDetailUsersLabel;

    @FXML
    private Label requestDetailNoteLabel;

    @FXML
    private Label requestDetailUpdatedAtLabel;

    @FXML
    private TableView<RequestDetailLine> requestDetailLineTable;

    @FXML
    private TableColumn<RequestDetailLine, String> requestDetailCodeColumn;

    @FXML
    private TableColumn<RequestDetailLine, String> requestDetailProductColumn;

    @FXML
    private TableColumn<RequestDetailLine, String> requestDetailRequestedColumn;

    @FXML
    private TableColumn<RequestDetailLine, String> requestDetailApprovedColumn;

    @FXML
    private TableView<MovementLogEntry> movementLogTable;

    @FXML
    private TableColumn<MovementLogEntry, String> timestampColumn;

    @FXML
    private TableColumn<MovementLogEntry, String> movementTypeColumn;

    @FXML
    private TableColumn<MovementLogEntry, String> productColumn;

    @FXML
    private TableColumn<MovementLogEntry, String> manufacturerColumn;

    @FXML
    private TableColumn<MovementLogEntry, String> quantityColumn;

    @FXML
    private TableColumn<MovementLogEntry, String> sourceWarehouseColumn;

    @FXML
    private TableColumn<MovementLogEntry, String> destinationWarehouseColumn;

    @FXML
    private TableColumn<MovementLogEntry, String> userColumn;

    @FXML
    private TableColumn<MovementLogEntry, String> detailsColumn;

    @FXML
    private Button overviewNavButton;

    @FXML
    private Button operationsNavButton;

    @FXML
    private Button requestsNavButton;

    @FXML
    private Button catalogNavButton;

    @FXML
    private Button reportsNavButton;

    @FXML
    private Button adminNavButton;

    @FXML
    private Node overviewScrollPane;

    @FXML
    private Node operationsScrollPane;

    @FXML
    private Node requestsScrollPane;

    @FXML
    private Node catalogScrollPane;

    @FXML
    private Node reportsScrollPane;

    @FXML
    private Node adminScrollPane;

    @FXML
    private Node overviewPage;

    @FXML
    private Node operationsPage;

    @FXML
    private Node requestsPage;

    @FXML
    private Node catalogPage;

    @FXML
    private Node reportsPage;

    @FXML
    private Node adminPage;

    @FXML
    private ComboBox<Warehouse> reportWarehouseSelector;

    @FXML
    private ComboBox<String> reportCategorySelector;

    @FXML
    private ComboBox<String> reportManufacturerSelector;

    @FXML
    private ComboBox<String> reportSortSelector;

    @FXML
    private Label reportSummaryLabel;

    @FXML
    private TableView<InventoryReportRow> reportTable;

    @FXML
    private TableColumn<InventoryReportRow, String> reportWarehouseColumn;

    @FXML
    private TableColumn<InventoryReportRow, String> reportCodeColumn;

    @FXML
    private TableColumn<InventoryReportRow, String> reportProductColumn;

    @FXML
    private TableColumn<InventoryReportRow, String> reportManufacturerColumn;

    @FXML
    private TableColumn<InventoryReportRow, String> reportCategoryColumn;

    @FXML
    private TableColumn<InventoryReportRow, String> reportQuantityColumn;

    public void setSession(UserSession session) {
        this.currentSession = session;
        if (currentUserLabel != null) {
            currentUserLabel.setText(session.fullName() + " | " + session.role().displayName() + " | " + session.scopeLabel());
        }
        updateNavigationVisibility();
        refreshWarehouseFilterOptions();
        applyDefaultWarehouseFilterForSession(true);
        setActivePage(session.isAdmin() ? AppPage.OVERVIEW : AppPage.OPERATIONS);
        refreshDashboard();
    }

    public void setLogoutHandler(Runnable logoutHandler) {
        this.logoutHandler = logoutHandler;
    }

    @FXML
    private void initialize() {
        configureSelectors();
        configureProductTable();
        configureCategoryTable();
        configureManufacturerTable();
        configureWarehouseTable();
        configureUserTable();
        configureSupplyProductSelector();
        configureSupplyDraftTable();
        configureDispatchInventoryTable();
        configureDispatchDraftTable();
        configureInventoryTable();
        configureRequestInventoryTable();
        configureRequestDraftTable();
        configureRequestTable();
        configureRequestDetailTable();
        configureMovementLogTable();
        configureReportTable();
        configureNavigationButtons();
        refreshStatusLabel.setText("Last refresh: pending | Manual refresh only");
        setActivePage(AppPage.OVERVIEW);
    }

    @FXML
    private void handleManualRefresh() {
        refreshDashboard();
    }

    @FXML
    private void handleRetryConnection() {
        databaseStatusLabel.setText("Retrying database...");
        databaseFailureReasonLabel.setText("Checking " + com.pcwarehouse.db.DatabaseConfig.getUrl());
        refreshDashboard();
    }

    @FXML
    private void handleLogout() {
        if (logoutHandler != null) {
            logoutHandler.run();
        }
    }

    @FXML
    private void handleProductSearch() {
        refreshProductsOnly();
    }

    @FXML
    private void handleClearProductSearch() {
        productSearchField.clear();
        refreshProductsOnly();
    }

    @FXML
    private void handleInventorySearch() {
        refreshInventoryOnly();
    }

    @FXML
    private void handleClearInventoryFilters() {
        inventorySearchField.clear();
        if (!inventoryCategoryFilterSelector.getItems().isEmpty()) {
            inventoryCategoryFilterSelector.getSelectionModel().selectFirst();
        }
        if (!inventoryManufacturerFilterSelector.getItems().isEmpty()) {
            inventoryManufacturerFilterSelector.getSelectionModel().selectFirst();
        }
        refreshInventoryOnly();
    }

    @FXML
    private void handleCategorySearch() {
        refreshCategoriesOnly();
    }

    @FXML
    private void handleClearCategorySearch() {
        categorySearchField.clear();
        refreshCategoriesOnly();
    }

    @FXML
    private void handleManufacturerSearch() {
        refreshManufacturersOnly();
    }

    @FXML
    private void handleClearManufacturerSearch() {
        manufacturerSearchField.clear();
        refreshManufacturersOnly();
    }

    @FXML
    private void handleRefreshReport() {
        refreshReportOnly();
    }

    @FXML
    private void handleClearReportFilters() {
        suppressReportFilterRefresh = true;
        try {
            if (!reportWarehouseSelector.getItems().isEmpty()) {
                reportWarehouseSelector.getSelectionModel().selectFirst();
            }
            if (!reportCategorySelector.getItems().isEmpty()) {
                reportCategorySelector.getSelectionModel().selectFirst();
            }
            if (!reportManufacturerSelector.getItems().isEmpty()) {
                reportManufacturerSelector.getSelectionModel().selectFirst();
            }
            if (!reportSortSelector.getItems().isEmpty()) {
                reportSortSelector.getSelectionModel().select(SORT_BY_WAREHOUSE);
            }
        } finally {
            suppressReportFilterRefresh = false;
        }
        refreshReportOnly();
    }

    @FXML
    private void handleApplyMovementFilters() {
        refreshMovementLogView();
    }

    @FXML
    private void handleClearMovementFilters() {
        if (!movementTypeFilterSelector.getItems().isEmpty()) {
            movementTypeFilterSelector.getSelectionModel().selectFirst();
        }
        movementFromDatePicker.setValue(null);
        movementToDatePicker.setValue(null);
        refreshMovementLogView();
    }

    @FXML
    private void handleWarehouseSearch() {
        refreshWarehousesOnly();
    }

    @FXML
    private void handleClearWarehouseSearch() {
        warehouseSearchField.clear();
        refreshWarehousesOnly();
    }

    @FXML
    private void handleUserSearch() {
        refreshUsersOnly();
    }

    @FXML
    private void handleClearUserSearch() {
        userSearchField.clear();
        refreshUsersOnly();
    }

    @FXML
    private void handleAddRequestLine() {
        InventorySummary selectedInventory = requestInventoryTable.getSelectionModel().getSelectedItem();
        int quantity = parseQuantity(requestQuantityField.getText());
        if (selectedInventory == null) {
            requestActionStatusLabel.setText("Select an item from the target warehouse inventory before adding a request line.");
            return;
        }
        if (quantity <= 0) {
            requestActionStatusLabel.setText("Enter a positive quantity before adding a request line.");
            return;
        }
        int remainingQuantity = getRemainingAvailableQuantity(selectedInventory);
        if (quantity > remainingQuantity) {
            requestActionStatusLabel.setText("Only " + remainingQuantity + " additional unit(s) are available for "
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

        requestQuantityField.clear();
        requestQuantityField.setPromptText("Max " + getRemainingAvailableQuantity(selectedInventory));
        requestInventoryTable.refresh();
        updateRequestDraftButtons();
        requestActionStatusLabel.setText("Added " + selectedInventory.productName() + " to the request draft.");
    }

    @FXML
    private void handleRemoveRequestLine() {
        RequestDraftLine selectedLine = requestDraftTable.getSelectionModel().getSelectedItem();
        if (selectedLine == null) {
            requestActionStatusLabel.setText("Select a request draft line to remove.");
            return;
        }

        requestDraftLines.remove(selectedLine);
        requestInventoryTable.refresh();
        updateRequestDraftButtons();
        requestActionStatusLabel.setText("Removed " + selectedLine.productName() + " from the request draft.");
    }

    @FXML
    private void handleClearRequestDraft() {
        clearRequestDraftState();
        requestInventoryTable.refresh();
        requestActionStatusLabel.setText("Request draft cleared.");
    }

    @FXML
    private void handleShowIncomingRequests() {
        setActiveRequestView(RequestViewMode.INCOMING);
    }

    @FXML
    private void handleShowOutgoingRequests() {
        setActiveRequestView(RequestViewMode.OUTGOING);
    }

    @FXML
    private void handleShowRequestHistory() {
        setActiveRequestView(RequestViewMode.HISTORY);
    }

    @FXML
    private void handleShowOverviewPage() {
        setActivePage(AppPage.OVERVIEW);
    }

    @FXML
    private void handleShowOperationsPage() {
        setActivePage(AppPage.OPERATIONS);
    }

    @FXML
    private void handleShowRequestsPage() {
        setActivePage(AppPage.REQUESTS);
        refreshRequestsOnly();
    }

    @FXML
    private void handleShowCatalogPage() {
        setActivePage(AppPage.CATALOG);
    }

    @FXML
    private void handleShowReportsPage() {
        setActivePage(AppPage.REPORTS);
    }

    @FXML
    private void handleShowAdminPage() {
        setActivePage(AppPage.ADMIN);
    }

    @FXML
    private void handleCreateProduct() {
        if (currentSession == null) {
            return;
        }

        ProductChangeResult result = productManagementService.createProduct(
                currentSession,
                productCodeField.getText(),
                productModelField.getText(),
                productManufacturerSelector.getSelectionModel().getSelectedItem(),
                productCategorySelector.getSelectionModel().getSelectedItem(),
                productUnitField.getText(),
                productDescriptionField.getText()
        );

        if (result.success()) {
            refreshProductsOnly();
            clearProductForm();
        }
        productStatusLabel.setText(result.message());
    }

    @FXML
    private void handleUpdateProduct() {
        if (currentSession == null) {
            return;
        }

        ProductRecord selectedProduct = productTable.getSelectionModel().getSelectedItem();
        String originalCode = selectedProduct == null ? null : selectedProduct.productCode();

        ProductChangeResult result = productManagementService.updateProduct(
                currentSession,
                originalCode,
                productCodeField.getText(),
                productModelField.getText(),
                productManufacturerSelector.getSelectionModel().getSelectedItem(),
                productCategorySelector.getSelectionModel().getSelectedItem(),
                productUnitField.getText(),
                productDescriptionField.getText()
        );

        if (result.success()) {
            refreshProductsOnly();
        }
        productStatusLabel.setText(result.message());
    }

    @FXML
    private void handleDeleteProduct() {
        if (currentSession == null) {
            return;
        }

        ProductRecord selectedProduct = productTable.getSelectionModel().getSelectedItem();
        ProductChangeResult result = productManagementService.deleteProduct(currentSession, selectedProduct);

        if (result.success()) {
            refreshProductsOnly();
            clearProductForm();
        }
        productStatusLabel.setText(result.message());
    }

    @FXML
    private void handleClearProductForm() {
        clearProductForm();
        productTable.getSelectionModel().clearSelection();
        productStatusLabel.setText("Product form cleared.");
    }

    @FXML
    private void handleCreateCategory() {
        if (currentSession == null) {
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.createCategory(currentSession, categoryNameField.getText());
        if (result.success()) {
            clearCategoryForm();
            refreshDashboard();
        }
        categoryStatusLabel.setText(result.message());
    }

    @FXML
    private void handleUpdateCategory() {
        if (currentSession == null) {
            return;
        }

        CatalogLookupRecord selectedCategory = categoryTable.getSelectionModel().getSelectedItem();
        String originalName = selectedCategory == null ? null : selectedCategory.name();
        CatalogLookupChangeResult result = catalogLookupManagementService.updateCategory(
                currentSession,
                originalName,
                categoryNameField.getText()
        );

        if (result.success()) {
            refreshDashboard();
        }
        categoryStatusLabel.setText(result.message());
    }

    @FXML
    private void handleDeleteCategory() {
        if (currentSession == null) {
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.deleteCategory(
                currentSession,
                categoryTable.getSelectionModel().getSelectedItem()
        );

        if (result.success()) {
            clearCategoryForm();
            refreshDashboard();
        }
        categoryStatusLabel.setText(result.message());
    }

    @FXML
    private void handleClearCategoryForm() {
        clearCategoryForm();
        categoryTable.getSelectionModel().clearSelection();
        categoryStatusLabel.setText("Category form cleared.");
    }

    @FXML
    private void handleCreateManufacturer() {
        if (currentSession == null) {
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.createManufacturer(currentSession, manufacturerNameField.getText());
        if (result.success()) {
            clearManufacturerForm();
            refreshDashboard();
        }
        manufacturerStatusLabel.setText(result.message());
    }

    @FXML
    private void handleUpdateManufacturer() {
        if (currentSession == null) {
            return;
        }

        CatalogLookupRecord selectedManufacturer = manufacturerTable.getSelectionModel().getSelectedItem();
        String originalName = selectedManufacturer == null ? null : selectedManufacturer.name();
        CatalogLookupChangeResult result = catalogLookupManagementService.updateManufacturer(
                currentSession,
                originalName,
                manufacturerNameField.getText()
        );

        if (result.success()) {
            refreshDashboard();
        }
        manufacturerStatusLabel.setText(result.message());
    }

    @FXML
    private void handleDeleteManufacturer() {
        if (currentSession == null) {
            return;
        }

        CatalogLookupChangeResult result = catalogLookupManagementService.deleteManufacturer(
                currentSession,
                manufacturerTable.getSelectionModel().getSelectedItem()
        );

        if (result.success()) {
            clearManufacturerForm();
            refreshDashboard();
        }
        manufacturerStatusLabel.setText(result.message());
    }

    @FXML
    private void handleClearManufacturerForm() {
        clearManufacturerForm();
        manufacturerTable.getSelectionModel().clearSelection();
        manufacturerStatusLabel.setText("Manufacturer form cleared.");
    }

    @FXML
    private void handleCreateWarehouse() {
        if (currentSession == null) {
            return;
        }

        WarehouseChangeResult result = warehouseManagementService.createWarehouse(
                currentSession,
                warehouseCodeField.getText(),
                warehouseNameField.getText(),
                warehouseCityField.getText(),
                warehouseAddressField.getText()
        );

        if (result.success()) {
            clearWarehouseForm();
            refreshDashboard();
        }
        warehouseStatusLabel.setText(result.message());
    }

    @FXML
    private void handleUpdateWarehouse() {
        if (currentSession == null) {
            return;
        }

        WarehouseRecord selectedWarehouse = warehouseTable.getSelectionModel().getSelectedItem();
        String originalWarehouseCode = selectedWarehouse == null ? null : selectedWarehouse.warehouseCode();

        WarehouseChangeResult result = warehouseManagementService.updateWarehouse(
                currentSession,
                originalWarehouseCode,
                warehouseCodeField.getText(),
                warehouseNameField.getText(),
                warehouseCityField.getText(),
                warehouseAddressField.getText(),
                isActiveSelection(warehouseActiveSelector, true)
        );

        if (result.success()) {
            refreshDashboard();
        }
        warehouseStatusLabel.setText(result.message());
    }

    @FXML
    private void handleDeleteWarehouse() {
        if (currentSession == null) {
            return;
        }

        WarehouseChangeResult result = warehouseManagementService.deleteWarehouse(
                currentSession,
                warehouseTable.getSelectionModel().getSelectedItem()
        );

        if (result.success()) {
            clearWarehouseForm();
            refreshDashboard();
        }
        warehouseStatusLabel.setText(result.message());
    }

    @FXML
    private void handleClearWarehouseForm() {
        clearWarehouseForm();
        warehouseTable.getSelectionModel().clearSelection();
        warehouseStatusLabel.setText("Warehouse form cleared.");
    }

    @FXML
    private void handleCreateUser() {
        if (currentSession == null) {
            return;
        }

        UserChangeResult result = userManagementService.createUser(
                currentSession,
                userUsernameField.getText(),
                userPasswordField.getText(),
                userFullNameField.getText(),
                userRoleSelector.getSelectionModel().getSelectedItem(),
                userWarehouseSelector.getSelectionModel().getSelectedItem(),
                isActiveSelection(userActiveSelector, true)
        );

        if (result.success()) {
            clearUserForm();
            refreshDashboard();
        }
        userStatusLabel.setText(result.message());
    }

    @FXML
    private void handleUpdateUser() {
        if (currentSession == null) {
            return;
        }

        ManagedUserRecord selectedUser = userTable.getSelectionModel().getSelectedItem();
        String originalUsername = selectedUser == null ? null : selectedUser.username();

        UserChangeResult result = userManagementService.updateUser(
                currentSession,
                originalUsername,
                userUsernameField.getText(),
                userPasswordField.getText(),
                userFullNameField.getText(),
                userRoleSelector.getSelectionModel().getSelectedItem(),
                userWarehouseSelector.getSelectionModel().getSelectedItem(),
                isActiveSelection(userActiveSelector, true)
        );

        if (result.success()) {
            refreshDashboard();
        }
        userStatusLabel.setText(result.message());
    }

    @FXML
    private void handleDeleteUser() {
        if (currentSession == null) {
            return;
        }

        UserChangeResult result = userManagementService.deleteUser(
                currentSession,
                userTable.getSelectionModel().getSelectedItem()
        );

        if (result.success()) {
            clearUserForm();
            refreshDashboard();
        }
        userStatusLabel.setText(result.message());
    }

    @FXML
    private void handleClearUserForm() {
        clearUserForm();
        userTable.getSelectionModel().clearSelection();
        userStatusLabel.setText("User form cleared.");
    }

    @FXML
    private void handleToggleNewSupplyProduct() {
        refreshSupplyProductMode();
    }

    @FXML
    private void handleAddSupplyLine() {
        int quantity = parseQuantity(supplyQuantityField.getText());
        if (quantity <= 0) {
            supplyStatusLabel.setText("Enter a positive supply quantity.");
            return;
        }

        StockOperationLine line;
        if (supplyNewProductCheckBox.isSelected()) {
            line = new StockOperationLine(
                    true,
                    supplyNewProductCodeField.getText().trim(),
                    supplyNewProductNameField.getText().trim(),
                    supplyNewProductManufacturerSelector.getSelectionModel().getSelectedItem(),
                    supplyNewProductCategorySelector.getSelectionModel().getSelectedItem(),
                    supplyNewProductUnitField.getText().isBlank() ? "pcs" : supplyNewProductUnitField.getText().trim(),
                    supplyNewProductDescriptionField.getText(),
                    quantity
            );
            if (line.productCode().isBlank() || line.productName().isBlank()
                    || line.manufacturer() == null || line.category() == null) {
                supplyStatusLabel.setText("New products need code, name, manufacturer, and category.");
                return;
            }
        } else {
            ProductOption product = getSelectedSupplyProduct();
            if (product == null) {
                supplyStatusLabel.setText("Select an existing product, or check New product to catalog.");
                return;
            }
            line = StockOperationLine.existingProduct(product, quantity);
        }

        upsertStockLine(supplyDraftLines, line);
        supplyQuantityField.clear();
        updateStockOperationButtons(DatabaseConnection.canConnect());
        supplyStatusLabel.setText("Added " + line.productName() + " to supply draft.");
    }

    @FXML
    private void handleRemoveSupplyLine() {
        StockOperationLine selectedLine = supplyDraftTable.getSelectionModel().getSelectedItem();
        if (selectedLine == null) {
            supplyStatusLabel.setText("Select a supply line to remove.");
            return;
        }
        supplyDraftLines.remove(selectedLine);
        updateStockOperationButtons(DatabaseConnection.canConnect());
        supplyStatusLabel.setText("Removed " + selectedLine.productName() + ".");
    }

    @FXML
    private void handleClearSupplyDraft() {
        supplyDraftLines.clear();
        supplyDraftTable.getSelectionModel().clearSelection();
        updateStockOperationButtons(DatabaseConnection.canConnect());
        supplyStatusLabel.setText("Supply draft cleared.");
    }

    @FXML
    private void handleRecordSupply() {
        StockOperationResult result = stockOperationService.supplyLines(
                currentSession,
                supplyWarehouseSelector.getSelectionModel().getSelectedItem(),
                new ArrayList<>(supplyDraftLines),
                readOptionalField(supplySupplierField),
                buildSupplyDetails()
        );

        if (result.success()) {
            supplyDraftLines.clear();
            supplyQuantityField.clear();
            refreshDashboard();
        }
        supplyStatusLabel.setText(result.message());
    }

    @FXML
    private void handleAddDispatchLine() {
        InventorySummary selectedInventory = dispatchInventoryTable.getSelectionModel().getSelectedItem();
        int quantity = parseQuantity(dispatchQuantityField.getText());
        if (selectedInventory == null) {
            dispatchStatusLabel.setText("Select an inventory item to dispatch.");
            return;
        }
        if (quantity <= 0) {
            dispatchStatusLabel.setText("Enter a positive dispatch quantity.");
            return;
        }
        int remainingQuantity = getRemainingDispatchQuantity(selectedInventory);
        if (quantity > remainingQuantity) {
            dispatchStatusLabel.setText("Only " + remainingQuantity + " additional unit(s) are available for "
                    + selectedInventory.productName() + ".");
            return;
        }

        StockOperationLine line = StockOperationLine.inventoryProduct(selectedInventory, quantity);
        upsertStockLine(dispatchDraftLines, line);
        dispatchQuantityField.clear();
        dispatchQuantityField.setPromptText("Max " + getRemainingDispatchQuantity(selectedInventory));
        updateStockOperationButtons(DatabaseConnection.canConnect());
        dispatchStatusLabel.setText("Added " + line.productName() + " to dispatch draft.");
    }

    @FXML
    private void handleRemoveDispatchLine() {
        StockOperationLine selectedLine = dispatchDraftTable.getSelectionModel().getSelectedItem();
        if (selectedLine == null) {
            dispatchStatusLabel.setText("Select a dispatch line to remove.");
            return;
        }
        dispatchDraftLines.remove(selectedLine);
        updateStockOperationButtons(DatabaseConnection.canConnect());
        dispatchStatusLabel.setText("Removed " + selectedLine.productName() + ".");
    }

    @FXML
    private void handleClearDispatchDraft() {
        dispatchDraftLines.clear();
        dispatchDraftTable.getSelectionModel().clearSelection();
        updateStockOperationButtons(DatabaseConnection.canConnect());
        dispatchStatusLabel.setText("Dispatch draft cleared.");
    }

    @FXML
    private void handleRecordDispatch() {
        StockOperationResult result = stockOperationService.dispatchLines(
                currentSession,
                dispatchWarehouseSelector.getSelectionModel().getSelectedItem(),
                new ArrayList<>(dispatchDraftLines),
                readOptionalField(dispatchDestinationField),
                buildDispatchDetails()
        );

        if (result.success()) {
            dispatchDraftLines.clear();
            dispatchQuantityField.clear();
            refreshDashboard();
        }
        dispatchStatusLabel.setText(result.message());
    }

    @FXML
    private void handleCreateRequest() {
        if (currentSession == null) {
            return;
        }
        if (editingRequestNumber != null) {
            requestActionStatusLabel.setText("Finish or clear the current edit before creating a new request.");
            return;
        }

        Warehouse sourceWarehouse = requestSourceWarehouseSelector.getSelectionModel().getSelectedItem();
        Warehouse targetWarehouse = requestTargetWarehouseSelector.getSelectionModel().getSelectedItem();
        if (requestDraftLines.isEmpty()) {
            requestActionStatusLabel.setText("Add at least one line to the request draft before submitting.");
            return;
        }

        RequestActionResult result = requestActionService.createRequest(
                currentSession,
                sourceWarehouse,
                targetWarehouse,
                new ArrayList<>(requestDraftLines),
                requestNoteField.getText()
        );

        if (result.success()) {
            clearRequestDraftState();
            refreshDashboard();
            requestActionStatusLabel.setText(result.message());
            return;
        }

        requestActionStatusLabel.setText(result.message());
    }

    @FXML
    private void handleApproveRequest() {
        executeRequestAction(true);
    }

    @FXML
    private void handleRejectRequest() {
        executeRequestAction(false);
    }

    @FXML
    private void handleEditSelectedRequest() {
        if (currentSession == null) {
            return;
        }

        RequestSummary selectedRequest = requestTable.getSelectionModel().getSelectedItem();
        if (!canEditSelectedPendingRequest(selectedRequest)) {
            requestActionStatusLabel.setText("Only pending requests created by this manager or admin can be edited.");
            return;
        }

        RequestDetailRecord detail = requestDetailService.load(currentSession, selectedRequest.requestNumber());
        if (detail == null) {
            requestActionStatusLabel.setText("Could not load the selected request details.");
            return;
        }

        editingRequestNumber = detail.requestNumber();
        selectWarehouseByCode(requestSourceWarehouseSelector, detail.sourceWarehouseCode());
        refreshRequestTargetOptions();
        selectWarehouseByCode(requestTargetWarehouseSelector, detail.destinationWarehouseCode());
        requestNoteField.setText(detail.note() == null ? "" : detail.note());
        requestQuantityField.clear();
        requestDraftLines.setAll(detail.lines().stream()
                .map(line -> new RequestDraftLine(
                        line.productCode(),
                        line.productName(),
                        line.manufacturer(),
                        line.category(),
                        line.quantityRequested()
                ))
                .toList());
        requestInventoryTable.refresh();
        updateRequestActionButtons(DatabaseConnection.canConnect());
        requestActionStatusLabel.setText("Editing " + detail.requestNumber() + ". Save pending edits when ready.");
    }

    @FXML
    private void handleSaveRequestEdits() {
        if (currentSession == null) {
            return;
        }
        if (editingRequestNumber == null) {
            requestActionStatusLabel.setText("Load a pending outgoing request before saving edits.");
            return;
        }

        RequestActionResult result = requestActionService.updatePendingRequest(
                currentSession,
                editingRequestNumber,
                requestSourceWarehouseSelector.getSelectionModel().getSelectedItem(),
                requestTargetWarehouseSelector.getSelectionModel().getSelectedItem(),
                new ArrayList<>(requestDraftLines),
                requestNoteField.getText()
        );

        if (result.success()) {
            clearRequestDraftState();
            refreshDashboard();
            requestActionStatusLabel.setText(result.message());
            return;
        }

        requestActionStatusLabel.setText(result.message());
    }

    @FXML
    private void handleCancelSelectedRequest() {
        if (currentSession == null) {
            return;
        }

        RequestSummary selectedRequest = requestTable.getSelectionModel().getSelectedItem();
        RequestActionResult result = requestActionService.cancelRequest(currentSession, selectedRequest);

        if (result.success()) {
            if (editingRequestNumber != null && selectedRequest != null
                    && editingRequestNumber.equals(selectedRequest.requestNumber())) {
                clearRequestDraftState();
            }
            refreshDashboard();
            requestActionStatusLabel.setText(result.message());
            return;
        }

        requestActionStatusLabel.setText(result.message());
    }

    private void configureSelectors() {
        refreshWarehouseFilterOptions();
        warehouseFilterSelector.setOnAction(event -> {
            if (!suppressWarehouseFilterRefresh && currentSession != null) {
                refreshDashboard();
            }
        });

        warehouseActiveSelector.setItems(FXCollections.observableArrayList(ACTIVE_LABEL, INACTIVE_LABEL));
        warehouseActiveSelector.getSelectionModel().select(ACTIVE_LABEL);

        userRoleSelector.setItems(FXCollections.observableArrayList(Role.values()));
        userRoleSelector.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        });
        userRoleSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        });
        userRoleSelector.getSelectionModel().select(Role.WAREHOUSE_MANAGER);
        userRoleSelector.setOnAction(event -> refreshUserWarehouseSelectorState());

        userWarehouseSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(WarehouseAssignmentOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public WarehouseAssignmentOption fromString(String string) {
                return null;
            }
        });

        userActiveSelector.setItems(FXCollections.observableArrayList(ACTIVE_LABEL, INACTIVE_LABEL));
        userActiveSelector.getSelectionModel().select(ACTIVE_LABEL);

        inventoryCategoryFilterSelector.setOnAction(event -> {
            if (!suppressInventoryFilterRefresh) {
                refreshInventoryOnly();
            }
        });
        inventoryManufacturerFilterSelector.setOnAction(event -> {
            if (!suppressInventoryFilterRefresh) {
                refreshInventoryOnly();
            }
        });
        reportWarehouseSelector.setOnAction(event -> {
            if (!suppressReportFilterRefresh) {
                refreshReportOnly();
            }
        });
        reportCategorySelector.setOnAction(event -> {
            if (!suppressReportFilterRefresh) {
                refreshReportOnly();
            }
        });
        reportManufacturerSelector.setOnAction(event -> {
            if (!suppressReportFilterRefresh) {
                refreshReportOnly();
            }
        });
        reportSortSelector.setItems(FXCollections.observableArrayList(
                SORT_BY_WAREHOUSE,
                SORT_BY_CODE,
                SORT_BY_PRODUCT,
                SORT_BY_QUANTITY
        ));
        reportSortSelector.getSelectionModel().select(SORT_BY_WAREHOUSE);
        reportSortSelector.setOnAction(event -> {
            if (!suppressReportFilterRefresh) {
                refreshReportOnly();
            }
        });

        List<String> movementTypes = new ArrayList<>();
        movementTypes.add(ALL_MOVEMENT_TYPES_LABEL);
        for (MovementType movementType : MovementType.values()) {
            movementTypes.add(movementType.displayName());
        }
        movementTypeFilterSelector.setItems(FXCollections.observableArrayList(movementTypes));
        movementTypeFilterSelector.getSelectionModel().selectFirst();
        movementTypeFilterSelector.setOnAction(event -> refreshMovementLogView());
        configureDatePicker(movementFromDatePicker);
        configureDatePicker(movementToDatePicker);
        movementFromDatePicker.setOnAction(event -> refreshMovementLogView());
        movementToDatePicker.setOnAction(event -> refreshMovementLogView());

        supplyWarehouseSelector.setOnAction(event -> refreshDispatchInventory());
        dispatchWarehouseSelector.setOnAction(event -> refreshDispatchInventory());
        dispatchInventorySearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshDispatchInventory());
        requestSourceWarehouseSelector.setOnAction(event -> {
            refreshRequestTargetOptions();
            refreshRequestTargetInventory();
        });
        requestTargetWarehouseSelector.setOnAction(event -> refreshRequestTargetInventory());
        requestInventorySearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshRequestTargetInventory());
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

    private void configureProductTable() {
        productCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        productModelColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().modelName()));
        productManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        productCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        productUnitColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().unitName()));
        productActiveColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().active() ? "Yes" : "No"));
        productTable.setPlaceholder(new Label("No products match the current search."));
        productTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateProductForm(newValue));
    }

    private void configureSupplyProductSelector() {
        supplyProductSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductOption product) {
                return product == null ? "" : product.toString();
            }

            @Override
            public ProductOption fromString(String value) {
                return findSupplyProductByExactText(value);
            }
        });
        supplyProductSelector.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!suppressSupplyProductSearch && !supplyNewProductCheckBox.isSelected()) {
                ProductOption selectedProduct = supplyProductSelector.getValue();
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
        supplyDraftCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        supplyDraftProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productLabel()));
        supplyDraftQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        supplyDraftTable.setItems(supplyDraftLines);
        supplyDraftTable.setPlaceholder(new Label("Add supply lines above before recording stock."));
    }

    private void configureDispatchInventoryTable() {
        dispatchInventoryCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        dispatchInventoryProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        dispatchInventoryManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        dispatchInventoryCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        dispatchInventoryQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        dispatchInventoryTable.setPlaceholder(new Label("Search current warehouse inventory, then select a product to dispatch."));
        dispatchInventoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedItem) -> {
            dispatchQuantityField.setPromptText(selectedItem == null ? "Qty" : "Max " + getRemainingDispatchQuantity(selectedItem));
            updateStockOperationButtons(DatabaseConnection.canConnect());
        });
    }

    private void configureDispatchDraftTable() {
        dispatchDraftCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        dispatchDraftProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productLabel()));
        dispatchDraftQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        dispatchDraftTable.setItems(dispatchDraftLines);
        dispatchDraftTable.setPlaceholder(new Label("Add dispatch lines above before recording stock."));
    }

    private void configureCategoryTable() {
        categoryNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name()));
        categoryTable.setPlaceholder(new Label("No categories match the current search."));
        categoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateCategoryForm(newValue));
    }

    private void configureManufacturerTable() {
        manufacturerNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name()));
        manufacturerTable.setPlaceholder(new Label("No manufacturers match the current search."));
        manufacturerTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateManufacturerForm(newValue));
    }

    private void configureInventoryTable() {
        inventoryWarehouseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseLabel()));
        inventoryCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        inventoryProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        inventoryManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        inventoryCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        inventoryQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        inventoryTable.setPlaceholder(new Label("No inventory rows are available for this session."));
    }

    private void configureRequestInventoryTable() {
        requestInventoryCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        requestInventoryProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        requestInventoryManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        requestInventoryCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        requestInventoryQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        requestInventoryQuantityColumn.setCellFactory(column -> new TableCell<>() {
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
        requestInventoryTable.setPlaceholder(new Label("Select a target warehouse, then search by code or product name."));
        requestInventoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedItem) -> {
            requestQuantityField.setPromptText(selectedItem == null ? "Qty" : "Max " + getRemainingAvailableQuantity(selectedItem));
            updateRequestDraftButtons();
        });
    }

    private void configureRequestDraftTable() {
        requestDraftCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        requestDraftProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        requestDraftQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        requestDraftTable.setItems(requestDraftLines);
        requestDraftTable.setPlaceholder(new Label("Add products above to build a multi-line warehouse request."));
    }

    private void configureWarehouseTable() {
        warehouseCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseCode()));
        warehouseNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseName()));
        warehouseCityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().city()));
        warehouseAddressColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().addressLine()));
        warehouseActiveColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().active() ? "Yes" : "No"));
        warehouseTable.setPlaceholder(new Label("No warehouses match the current search."));
        warehouseTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateWarehouseForm(newValue));
    }

    private void configureUserTable() {
        userUsernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().username()));
        userFullNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().fullName()));
        userRoleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().role().displayName()));
        userWarehouseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseLabel()));
        userActiveColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().active() ? "Yes" : "No"));
        userTable.setPlaceholder(new Label("No users match the current search."));
        userTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateUserForm(newValue));
    }

    private void configureRequestTable() {
        requestNumberColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().requestNumber()));
        requestSourceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().sourceWarehouseLabel()));
        requestDestinationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().destinationWarehouseLabel()));
        requestRequestedByColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().requestedBy()));
        requestStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().status().displayName()));
        requestLineCountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().lineCount())));
        requestTotalQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().totalQuantity())));
        requestCreatedAtColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().createdAt().format(TIMESTAMP_FORMATTER)));
        requestTable.setPlaceholder(new Label("No warehouse requests are available for this view."));
        requestTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            refreshSelectedRequestDetail();
            updateRequestActionButtons(DatabaseConnection.canConnect());
        });
    }

    private void configureRequestDetailTable() {
        requestDetailCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        requestDetailProductColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().productName() + " | " + cellData.getValue().manufacturer() + " | " + cellData.getValue().category()));
        requestDetailRequestedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantityRequested())));
        requestDetailApprovedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().quantityApproved() == null ? "-" : Integer.toString(cellData.getValue().quantityApproved())));
        requestDetailLineTable.setPlaceholder(new Label("Select a request to see its requested items."));
        requestDetailLineTable.setRowFactory(tableView -> new TableRow<>() {
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
        timestampColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().timestamp().format(TIMESTAMP_FORMATTER)));
        movementTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().type().displayName()));
        productColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().product()));
        manufacturerColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().manufacturer()));
        quantityColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        sourceWarehouseColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().sourceWarehouseLabel()));
        destinationWarehouseColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().destinationWarehouseLabel()));
        userColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().performedBy()));
        detailsColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().details()));
        movementLogTable.setPlaceholder(new Label("No movement logs match the selected filter."));
    }

    private void configureReportTable() {
        reportWarehouseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseLabel()));
        reportCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productCode()));
        reportProductColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        reportManufacturerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().manufacturer()));
        reportCategoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        reportQuantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().quantity())));
        reportTable.setPlaceholder(new Label("No rows match the current report filters."));
    }

    private void configureNavigationButtons() {
        Button[] navButtons = {
                overviewNavButton,
                operationsNavButton,
                requestsNavButton,
                catalogNavButton,
                reportsNavButton,
                adminNavButton
        };

        for (Button button : navButtons) {
            if (button == null) {
                continue;
            }
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMinHeight(42.0);
            button.setPrefHeight(42.0);
        }
    }

    private void refreshDashboard() {
        if (currentSession == null) {
            return;
        }

        refreshWarehouseFilterOptions();
        applyDefaultWarehouseFilterForSession(false);
        WarehouseFilterOption filterOption = getSelectedWarehouseFilter();
        DashboardSnapshot snapshot = dashboardDataService.load(currentSession, filterOption);

        updateDatabaseStatus(snapshot);
        dataSourceLabel.setText(snapshot.usingLiveDatabase() ? "Data source: PostgreSQL live" : "Data source: unavailable");
        updateMetrics(snapshot.metrics());
        updateProductControls(currentSession, snapshot.usingLiveDatabase());
        updateCategoryControls(currentSession, snapshot.usingLiveDatabase());
        updateManufacturerControls(currentSession, snapshot.usingLiveDatabase());
        updateWarehouseControls(currentSession, snapshot.usingLiveDatabase());
        updateUserControls(currentSession, snapshot.usingLiveDatabase());
        updateStockOperationControls(currentSession, snapshot.usingLiveDatabase());
        updateRequestControls(currentSession, snapshot.usingLiveDatabase());
        updateInventoryFilters();
        updateInventoryView();
        refreshRequestsOnly();
        updateMovementLogs(snapshot.movementLogEntries(), filterOption);
        updateReportControls();
        updateReportView();

        refreshStatusLabel.setText("Last refresh: " + TIMESTAMP_FORMATTER.format(LocalDateTime.now())
                + " | Manual refresh only");
    }

    private void refreshProductsOnly() {
        if (currentSession == null) {
            return;
        }
        updateProductControls(currentSession, DatabaseConnection.canConnect());
    }

    private void refreshInventoryOnly() {
        if (currentSession == null) {
            return;
        }
        updateInventoryFilters();
        updateInventoryView();
    }

    private void refreshCategoriesOnly() {
        if (currentSession == null) {
            return;
        }
        updateCategoryControls(currentSession, DatabaseConnection.canConnect());
    }

    private void refreshManufacturersOnly() {
        if (currentSession == null) {
            return;
        }
        updateManufacturerControls(currentSession, DatabaseConnection.canConnect());
    }

    private void refreshReportOnly() {
        if (currentSession == null) {
            return;
        }
        updateReportControls();
        updateReportView();
    }

    private void refreshWarehousesOnly() {
        if (currentSession == null) {
            return;
        }
        updateWarehouseControls(currentSession, DatabaseConnection.canConnect());
    }

    private void refreshUsersOnly() {
        if (currentSession == null) {
            return;
        }
        updateUserControls(currentSession, DatabaseConnection.canConnect());
    }

    private void updateDatabaseStatus(DashboardSnapshot snapshot) {
        if (snapshot.usingLiveDatabase()) {
            databaseStatusLabel.setText("Database online");
            databaseFailureReasonLabel.setText("Connected to " + com.pcwarehouse.db.DatabaseConfig.getUrl());
            return;
        }

        databaseStatusLabel.setText("Database offline");
        databaseFailureReasonLabel.setText(snapshot.databaseError() == null || snapshot.databaseError().isBlank()
                ? "Connection failed. Check PostgreSQL, database name, username, password, and application.properties."
                : snapshot.databaseError());
    }

    private void updateMetrics(List<DashboardMetric> metrics) {
        if (metrics.size() < 4) {
            throw new IllegalStateException("Expected four dashboard metrics.");
        }

        applyMetric(stockMetricValueLabel, stockMetricNoteLabel, metrics.get(0));
        applyMetric(requestMetricValueLabel, requestMetricNoteLabel, metrics.get(1));
        applyMetric(operationsMetricValueLabel, operationsMetricNoteLabel, metrics.get(2));
        applyMetric(movementMetricValueLabel, movementMetricNoteLabel, metrics.get(3));
    }

    private void applyMetric(Label valueLabel, Label noteLabel, DashboardMetric metric) {
        valueLabel.setText(metric.value());
        noteLabel.setText(metric.note());
    }

    private void updateProductControls(UserSession session, boolean usingLiveDatabase) {
        ProductRecord selectedProduct = productTable.getSelectionModel().getSelectedItem();
        String selectedProductCode = selectedProduct == null ? null : selectedProduct.productCode();
        String selectedManufacturer = productManufacturerSelector.getSelectionModel().getSelectedItem();
        String selectedCategory = productCategorySelector.getSelectionModel().getSelectedItem();

        List<ProductRecord> products = productManagementService.loadProducts(productSearchField.getText());
        List<String> manufacturers = productManagementService.loadManufacturers();
        List<String> categories = productManagementService.loadCategories();

        productTable.getItems().setAll(products);
        productManufacturerSelector.setItems(FXCollections.observableArrayList(manufacturers));
        productCategorySelector.setItems(FXCollections.observableArrayList(categories));

        if (selectedManufacturer != null && manufacturers.contains(selectedManufacturer)) {
            productManufacturerSelector.getSelectionModel().select(selectedManufacturer);
        } else if (!manufacturers.isEmpty() && productManufacturerSelector.getSelectionModel().isEmpty()) {
            productManufacturerSelector.getSelectionModel().selectFirst();
        }

        if (selectedCategory != null && categories.contains(selectedCategory)) {
            productCategorySelector.getSelectionModel().select(selectedCategory);
        } else if (!categories.isEmpty() && productCategorySelector.getSelectionModel().isEmpty()) {
            productCategorySelector.getSelectionModel().selectFirst();
        }

        if (selectedProductCode != null) {
            products.stream()
                    .filter(product -> product.productCode().equals(selectedProductCode))
                    .findFirst()
                    .ifPresent(product -> productTable.getSelectionModel().select(product));
        }

        boolean canManageProducts = session.role().name().equals("ADMIN") && usingLiveDatabase;
        productCodeField.setDisable(!canManageProducts);
        productModelField.setDisable(!canManageProducts);
        productManufacturerSelector.setDisable(!canManageProducts);
        productCategorySelector.setDisable(!canManageProducts);
        productUnitField.setDisable(!canManageProducts);
        productDescriptionField.setDisable(!canManageProducts);
        createProductButton.setDisable(!canManageProducts);
        updateProductButton.setDisable(!canManageProducts);
        deleteProductButton.setDisable(!canManageProducts);

        productSummaryLabel.setText(products.size() + " products shown");
        if (!usingLiveDatabase) {
            productStatusLabel.setText("Product edits need a live database connection.");
        } else if (!canManageProducts) {
            productStatusLabel.setText("Product catalog is read-only for this session.");
        } else if (productStatusLabel.getText() == null || productStatusLabel.getText().isBlank()) {
            productStatusLabel.setText("Search products or select a row to update it.");
        }
    }

    private void updateCategoryControls(UserSession session, boolean usingLiveDatabase) {
        CatalogLookupRecord selectedCategory = categoryTable.getSelectionModel().getSelectedItem();
        String selectedCategoryName = selectedCategory == null ? null : selectedCategory.name();

        List<CatalogLookupRecord> categories = catalogLookupManagementService.loadCategories(categorySearchField.getText());
        categoryTable.getItems().setAll(categories);

        if (selectedCategoryName != null) {
            categories.stream()
                    .filter(category -> category.name().equals(selectedCategoryName))
                    .findFirst()
                    .ifPresent(category -> categoryTable.getSelectionModel().select(category));
        }

        boolean canManageCatalog = session.isAdmin() && usingLiveDatabase;
        categoryNameField.setDisable(!canManageCatalog);
        createCategoryButton.setDisable(!canManageCatalog);
        updateCategoryButton.setDisable(!canManageCatalog);
        deleteCategoryButton.setDisable(!canManageCatalog);

        categorySummaryLabel.setText(categories.size() + " categories shown");
        if (!usingLiveDatabase) {
            categoryStatusLabel.setText("Category edits need a live database connection.");
        } else if (!canManageCatalog) {
            categoryStatusLabel.setText("Categories are read-only for this session.");
        } else if (categoryStatusLabel.getText() == null || categoryStatusLabel.getText().isBlank()) {
            categoryStatusLabel.setText("Search categories or select a row to update it.");
        }
    }

    private void updateManufacturerControls(UserSession session, boolean usingLiveDatabase) {
        CatalogLookupRecord selectedManufacturer = manufacturerTable.getSelectionModel().getSelectedItem();
        String selectedManufacturerName = selectedManufacturer == null ? null : selectedManufacturer.name();

        List<CatalogLookupRecord> manufacturers = catalogLookupManagementService.loadManufacturers(manufacturerSearchField.getText());
        manufacturerTable.getItems().setAll(manufacturers);

        if (selectedManufacturerName != null) {
            manufacturers.stream()
                    .filter(manufacturer -> manufacturer.name().equals(selectedManufacturerName))
                    .findFirst()
                    .ifPresent(manufacturer -> manufacturerTable.getSelectionModel().select(manufacturer));
        }

        boolean canManageCatalog = session.isAdmin() && usingLiveDatabase;
        manufacturerNameField.setDisable(!canManageCatalog);
        createManufacturerButton.setDisable(!canManageCatalog);
        updateManufacturerButton.setDisable(!canManageCatalog);
        deleteManufacturerButton.setDisable(!canManageCatalog);

        manufacturerSummaryLabel.setText(manufacturers.size() + " manufacturers shown");
        if (!usingLiveDatabase) {
            manufacturerStatusLabel.setText("Manufacturer edits need a live database connection.");
        } else if (!canManageCatalog) {
            manufacturerStatusLabel.setText("Manufacturers are read-only for this session.");
        } else if (manufacturerStatusLabel.getText() == null || manufacturerStatusLabel.getText().isBlank()) {
            manufacturerStatusLabel.setText("Search manufacturers or select a row to update it.");
        }
    }

    private void updateWarehouseControls(UserSession session, boolean usingLiveDatabase) {
        WarehouseRecord selectedWarehouse = warehouseTable.getSelectionModel().getSelectedItem();
        String selectedWarehouseCode = selectedWarehouse == null ? null : selectedWarehouse.warehouseCode();
        String selectedWarehouseActive = warehouseActiveSelector.getSelectionModel().getSelectedItem();

        List<WarehouseRecord> warehouses = warehouseManagementService.loadWarehouses(warehouseSearchField.getText());
        warehouseTable.getItems().setAll(warehouses);

        if (selectedWarehouseCode != null) {
            warehouses.stream()
                    .filter(warehouse -> warehouse.warehouseCode().equals(selectedWarehouseCode))
                    .findFirst()
                    .ifPresent(warehouse -> warehouseTable.getSelectionModel().select(warehouse));
        }

        if (selectedWarehouseActive != null) {
            warehouseActiveSelector.getSelectionModel().select(selectedWarehouseActive);
        } else if (warehouseActiveSelector.getSelectionModel().isEmpty()) {
            warehouseActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
        }

        boolean canManageWarehouses = session.isAdmin() && usingLiveDatabase;
        warehouseCodeField.setDisable(!canManageWarehouses);
        warehouseNameField.setDisable(!canManageWarehouses);
        warehouseCityField.setDisable(!canManageWarehouses);
        warehouseAddressField.setDisable(!canManageWarehouses);
        warehouseActiveSelector.setDisable(!canManageWarehouses);
        createWarehouseButton.setDisable(!canManageWarehouses);
        updateWarehouseButton.setDisable(!canManageWarehouses);
        deleteWarehouseButton.setDisable(!canManageWarehouses);

        warehouseSummaryLabel.setText(warehouses.size() + " warehouses shown");
        if (!usingLiveDatabase) {
            warehouseStatusLabel.setText("Warehouse edits need a live database connection.");
        } else if (!canManageWarehouses) {
            warehouseStatusLabel.setText("Warehouse administration is read-only for this session.");
        } else if (warehouseStatusLabel.getText() == null || warehouseStatusLabel.getText().isBlank()) {
            warehouseStatusLabel.setText("Search warehouses or select a row to update it.");
        }
    }

    private void updateUserControls(UserSession session, boolean usingLiveDatabase) {
        ManagedUserRecord selectedUser = userTable.getSelectionModel().getSelectedItem();
        String selectedUsername = selectedUser == null ? null : selectedUser.username();
        Role selectedRole = userRoleSelector.getSelectionModel().getSelectedItem();
        WarehouseAssignmentOption selectedWarehouse = userWarehouseSelector.getSelectionModel().getSelectedItem();
        String selectedActive = userActiveSelector.getSelectionModel().getSelectedItem();

        List<ManagedUserRecord> users = userManagementService.loadUsers(userSearchField.getText());
        List<WarehouseAssignmentOption> warehouseAssignments = userManagementService.loadWarehouseAssignments();

        userTable.getItems().setAll(users);
        userWarehouseSelector.setItems(FXCollections.observableArrayList(warehouseAssignments));

        if (selectedUsername != null) {
            users.stream()
                    .filter(user -> user.username().equals(selectedUsername))
                    .findFirst()
                    .ifPresent(user -> userTable.getSelectionModel().select(user));
        }

        if (selectedRole != null) {
            userRoleSelector.getSelectionModel().select(selectedRole);
        } else if (userRoleSelector.getSelectionModel().isEmpty()) {
            userRoleSelector.getSelectionModel().select(Role.WAREHOUSE_MANAGER);
        }

        WarehouseAssignmentOption targetWarehouseOption = selectedWarehouse;
        ManagedUserRecord selectedTableUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedTableUser != null) {
            targetWarehouseOption = findWarehouseAssignmentOption(selectedTableUser.warehouseCode());
        } else if (selectedWarehouse != null) {
            targetWarehouseOption = findWarehouseAssignmentOption(selectedWarehouse.warehouseCode());
        }

        if (targetWarehouseOption != null) {
            userWarehouseSelector.getSelectionModel().select(targetWarehouseOption);
        } else if (!warehouseAssignments.isEmpty()) {
            userWarehouseSelector.getSelectionModel().selectFirst();
        }

        if (selectedActive != null) {
            userActiveSelector.getSelectionModel().select(selectedActive);
        } else if (userActiveSelector.getSelectionModel().isEmpty()) {
            userActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
        }

        boolean canManageUsers = session.role().canManageUsers() && usingLiveDatabase;
        userUsernameField.setDisable(!canManageUsers);
        userPasswordField.setDisable(!canManageUsers);
        userFullNameField.setDisable(!canManageUsers);
        userRoleSelector.setDisable(!canManageUsers);
        userActiveSelector.setDisable(!canManageUsers);
        createUserButton.setDisable(!canManageUsers);
        updateUserButton.setDisable(!canManageUsers);
        deleteUserButton.setDisable(!canManageUsers);
        refreshUserWarehouseSelectorState();

        userSummaryLabel.setText(users.size() + " users shown");
        if (!usingLiveDatabase) {
            userStatusLabel.setText("User edits need a live database connection.");
        } else if (!canManageUsers) {
            userStatusLabel.setText("User administration is read-only for this session.");
        } else if (userStatusLabel.getText() == null || userStatusLabel.getText().isBlank()) {
            userStatusLabel.setText("Search users or select a row to update it. Leave password blank to keep it unchanged.");
        }
    }

    private void updateStockOperationControls(UserSession session, boolean usingLiveDatabase) {
        Warehouse selectedSupplyWarehouse = supplyWarehouseSelector.getSelectionModel().getSelectedItem();
        Warehouse selectedDispatchWarehouse = dispatchWarehouseSelector.getSelectionModel().getSelectedItem();
        ProductOption selectedSupplyProduct = supplyProductSelector.getValue();

        List<Warehouse> warehouses = stockOperationService.loadWarehouses(session);
        allSupplyProducts = stockOperationService.loadProducts();

        supplyWarehouseSelector.setItems(FXCollections.observableArrayList(warehouses));
        dispatchWarehouseSelector.setItems(FXCollections.observableArrayList(warehouses));
        setSupplyProductItems(allSupplyProducts);
        supplyNewProductManufacturerSelector.setItems(FXCollections.observableArrayList(productManagementService.loadManufacturers()));
        supplyNewProductCategorySelector.setItems(FXCollections.observableArrayList(productManagementService.loadCategories()));

        selectWarehouseOrFirst(supplyWarehouseSelector, selectedSupplyWarehouse, warehouses);
        selectWarehouseOrFirst(dispatchWarehouseSelector, selectedDispatchWarehouse, warehouses);
        if (selectedSupplyProduct != null) {
            allSupplyProducts.stream()
                    .filter(product -> product.productCode().equals(selectedSupplyProduct.productCode()))
                    .findFirst()
                    .ifPresent(product -> supplyProductSelector.getSelectionModel().select(product));
        } else if (!allSupplyProducts.isEmpty() && supplyProductSelector.getSelectionModel().isEmpty()) {
            supplyProductSelector.getSelectionModel().selectFirst();
        }

        boolean canSupply = session.role().canSupply() && usingLiveDatabase;
        boolean canDispatch = session.role().canDispatch() && usingLiveDatabase;
        boolean warehouseEditable = session.isAdmin() && usingLiveDatabase;

        supplyWarehouseSelector.setDisable(!warehouseEditable);
        dispatchWarehouseSelector.setDisable(!warehouseEditable);
        refreshSupplyProductMode();
        supplyQuantityField.setDisable(!canSupply);
        supplySupplierField.setDisable(!canSupply);
        supplyNoteField.setDisable(!canSupply);
        supplyNewProductCheckBox.setDisable(!canSupply);
        dispatchInventorySearchField.setDisable(!canDispatch);
        dispatchInventoryTable.setDisable(!canDispatch);
        dispatchQuantityField.setDisable(!canDispatch);
        dispatchDestinationField.setDisable(!canDispatch);
        dispatchNoteField.setDisable(!canDispatch);
        refreshDispatchInventory();
        updateStockOperationButtons(usingLiveDatabase);

        if (!usingLiveDatabase) {
            supplyStatusLabel.setText("Stock operations need a live database connection.");
            dispatchStatusLabel.setText("Stock operations need a live database connection.");
        } else if (!canSupply && !canDispatch) {
            supplyStatusLabel.setText("This session is read-only for stock operations.");
            dispatchStatusLabel.setText("This session is read-only for stock operations.");
        } else {
            if (canSupply && (supplyStatusLabel.getText() == null || supplyStatusLabel.getText().isBlank()
                    || supplyStatusLabel.getText().startsWith("Waiting"))) {
                supplyStatusLabel.setText("Build a supply draft, then record incoming stock.");
            }
            if (canDispatch && (dispatchStatusLabel.getText() == null || dispatchStatusLabel.getText().isBlank()
                    || dispatchStatusLabel.getText().startsWith("Waiting"))) {
                dispatchStatusLabel.setText("Select inventory, build a dispatch draft, then record outgoing stock.");
            }
        }
    }

    private void updateStockOperationButtons(boolean usingLiveDatabase) {
        boolean canSupply = currentSession != null && currentSession.role().canSupply() && usingLiveDatabase;
        boolean canDispatch = currentSession != null && currentSession.role().canDispatch() && usingLiveDatabase;

        addSupplyLineButton.setDisable(!canSupply);
        removeSupplyLineButton.setDisable(!canSupply || supplyDraftLines.isEmpty());
        clearSupplyDraftButton.setDisable(!canSupply || supplyDraftLines.isEmpty());
        recordSupplyButton.setDisable(!canSupply || supplyDraftLines.isEmpty());

        InventorySummary selectedInventory = dispatchInventoryTable.getSelectionModel().getSelectedItem();
        addDispatchLineButton.setDisable(!canDispatch || selectedInventory == null || getRemainingDispatchQuantity(selectedInventory) <= 0);
        removeDispatchLineButton.setDisable(!canDispatch || dispatchDraftLines.isEmpty());
        clearDispatchDraftButton.setDisable(!canDispatch || dispatchDraftLines.isEmpty());
        recordDispatchButton.setDisable(!canDispatch || dispatchDraftLines.isEmpty());
    }

    private void refreshSupplyProductMode() {
        boolean canSupply = currentSession != null && currentSession.role().canSupply() && DatabaseConnection.canConnect();
        boolean newProduct = supplyNewProductCheckBox.isSelected();
        supplyProductSelector.setDisable(!canSupply || newProduct);
        setPageVisibility(supplyNewProductFields, canSupply && newProduct);
        supplyNewProductCodeField.setDisable(!canSupply || !newProduct);
        supplyNewProductNameField.setDisable(!canSupply || !newProduct);
        supplyNewProductUnitField.setDisable(!canSupply || !newProduct);
        supplyNewProductManufacturerSelector.setDisable(!canSupply || !newProduct);
        supplyNewProductCategorySelector.setDisable(!canSupply || !newProduct);
        supplyNewProductDescriptionField.setDisable(!canSupply || !newProduct);
    }

    private void setSupplyProductItems(List<ProductOption> products) {
        String editorText = supplyProductSelector.isEditable() ? supplyProductSelector.getEditor().getText() : "";
        int caretPosition = supplyProductSelector.isEditable() ? supplyProductSelector.getEditor().getCaretPosition() : 0;
        boolean restoreEditor = supplyProductSelector.isEditable() && supplyProductSelector.getEditor().isFocused();
        suppressSupplyProductSearch = true;
        try {
            supplyProductSelector.setItems(FXCollections.observableArrayList(products));
            if (restoreEditor) {
                restoreSupplyProductEditor(editorText, caretPosition);
            }
        } finally {
            suppressSupplyProductSearch = false;
        }
    }

    private void scheduleSupplyProductFilter(String searchTerm) {
        int searchVersion = ++supplyProductSearchVersion;
        Platform.runLater(() -> {
            if (searchVersion != supplyProductSearchVersion
                    || suppressSupplyProductSearch
                    || supplyNewProductCheckBox.isSelected()) {
                return;
            }

            String currentText = supplyProductSelector.getEditor().getText();
            if (isSupplyProductDisplayText(currentText)) {
                return;
            }
            filterSupplyProductSelector(currentText == null ? searchTerm : currentText);
        });
    }

    private void filterSupplyProductSelector(String searchTerm) {
        String normalizedSearch = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
        List<ProductOption> filteredProducts = normalizedSearch.isBlank()
                ? allSupplyProducts
                : allSupplyProducts.stream()
                .filter(product -> product.productCode().toLowerCase().contains(normalizedSearch)
                        || product.productName().toLowerCase().contains(normalizedSearch)
                        || product.manufacturer().toLowerCase().contains(normalizedSearch)
                        || product.category().toLowerCase().contains(normalizedSearch)
                        || product.toString().toLowerCase().contains(normalizedSearch))
                .toList();

        setSupplyProductItems(filteredProducts);
        if (filteredProducts.isEmpty()) {
            supplyProductSelector.hide();
        } else if (supplyProductSelector.getEditor().isFocused()) {
            supplyProductSelector.show();
        }
    }

    private ProductOption getSelectedSupplyProduct() {
        String editorText = supplyProductSelector.getEditor().getText();
        ProductOption typedProduct = findSupplyProductByExactText(editorText);
        if (typedProduct != null) {
            return typedProduct;
        }

        ProductOption selectedProduct = supplyProductSelector.getValue();
        if (selectedProduct != null && selectedProduct.toString().equals(editorText)) {
            return selectedProduct;
        }
        return null;
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

        String normalizedValue = value.trim();
        return allSupplyProducts.stream()
                .filter(product -> product.productCode().equalsIgnoreCase(normalizedValue)
                        || product.productName().equalsIgnoreCase(normalizedValue)
                        || product.toString().equalsIgnoreCase(normalizedValue))
                .findFirst()
                .orElse(null);
    }

    private void restoreSupplyProductEditor(String text, int caretPosition) {
        supplyProductSelector.getEditor().setText(text);
        supplyProductSelector.getEditor().positionCaret(Math.min(caretPosition, text.length()));
    }

    private void refreshDispatchInventory() {
        if (currentSession == null || dispatchInventoryTable == null) {
            return;
        }

        Warehouse selectedWarehouse = dispatchWarehouseSelector.getSelectionModel().getSelectedItem();
        if (selectedWarehouse == null) {
            dispatchInventoryTable.getItems().clear();
            dispatchInventorySummaryLabel.setText("Select a warehouse.");
            updateStockOperationButtons(DatabaseConnection.canConnect());
            return;
        }

        InventorySummary currentSelection = dispatchInventoryTable.getSelectionModel().getSelectedItem();
        String currentProductCode = currentSelection == null ? null : currentSelection.productCode();
        InventoryFilterCriteria criteria = new InventoryFilterCriteria(
                dispatchInventorySearchField.getText(),
                null,
                null,
                selectedWarehouse.code()
        );
        List<InventorySummary> items = inventoryInsightsService.loadInventory(currentSession, criteria);
        dispatchInventoryTable.getItems().setAll(items);
        if (currentProductCode != null) {
            items.stream()
                    .filter(item -> item.productCode().equals(currentProductCode))
                    .findFirst()
                    .ifPresent(item -> dispatchInventoryTable.getSelectionModel().select(item));
        }
        dispatchInventorySummaryLabel.setText(items.size() + " item(s) in " + selectedWarehouse.label());
        updateStockOperationButtons(DatabaseConnection.canConnect());
    }

    private void upsertStockLine(ObservableList<StockOperationLine> lines, StockOperationLine newLine) {
        for (int index = 0; index < lines.size(); index++) {
            StockOperationLine existingLine = lines.get(index);
            if (existingLine.productCode().equalsIgnoreCase(newLine.productCode())) {
                lines.set(index, new StockOperationLine(
                        existingLine.newProduct(),
                        existingLine.productCode(),
                        existingLine.productName(),
                        existingLine.manufacturer(),
                        existingLine.category(),
                        existingLine.unitName(),
                        existingLine.description(),
                        existingLine.quantity() + newLine.quantity()
                ));
                return;
            }
        }
        lines.add(newLine);
    }

    private String buildSupplyDetails() {
        String note = supplyNoteField.getText() == null || supplyNoteField.getText().isBlank()
                ? ""
                : "Note: " + supplyNoteField.getText().trim();
        return note;
    }

    private String buildDispatchDetails() {
        String note = dispatchNoteField.getText() == null || dispatchNoteField.getText().isBlank()
                ? ""
                : "Note: " + dispatchNoteField.getText().trim();
        return note;
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

    private void updateRequestControls(UserSession session, boolean usingLiveDatabase) {
        Warehouse selectedSource = requestSourceWarehouseSelector.getSelectionModel().getSelectedItem();
        Warehouse selectedTarget = requestTargetWarehouseSelector.getSelectionModel().getSelectedItem();

        List<Warehouse> sourceWarehouses = requestActionService.loadSourceWarehouses(session);
        allRequestWarehouses = requestActionService.loadAllWarehouses();

        requestSourceWarehouseSelector.setItems(FXCollections.observableArrayList(sourceWarehouses));

        if (selectedSource != null && sourceWarehouses.stream().anyMatch(warehouse -> warehouse.code().equals(selectedSource.code()))) {
            requestSourceWarehouseSelector.getSelectionModel().select(
                    sourceWarehouses.stream().filter(warehouse -> warehouse.code().equals(selectedSource.code())).findFirst().orElse(null)
            );
        } else if (!sourceWarehouses.isEmpty()) {
            requestSourceWarehouseSelector.getSelectionModel().selectFirst();
        }

        refreshRequestTargetOptions();
        if (selectedTarget != null) {
            requestTargetWarehouseSelector.getItems().stream()
                    .filter(warehouse -> warehouse.code().equals(selectedTarget.code()))
                    .findFirst()
                    .ifPresent(warehouse -> requestTargetWarehouseSelector.getSelectionModel().select(warehouse));
        }

        refreshRequestTargetInventory();

        boolean canCreateRequests = session.role().canCreateRequests() && usingLiveDatabase;
        boolean canApproveRequests = session.role().canApproveRequests() && usingLiveDatabase;

        requestSourceWarehouseSelector.setDisable(!usingLiveDatabase || !session.isAdmin());
        requestTargetWarehouseSelector.setDisable(!canCreateRequests);
        requestInventorySearchField.setDisable(!canCreateRequests);
        requestInventoryTable.setDisable(!canCreateRequests);
        requestQuantityField.setDisable(!canCreateRequests);
        requestNoteField.setDisable(!canCreateRequests);
        addRequestLineButton.setDisable(!canCreateRequests
                || requestInventoryTable.getSelectionModel().getSelectedItem() == null
                || getRemainingAvailableQuantity(requestInventoryTable.getSelectionModel().getSelectedItem()) <= 0);
        removeRequestLineButton.setDisable(!canCreateRequests || requestDraftLines.isEmpty());
        clearRequestDraftButton.setDisable(!canCreateRequests || requestDraftLines.isEmpty());
        updateRequestActionButtons(usingLiveDatabase);
        updateRequestViewButtonStates();

        if (!usingLiveDatabase) {
            requestActionStatusLabel.setText("Request actions need a live database connection.");
        } else if (!canCreateRequests && !canApproveRequests) {
            requestActionStatusLabel.setText("This session is read-only for warehouse requests.");
        } else if (requestActionStatusLabel.getText() == null || requestActionStatusLabel.getText().isBlank()) {
            requestActionStatusLabel.setText(canApproveRequests
                    ? "Create requests or approve/reject the selected pending request."
                    : "Create requests for another warehouse from this form.");
        }
    }

    private void updateRequestDraftButtons() {
        boolean canCreateRequests = currentSession != null && currentSession.role().canCreateRequests() && DatabaseConnection.canConnect();
        InventorySummary selectedInventory = requestInventoryTable.getSelectionModel().getSelectedItem();
        addRequestLineButton.setDisable(!canCreateRequests || selectedInventory == null || getRemainingAvailableQuantity(selectedInventory) <= 0);
        removeRequestLineButton.setDisable(!canCreateRequests || requestDraftLines.isEmpty());
        clearRequestDraftButton.setDisable(!canCreateRequests || (requestDraftLines.isEmpty() && editingRequestNumber == null));
        updateRequestActionButtons(DatabaseConnection.canConnect());
    }

    private void updateInventoryFilters() {
        String selectedCategory = inventoryCategoryFilterSelector.getSelectionModel().getSelectedItem();
        String selectedManufacturer = inventoryManufacturerFilterSelector.getSelectionModel().getSelectedItem();

        List<String> categories = new ArrayList<>();
        categories.add("All categories");
        categories.addAll(productManagementService.loadCategories());

        List<String> manufacturers = new ArrayList<>();
        manufacturers.add("All manufacturers");
        manufacturers.addAll(productManagementService.loadManufacturers());

        suppressInventoryFilterRefresh = true;
        try {
            inventoryCategoryFilterSelector.setItems(FXCollections.observableArrayList(categories));
            inventoryManufacturerFilterSelector.setItems(FXCollections.observableArrayList(manufacturers));
            selectOrFirst(inventoryCategoryFilterSelector, selectedCategory);
            selectOrFirst(inventoryManufacturerFilterSelector, selectedManufacturer);
        } finally {
            suppressInventoryFilterRefresh = false;
        }
    }

    private void updateInventoryView() {
        if (currentSession == null) {
            return;
        }

        InventoryFilterCriteria criteria = new InventoryFilterCriteria(
                inventorySearchField == null ? "" : inventorySearchField.getText(),
                normalizeAllSelection(inventoryCategoryFilterSelector),
                normalizeAllSelection(inventoryManufacturerFilterSelector),
                null
        );
        updateInventory(inventoryInsightsService.loadInventory(currentSession, criteria));
    }

    private void updateReportControls() {
        if (currentSession == null) {
            return;
        }

        Warehouse selectedWarehouse = reportWarehouseSelector.getSelectionModel().getSelectedItem();
        String selectedCategory = reportCategorySelector.getSelectionModel().getSelectedItem();
        String selectedManufacturer = reportManufacturerSelector.getSelectionModel().getSelectedItem();
        String selectedSort = reportSortSelector.getSelectionModel().getSelectedItem();

        List<Warehouse> warehouses = new ArrayList<>();
        warehouses.add(new Warehouse("", "All Warehouses", ""));
        warehouses.addAll(inventoryInsightsService.loadReportWarehouses(currentSession));

        List<String> categories = new ArrayList<>();
        categories.add("All categories");
        categories.addAll(productManagementService.loadCategories());

        List<String> manufacturers = new ArrayList<>();
        manufacturers.add("All manufacturers");
        manufacturers.addAll(productManagementService.loadManufacturers());

        suppressReportFilterRefresh = true;
        try {
            reportWarehouseSelector.setItems(FXCollections.observableArrayList(warehouses));
            reportCategorySelector.setItems(FXCollections.observableArrayList(categories));
            reportManufacturerSelector.setItems(FXCollections.observableArrayList(manufacturers));

            if (selectedWarehouse != null) {
                warehouses.stream()
                        .filter(warehouse -> warehouse.code().equals(selectedWarehouse.code()))
                        .findFirst()
                        .ifPresentOrElse(
                                warehouse -> reportWarehouseSelector.getSelectionModel().select(warehouse),
                                () -> reportWarehouseSelector.getSelectionModel().selectFirst()
                        );
            } else if (reportWarehouseSelector.getSelectionModel().isEmpty()) {
                reportWarehouseSelector.getSelectionModel().selectFirst();
            }

            selectOrFirst(reportCategorySelector, selectedCategory);
            selectOrFirst(reportManufacturerSelector, selectedManufacturer);
            selectOrFirst(reportSortSelector, selectedSort == null ? SORT_BY_WAREHOUSE : selectedSort);
        } finally {
            suppressReportFilterRefresh = false;
        }
    }

    private void updateReportView() {
        if (currentSession == null) {
            return;
        }

        Warehouse selectedWarehouse = reportWarehouseSelector.getSelectionModel().getSelectedItem();
        InventoryFilterCriteria criteria = new InventoryFilterCriteria(
                "",
                normalizeAllSelection(reportCategorySelector),
                normalizeAllSelection(reportManufacturerSelector),
                selectedWarehouse == null || selectedWarehouse.code().isBlank() ? null : selectedWarehouse.code()
        );
        List<InventoryReportRow> rows = inventoryInsightsService.loadInventoryReport(currentSession, criteria);
        rows = sortReportRows(rows);
        reportTable.getItems().setAll(rows);
        reportSummaryLabel.setText(rows.size() + " rows | Warehouse + category + manufacturer filters | Sort: "
                + reportSortSelector.getSelectionModel().getSelectedItem());
    }

    private List<InventoryReportRow> sortReportRows(List<InventoryReportRow> rows) {
        String selectedSort = reportSortSelector.getSelectionModel().getSelectedItem();
        Comparator<InventoryReportRow> comparator = switch (selectedSort == null ? SORT_BY_WAREHOUSE : selectedSort) {
            case SORT_BY_CODE -> Comparator.comparing(InventoryReportRow::productCode, String.CASE_INSENSITIVE_ORDER);
            case SORT_BY_PRODUCT -> Comparator.comparing(InventoryReportRow::productName, String.CASE_INSENSITIVE_ORDER);
            case SORT_BY_QUANTITY -> Comparator.comparingInt(InventoryReportRow::quantity).reversed();
            default -> Comparator.comparing(InventoryReportRow::warehouseLabel, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(InventoryReportRow::productCode, String.CASE_INSENSITIVE_ORDER);
        };
        return rows.stream().sorted(comparator).toList();
    }

    private void refreshRequestTargetOptions() {
        Warehouse selectedSource = requestSourceWarehouseSelector.getSelectionModel().getSelectedItem();
        Warehouse currentTarget = requestTargetWarehouseSelector.getSelectionModel().getSelectedItem();

        List<Warehouse> targets = allRequestWarehouses.stream()
                .filter(warehouse -> selectedSource == null || !warehouse.code().equals(selectedSource.code()))
                .toList();

        requestTargetWarehouseSelector.setItems(FXCollections.observableArrayList(targets));

        if (currentTarget != null && targets.stream().anyMatch(warehouse -> warehouse.code().equals(currentTarget.code()))) {
            requestTargetWarehouseSelector.getSelectionModel().select(
                    targets.stream().filter(warehouse -> warehouse.code().equals(currentTarget.code())).findFirst().orElse(null)
            );
        } else if (!targets.isEmpty()) {
            requestTargetWarehouseSelector.getSelectionModel().selectFirst();
        }

        refreshRequestTargetInventory();
    }

    private void refreshRequestTargetInventory() {
        if (currentSession == null) {
            requestInventoryTable.getItems().clear();
            requestInventorySummaryLabel.setText("Sign in to view request inventory.");
            updateRequestDraftButtons();
            return;
        }

        Warehouse selectedTarget = requestTargetWarehouseSelector.getSelectionModel().getSelectedItem();
        if (selectedTarget == null) {
            requestInventoryTable.getItems().clear();
            requestInventorySummaryLabel.setText("Select a target warehouse.");
            updateRequestDraftButtons();
            return;
        }

        InventorySummary currentSelection = requestInventoryTable.getSelectionModel().getSelectedItem();
        String currentProductCode = currentSelection == null ? null : currentSelection.productCode();
        List<InventorySummary> targetInventory = requestActionService.loadTargetInventory(
                selectedTarget,
                requestInventorySearchField.getText()
        );

        requestInventoryTable.getItems().setAll(targetInventory);
        if (currentProductCode != null) {
            targetInventory.stream()
                    .filter(item -> item.productCode().equals(currentProductCode))
                    .findFirst()
                    .ifPresent(item -> requestInventoryTable.getSelectionModel().select(item));
        }

        String searchTerm = requestInventorySearchField.getText() == null ? "" : requestInventorySearchField.getText().trim();
        requestInventorySummaryLabel.setText(targetInventory.size() + " available item(s) in " + selectedTarget.label()
                + (searchTerm.isBlank() ? "" : " matching \"" + searchTerm + "\""));
        updateRequestDraftButtons();
    }

    private void updateInventory(List<InventorySummary> inventoryItems) {
        inventoryTable.getItems().setAll(inventoryItems);
        inventorySummaryLabel.setText(inventoryItems.size() + " visible stock rows");
    }

    private void updateRequests(List<RequestSummary> requestSummaries) {
        RequestSummary currentSelection = requestTable.getSelectionModel().getSelectedItem();
        String currentRequestNumber = currentSelection == null ? null : currentSelection.requestNumber();

        requestTable.getItems().setAll(requestSummaries);
        if (currentRequestNumber != null) {
            requestSummaries.stream()
                    .filter(summary -> summary.requestNumber().equals(currentRequestNumber))
                    .findFirst()
                    .ifPresent(summary -> requestTable.getSelectionModel().select(summary));
        }

        long pendingCount = requestSummaries.stream().filter(summary -> summary.status() == RequestStatus.PENDING).count();
        requestSummaryLabel.setText(requestSummaries.size() + " " + activeRequestViewMode.summaryLabel()
                + " | " + pendingCount + " pending");
        updateRequestActionButtons(DatabaseConnection.canConnect());
    }

    private void refreshRequestsOnly() {
        if (currentSession == null) {
            return;
        }

        updateRequests(requestActionService.loadRequests(currentSession, activeRequestViewMode));
        refreshSelectedRequestDetail();
    }

    private void setActiveRequestView(RequestViewMode viewMode) {
        activeRequestViewMode = viewMode;
        updateRequestViewButtonStates();
        refreshRequestsOnly();
        requestActionStatusLabel.setText("Showing " + viewMode.summaryLabel() + ".");
    }

    private void updateRequestViewButtonStates() {
        applyRequestViewButtonState(requestIncomingViewButton, activeRequestViewMode == RequestViewMode.INCOMING);
        applyRequestViewButtonState(requestOutgoingViewButton, activeRequestViewMode == RequestViewMode.OUTGOING);
        applyRequestViewButtonState(requestHistoryViewButton, activeRequestViewMode == RequestViewMode.HISTORY);
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
        if (currentSession == null) {
            return;
        }

        RequestSummary selectedRequest = requestTable.getSelectionModel().getSelectedItem();
        boolean canCreateRequests = currentSession.role().canCreateRequests() && usingLiveDatabase;
        boolean canApproveSelected = canApproveSelectedPendingRequest(selectedRequest) && usingLiveDatabase;
        boolean canEditSelected = canEditSelectedPendingRequest(selectedRequest) && usingLiveDatabase;
        boolean editing = editingRequestNumber != null;

        if (requestEditorTitleLabel != null) {
            requestEditorTitleLabel.setText(editing
                    ? "Editing " + editingRequestNumber
                    : "Create / Edit Request");
        }

        requestSourceWarehouseSelector.setDisable(!usingLiveDatabase || !currentSession.isAdmin());
        requestTargetWarehouseSelector.setDisable(!canCreateRequests);
        requestInventorySearchField.setDisable(!canCreateRequests);
        requestInventoryTable.setDisable(!canCreateRequests);
        requestQuantityField.setDisable(!canCreateRequests);
        requestNoteField.setDisable(!canCreateRequests);
        addRequestLineButton.setDisable(!canCreateRequests
                || requestInventoryTable.getSelectionModel().getSelectedItem() == null
                || getRemainingAvailableQuantity(requestInventoryTable.getSelectionModel().getSelectedItem()) <= 0);
        removeRequestLineButton.setDisable(!canCreateRequests || requestDraftLines.isEmpty());
        clearRequestDraftButton.setDisable(!canCreateRequests || (requestDraftLines.isEmpty() && !editing));
        createRequestButton.setDisable(!canCreateRequests || editing);
        saveRequestEditsButton.setDisable(!canCreateRequests || !editing);
        approveRequestButton.setDisable(!canApproveSelected);
        rejectRequestButton.setDisable(!canApproveSelected);
        editSelectedRequestButton.setDisable(!canEditSelected);
        cancelSelectedRequestButton.setDisable(!canEditSelected);
    }

    private boolean canApproveSelectedPendingRequest(RequestSummary requestSummary) {
        if (requestSummary == null || requestSummary.status() != RequestStatus.PENDING || currentSession == null) {
            return false;
        }
        return currentSession.isAdmin()
                || (currentSession.role().canApproveRequests()
                && requestSummary.destinationWarehouseCode().equals(currentSession.warehouse().code()));
    }

    private boolean canEditSelectedPendingRequest(RequestSummary requestSummary) {
        if (requestSummary == null || requestSummary.status() != RequestStatus.PENDING || currentSession == null) {
            return false;
        }
        return currentSession.isAdmin()
                || (currentSession.role() == Role.WAREHOUSE_MANAGER
                && requestSummary.requestedByUsername().equals(currentSession.username()));
    }

    private void updateMovementLogs(List<MovementLogEntry> entries, WarehouseFilterOption filterOption) {
        currentMovementLogEntries = entries;
        currentMovementWarehouseFilter = filterOption;
        refreshMovementLogView();
    }

    private void refreshMovementLogView() {
        if (movementLogTable == null || currentMovementLogEntries == null) {
            return;
        }

        String selectedType = movementTypeFilterSelector == null ? ALL_MOVEMENT_TYPES_LABEL
                : movementTypeFilterSelector.getSelectionModel().getSelectedItem();
        LocalDate fromDate = movementFromDatePicker == null ? null : movementFromDatePicker.getValue();
        LocalDate toDate = movementToDatePicker == null ? null : movementToDatePicker.getValue();

        List<MovementLogEntry> filteredEntries = currentMovementLogEntries.stream()
                .filter(entry -> selectedType == null
                        || ALL_MOVEMENT_TYPES_LABEL.equals(selectedType)
                        || entry.type().displayName().equals(selectedType))
                .filter(entry -> fromDate == null || !entry.timestamp().toLocalDate().isBefore(fromDate))
                .filter(entry -> toDate == null || !entry.timestamp().toLocalDate().isAfter(toDate))
                .toList();

        movementLogTable.getItems().setAll(filteredEntries);
        String warehouseLabel = currentMovementWarehouseFilter == null ? "All warehouses" : currentMovementWarehouseFilter.label();
        String typeLabel = selectedType == null ? ALL_MOVEMENT_TYPES_LABEL : selectedType;
        String dateLabel = (fromDate == null && toDate == null)
                ? "All dates"
                : (fromDate == null ? "Any start" : DATE_FILTER_FORMATTER.format(fromDate))
                + " to "
                + (toDate == null ? "Any end" : DATE_FILTER_FORMATTER.format(toDate));
        movementLogSummaryLabel.setText(filteredEntries.size() + " movement records shown | Warehouse: "
                + warehouseLabel + " | Type: " + typeLabel + " | Dates: " + dateLabel);
    }

    private void executeRequestAction(boolean approve) {
        if (currentSession == null) {
            return;
        }

        RequestSummary selectedRequest = requestTable.getSelectionModel().getSelectedItem();

        RequestActionResult result = approve
                ? requestActionService.approveRequest(currentSession, selectedRequest)
                : requestActionService.rejectRequest(currentSession, selectedRequest);

        if (result.success()) {
            refreshDashboard();
            requestActionStatusLabel.setText(result.message());
            return;
        }

        requestActionStatusLabel.setText(result.message());
    }

    private void populateProductForm(ProductRecord product) {
        if (product == null) {
            return;
        }

        productCodeField.setText(product.productCode());
        productModelField.setText(product.modelName());
        productManufacturerSelector.getSelectionModel().select(product.manufacturer());
        productCategorySelector.getSelectionModel().select(product.category());
        productUnitField.setText(product.unitName());
        productDescriptionField.setText(product.description());
    }

    private void populateCategoryForm(CatalogLookupRecord category) {
        if (category == null) {
            return;
        }
        categoryNameField.setText(category.name());
    }

    private void populateManufacturerForm(CatalogLookupRecord manufacturer) {
        if (manufacturer == null) {
            return;
        }
        manufacturerNameField.setText(manufacturer.name());
    }

    private void populateWarehouseForm(WarehouseRecord warehouse) {
        if (warehouse == null) {
            return;
        }

        warehouseCodeField.setText(warehouse.warehouseCode());
        warehouseNameField.setText(warehouse.warehouseName());
        warehouseCityField.setText(warehouse.city());
        warehouseAddressField.setText(warehouse.addressLine());
        warehouseActiveSelector.getSelectionModel().select(warehouse.active() ? ACTIVE_LABEL : INACTIVE_LABEL);
    }

    private void populateUserForm(ManagedUserRecord user) {
        if (user == null) {
            return;
        }

        userUsernameField.setText(user.username());
        userPasswordField.clear();
        userFullNameField.setText(user.fullName());
        userRoleSelector.getSelectionModel().select(user.role());
        WarehouseAssignmentOption option = findWarehouseAssignmentOption(user.warehouseCode());
        if (option != null) {
            userWarehouseSelector.getSelectionModel().select(option);
        }
        userActiveSelector.getSelectionModel().select(user.active() ? ACTIVE_LABEL : INACTIVE_LABEL);
        refreshUserWarehouseSelectorState();
    }

    private void clearProductForm() {
        productCodeField.clear();
        productModelField.clear();
        productUnitField.setText("pcs");
        productDescriptionField.clear();
        if (!productManufacturerSelector.getItems().isEmpty()) {
            productManufacturerSelector.getSelectionModel().selectFirst();
        }
        if (!productCategorySelector.getItems().isEmpty()) {
            productCategorySelector.getSelectionModel().selectFirst();
        }
    }

    private void clearCategoryForm() {
        categoryNameField.clear();
    }

    private void clearManufacturerForm() {
        manufacturerNameField.clear();
    }

    private void clearRequestDraftState() {
        editingRequestNumber = null;
        requestDraftLines.clear();
        requestDraftTable.getSelectionModel().clearSelection();
        requestQuantityField.clear();
        requestQuantityField.setPromptText("Qty");
        requestNoteField.clear();
        requestInventoryTable.refresh();
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

    private void refreshSelectedRequestDetail() {
        if (currentSession == null) {
            clearRequestDetail();
            return;
        }

        RequestSummary selectedRequest = requestTable.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            clearRequestDetail();
            return;
        }

        RequestDetailRecord detail = requestDetailService.load(currentSession, selectedRequest.requestNumber());
        if (detail == null) {
            clearRequestDetail();
            return;
        }

        currentRequestDetail = detail;
        requestDetailNumberLabel.setText(detail.requestNumber());
        requestDetailStatusLabel.setText(detail.status().displayName());
        requestDetailRouteLabel.setText(detail.sourceWarehouseLabel() + " -> " + detail.destinationWarehouseLabel());
        requestDetailUsersLabel.setText("Requested by " + detail.requestedBy()
                + (detail.approvedBy() == null || detail.approvedBy().isBlank() ? "" : " | Handled by " + detail.approvedBy()));
        requestDetailNoteLabel.setText(detail.note() == null || detail.note().isBlank() ? "No request note." : detail.note());
        requestDetailUpdatedAtLabel.setText("Created " + detail.createdAt().format(TIMESTAMP_FORMATTER)
                + " | Updated " + detail.updatedAt().format(TIMESTAMP_FORMATTER));
        requestDetailLineTable.getItems().setAll(detail.lines());
        requestDetailLineTable.refresh();
        updateRequestActionButtons(DatabaseConnection.canConnect());
    }

    private void clearRequestDetail() {
        currentRequestDetail = null;
        requestDetailNumberLabel.setText("No request selected");
        requestDetailStatusLabel.setText("-");
        requestDetailRouteLabel.setText("-");
        requestDetailUsersLabel.setText("-");
        requestDetailNoteLabel.setText("Select a request to inspect its note and requested items.");
        requestDetailUpdatedAtLabel.setText("-");
        requestDetailLineTable.getItems().clear();
        updateRequestActionButtons(DatabaseConnection.canConnect());
    }

    private void clearWarehouseForm() {
        warehouseCodeField.clear();
        warehouseNameField.clear();
        warehouseCityField.clear();
        warehouseAddressField.clear();
        warehouseActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
    }

    private void clearUserForm() {
        userUsernameField.clear();
        userPasswordField.clear();
        userFullNameField.clear();
        userRoleSelector.getSelectionModel().select(Role.WAREHOUSE_MANAGER);
        if (!userWarehouseSelector.getItems().isEmpty()) {
            WarehouseAssignmentOption option = userWarehouseSelector.getItems().stream()
                    .filter(WarehouseAssignmentOption::isAssignedWarehouse)
                    .findFirst()
                    .orElse(userWarehouseSelector.getItems().get(0));
            if (option != null) {
                userWarehouseSelector.getSelectionModel().select(option);
            } else {
                userWarehouseSelector.getSelectionModel().selectFirst();
            }
        }
        userActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
        refreshUserWarehouseSelectorState();
    }

    private void updateNavigationVisibility() {
        boolean adminVisible = currentSession != null && currentSession.isAdmin();
        adminNavButton.setVisible(adminVisible);
        adminNavButton.setManaged(adminVisible);
        if (!adminVisible && activePage == AppPage.ADMIN) {
            setActivePage(AppPage.OVERVIEW);
        }
    }

    private void setActivePage(AppPage page) {
        if (page == AppPage.ADMIN && currentSession != null && !currentSession.isAdmin()) {
            page = AppPage.OVERVIEW;
        }

        activePage = page;
        if (activePageLabel != null) {
            activePageLabel.setText(page.title);
        }

        setPageVisibility(overviewScrollPane, page == AppPage.OVERVIEW);
        setPageVisibility(operationsScrollPane, page == AppPage.OPERATIONS);
        setPageVisibility(requestsScrollPane, page == AppPage.REQUESTS);
        setPageVisibility(catalogScrollPane, page == AppPage.CATALOG);
        setPageVisibility(reportsScrollPane, page == AppPage.REPORTS);
        setPageVisibility(adminScrollPane, page == AppPage.ADMIN);

        setPageVisibility(overviewPage, page == AppPage.OVERVIEW);
        setPageVisibility(operationsPage, page == AppPage.OPERATIONS);
        setPageVisibility(requestsPage, page == AppPage.REQUESTS);
        setPageVisibility(catalogPage, page == AppPage.CATALOG);
        setPageVisibility(reportsPage, page == AppPage.REPORTS);
        setPageVisibility(adminPage, page == AppPage.ADMIN);

        applyNavButtonState(overviewNavButton, page == AppPage.OVERVIEW);
        applyNavButtonState(operationsNavButton, page == AppPage.OPERATIONS);
        applyNavButtonState(requestsNavButton, page == AppPage.REQUESTS);
        applyNavButtonState(catalogNavButton, page == AppPage.CATALOG);
        applyNavButtonState(reportsNavButton, page == AppPage.REPORTS);
        applyNavButtonState(adminNavButton, page == AppPage.ADMIN);
    }

    private void setPageVisibility(Node pageNode, boolean visible) {
        if (pageNode == null) {
            return;
        }
        pageNode.setVisible(visible);
        pageNode.setManaged(visible);
        pageNode.setMouseTransparent(!visible);
    }

    private void applyNavButtonState(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().remove("nav-button-active");
        button.setScaleX(1.0);
        button.setScaleY(1.0);
        if (active) {
            button.getStyleClass().add("nav-button-active");
            playNavSelectionAnimation(button);
        }
    }

    private void playNavSelectionAnimation(Button button) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(160), button);
        scaleTransition.setFromX(0.96);
        scaleTransition.setFromY(0.96);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.play();
    }

    private void refreshWarehouseFilterOptions() {
        WarehouseFilterOption currentFilter = warehouseFilterSelector.getSelectionModel().getSelectedItem();
        String currentCode = currentFilter == null || currentFilter.warehouse() == null ? null : currentFilter.warehouse().code();

        List<WarehouseFilterOption> options = warehouseManagementService.loadWarehouseFilterOptions();
        suppressWarehouseFilterRefresh = true;
        try {
            warehouseFilterSelector.setItems(FXCollections.observableArrayList(options));

            if (currentCode == null) {
                warehouseFilterSelector.getSelectionModel().selectFirst();
                return;
            }

            options.stream()
                    .filter(option -> option.warehouse() != null && option.warehouse().code().equals(currentCode))
                    .findFirst()
                    .ifPresentOrElse(
                            option -> warehouseFilterSelector.getSelectionModel().select(option),
                            () -> warehouseFilterSelector.getSelectionModel().selectFirst()
                    );
        } finally {
            suppressWarehouseFilterRefresh = false;
        }
    }

    private void applyDefaultWarehouseFilterForSession(boolean force) {
        if (currentSession == null || currentSession.isAdmin()) {
            if (force && warehouseFilterSelector.getSelectionModel().isEmpty() && !warehouseFilterSelector.getItems().isEmpty()) {
                warehouseFilterSelector.getSelectionModel().selectFirst();
            }
            return;
        }

        WarehouseFilterOption currentFilter = warehouseFilterSelector.getSelectionModel().getSelectedItem();
        if (!force && currentFilter != null) {
            return;
        }

        warehouseFilterSelector.getItems().stream()
                .filter(option -> option.warehouse() != null
                        && option.warehouse().code().equals(currentSession.warehouse().code()))
                .findFirst()
                .ifPresent(option -> warehouseFilterSelector.getSelectionModel().select(option));
    }

    private void refreshUserWarehouseSelectorState() {
        Role selectedRole = userRoleSelector.getSelectionModel().getSelectedItem();
        boolean canManageUsers = currentSession != null && currentSession.role().canManageUsers() && DatabaseConnection.canConnect();

        if (selectedRole == Role.ADMIN) {
            WarehouseAssignmentOption globalOption = findWarehouseAssignmentOption(null);
            if (globalOption != null) {
                userWarehouseSelector.getSelectionModel().select(globalOption);
            }
            userWarehouseSelector.setDisable(true);
            return;
        }

        userWarehouseSelector.setDisable(!canManageUsers);
        if (userWarehouseSelector.getSelectionModel().isEmpty() && !userWarehouseSelector.getItems().isEmpty()) {
            userWarehouseSelector.getSelectionModel().selectFirst();
        }
    }

    private WarehouseAssignmentOption findWarehouseAssignmentOption(String warehouseCode) {
        return userWarehouseSelector.getItems().stream()
                .filter(option -> {
                    if (warehouseCode == null) {
                        return option.warehouseCode() == null;
                    }
                    return warehouseCode.equals(option.warehouseCode());
                })
                .findFirst()
                .orElse(null);
    }

    private boolean isActiveSelection(ComboBox<String> selector, boolean defaultValue) {
        String selectedValue = selector.getSelectionModel().getSelectedItem();
        if (selectedValue == null) {
            return defaultValue;
        }
        return ACTIVE_LABEL.equals(selectedValue);
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

    private WarehouseFilterOption getSelectedWarehouseFilter() {
        WarehouseFilterOption filterOption = warehouseFilterSelector.getSelectionModel().getSelectedItem();
        if (filterOption == null) {
            filterOption = warehouseManagementService.loadWarehouseFilterOptions().get(0);
            warehouseFilterSelector.getSelectionModel().select(filterOption);
        }
        return filterOption;
    }
}
