package com.pcwarehouse.controller;

import com.pcwarehouse.model.CatalogLookupRecord;
import com.pcwarehouse.model.DashboardMetric;
import com.pcwarehouse.model.DashboardSnapshot;
import com.pcwarehouse.model.InventoryReportRow;
import com.pcwarehouse.model.InventorySummary;
import com.pcwarehouse.model.ManagedUserRecord;
import com.pcwarehouse.model.MovementLogEntry;
import com.pcwarehouse.model.ProductOption;
import com.pcwarehouse.model.ProductRecord;
import com.pcwarehouse.model.RequestDetailLine;
import com.pcwarehouse.model.RequestDraftLine;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.RequestSummary;
import com.pcwarehouse.model.StockOperationLine;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.Warehouse;
import com.pcwarehouse.model.WarehouseAssignmentOption;
import com.pcwarehouse.model.WarehouseFilterOption;
import com.pcwarehouse.model.WarehouseRecord;
import com.pcwarehouse.service.DashboardDataService;
import com.pcwarehouse.service.WarehouseManagementService;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DashboardDataService dashboardDataService = new DashboardDataService();
    private final WarehouseManagementService warehouseManagementService = new WarehouseManagementService();

    private CatalogPageController catalogPageController;
    private AdminPageController adminPageController;
    private ReportsPageController reportsPageController;
    private OperationsPageController operationsPageController;
    private RequestsPageController requestsPageController;
    private UserSession currentSession;
    private Runnable logoutHandler;
    private boolean suppressWarehouseFilterRefresh;
    private AppPage activePage = AppPage.OVERVIEW;

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
    private Button forceDeleteProductButton;

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
    private Button forceDeleteCategoryButton;

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
    private Button forceDeleteManufacturerButton;

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
    private Button forceDeleteWarehouseButton;

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
    private Button forceDeleteUserButton;

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
        configureCatalogPageController();
        configureAdminPageController();
        configureOperationsPageController();
        configureRequestsPageController();
        configureReportsPageController();
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
        catalogPageController.handleProductSearch();
    }

    @FXML
    private void handleClearProductSearch() {
        catalogPageController.handleClearProductSearch();
    }

    @FXML
    private void handleInventorySearch() {
        requestsPageController.handleInventorySearch();
    }

    @FXML
    private void handleClearInventoryFilters() {
        requestsPageController.handleClearInventoryFilters();
    }

    @FXML
    private void handleCategorySearch() {
        catalogPageController.handleCategorySearch();
    }

    @FXML
    private void handleClearCategorySearch() {
        catalogPageController.handleClearCategorySearch();
    }

    @FXML
    private void handleManufacturerSearch() {
        catalogPageController.handleManufacturerSearch();
    }

    @FXML
    private void handleClearManufacturerSearch() {
        catalogPageController.handleClearManufacturerSearch();
    }

    @FXML
    private void handleRefreshReport() {
        reportsPageController.handleRefreshReport();
    }

    @FXML
    private void handleClearReportFilters() {
        reportsPageController.handleClearReportFilters();
    }

    @FXML
    private void handleApplyMovementFilters() {
        requestsPageController.handleApplyMovementFilters();
    }

    @FXML
    private void handleClearMovementFilters() {
        requestsPageController.handleClearMovementFilters();
    }

    @FXML
    private void handleWarehouseSearch() {
        adminPageController.handleWarehouseSearch();
    }

    @FXML
    private void handleClearWarehouseSearch() {
        adminPageController.handleClearWarehouseSearch();
    }

    @FXML
    private void handleUserSearch() {
        adminPageController.handleUserSearch();
    }

    @FXML
    private void handleClearUserSearch() {
        adminPageController.handleClearUserSearch();
    }

    @FXML
    private void handleAddRequestLine() {
        requestsPageController.handleAddRequestLine();
    }

    @FXML
    private void handleRemoveRequestLine() {
        requestsPageController.handleRemoveRequestLine();
    }

    @FXML
    private void handleClearRequestDraft() {
        requestsPageController.handleClearRequestDraft();
    }

    @FXML
    private void handleShowIncomingRequests() {
        requestsPageController.handleShowIncomingRequests();
    }

    @FXML
    private void handleShowOutgoingRequests() {
        requestsPageController.handleShowOutgoingRequests();
    }

    @FXML
    private void handleShowRequestHistory() {
        requestsPageController.handleShowRequestHistory();
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
        catalogPageController.handleCreateProduct();
    }

    @FXML
    private void handleUpdateProduct() {
        catalogPageController.handleUpdateProduct();
    }

    @FXML
    private void handleDeleteProduct() {
        catalogPageController.handleDeleteProduct();
    }

    @FXML
    private void handleForceDeleteProduct() {
        catalogPageController.handleForceDeleteProduct();
    }

    @FXML
    private void handleClearProductForm() {
        catalogPageController.handleClearProductForm();
    }

    @FXML
    private void handleCreateCategory() {
        catalogPageController.handleCreateCategory();
    }

    @FXML
    private void handleUpdateCategory() {
        catalogPageController.handleUpdateCategory();
    }

    @FXML
    private void handleDeleteCategory() {
        catalogPageController.handleDeleteCategory();
    }

    @FXML
    private void handleForceDeleteCategory() {
        catalogPageController.handleForceDeleteCategory();
    }

    @FXML
    private void handleClearCategoryForm() {
        catalogPageController.handleClearCategoryForm();
    }

    @FXML
    private void handleCreateManufacturer() {
        catalogPageController.handleCreateManufacturer();
    }

    @FXML
    private void handleUpdateManufacturer() {
        catalogPageController.handleUpdateManufacturer();
    }

    @FXML
    private void handleDeleteManufacturer() {
        catalogPageController.handleDeleteManufacturer();
    }

    @FXML
    private void handleForceDeleteManufacturer() {
        catalogPageController.handleForceDeleteManufacturer();
    }

    @FXML
    private void handleClearManufacturerForm() {
        catalogPageController.handleClearManufacturerForm();
    }

    @FXML
    private void handleCreateWarehouse() {
        adminPageController.handleCreateWarehouse();
    }

    @FXML
    private void handleUpdateWarehouse() {
        adminPageController.handleUpdateWarehouse();
    }

    @FXML
    private void handleDeleteWarehouse() {
        adminPageController.handleDeleteWarehouse();
    }

    @FXML
    private void handleForceDeleteWarehouse() {
        adminPageController.handleForceDeleteWarehouse();
    }

    @FXML
    private void handleClearWarehouseForm() {
        adminPageController.handleClearWarehouseForm();
    }

    @FXML
    private void handleCreateUser() {
        adminPageController.handleCreateUser();
    }

    @FXML
    private void handleUpdateUser() {
        adminPageController.handleUpdateUser();
    }

    @FXML
    private void handleDeleteUser() {
        adminPageController.handleDeleteUser();
    }

    @FXML
    private void handleForceDeleteUser() {
        adminPageController.handleForceDeleteUser();
    }

    @FXML
    private void handleClearUserForm() {
        adminPageController.handleClearUserForm();
    }

    @FXML
    private void handleToggleNewSupplyProduct() {
        operationsPageController.handleToggleNewSupplyProduct();
    }

    @FXML
    private void handleAddSupplyLine() {
        operationsPageController.handleAddSupplyLine();
    }

    @FXML
    private void handleRemoveSupplyLine() {
        operationsPageController.handleRemoveSupplyLine();
    }

    @FXML
    private void handleClearSupplyDraft() {
        operationsPageController.handleClearSupplyDraft();
    }

    @FXML
    private void handleRecordSupply() {
        operationsPageController.handleRecordSupply();
    }

    @FXML
    private void handleAddDispatchLine() {
        operationsPageController.handleAddDispatchLine();
    }

    @FXML
    private void handleRemoveDispatchLine() {
        operationsPageController.handleRemoveDispatchLine();
    }

    @FXML
    private void handleClearDispatchDraft() {
        operationsPageController.handleClearDispatchDraft();
    }

    @FXML
    private void handleRecordDispatch() {
        operationsPageController.handleRecordDispatch();
    }

    @FXML
    private void handleCreateRequest() {
        requestsPageController.handleCreateRequest();
    }

    @FXML
    private void handleApproveRequest() {
        requestsPageController.handleApproveRequest();
    }

    @FXML
    private void handleRejectRequest() {
        requestsPageController.handleRejectRequest();
    }

    @FXML
    private void handleEditSelectedRequest() {
        requestsPageController.handleEditSelectedRequest();
    }

    @FXML
    private void handleSaveRequestEdits() {
        requestsPageController.handleSaveRequestEdits();
    }

    @FXML
    private void handleCancelSelectedRequest() {
        requestsPageController.handleCancelSelectedRequest();
    }

    private void configureSelectors() {
        refreshWarehouseFilterOptions();
        warehouseFilterSelector.setOnAction(event -> {
            if (!suppressWarehouseFilterRefresh && currentSession != null) {
                refreshDashboard();
            }
        });
    }

    private void configureCatalogPageController() {
        catalogPageController = new CatalogPageController(
                new CatalogPageController.View(
                        productSearchField,
                        productSummaryLabel,
                        productStatusLabel,
                        productCodeField,
                        productModelField,
                        productManufacturerSelector,
                        productCategorySelector,
                        productUnitField,
                        productDescriptionField,
                        createProductButton,
                        updateProductButton,
                        deleteProductButton,
                        forceDeleteProductButton,
                        productTable,
                        productCodeColumn,
                        productModelColumn,
                        productManufacturerColumn,
                        productCategoryColumn,
                        productUnitColumn,
                        productActiveColumn,
                        categorySearchField,
                        categorySummaryLabel,
                        categoryStatusLabel,
                        categoryNameField,
                        createCategoryButton,
                        updateCategoryButton,
                        deleteCategoryButton,
                        forceDeleteCategoryButton,
                        categoryTable,
                        categoryNameColumn,
                        manufacturerSearchField,
                        manufacturerSummaryLabel,
                        manufacturerStatusLabel,
                        manufacturerNameField,
                        createManufacturerButton,
                        updateManufacturerButton,
                        deleteManufacturerButton,
                        forceDeleteManufacturerButton,
                        manufacturerTable,
                        manufacturerNameColumn
                ),
                () -> currentSession,
                this::refreshDashboard
        );
        catalogPageController.initialize();
    }

    private void configureAdminPageController() {
        adminPageController = new AdminPageController(
                new AdminPageController.View(
                        warehouseSearchField,
                        warehouseSummaryLabel,
                        warehouseStatusLabel,
                        warehouseCodeField,
                        warehouseNameField,
                        warehouseCityField,
                        warehouseAddressField,
                        warehouseActiveSelector,
                        createWarehouseButton,
                        updateWarehouseButton,
                        deleteWarehouseButton,
                        forceDeleteWarehouseButton,
                        warehouseTable,
                        warehouseCodeColumn,
                        warehouseNameColumn,
                        warehouseCityColumn,
                        warehouseAddressColumn,
                        warehouseActiveColumn,
                        userSearchField,
                        userSummaryLabel,
                        userStatusLabel,
                        userUsernameField,
                        userPasswordField,
                        userFullNameField,
                        userRoleSelector,
                        userWarehouseSelector,
                        userActiveSelector,
                        createUserButton,
                        updateUserButton,
                        deleteUserButton,
                        forceDeleteUserButton,
                        userTable,
                        userUsernameColumn,
                        userFullNameColumn,
                        userRoleColumn,
                        userWarehouseColumn,
                        userActiveColumn
                ),
                () -> currentSession,
                this::refreshDashboard
        );
        adminPageController.initialize();
    }

    private void configureReportsPageController() {
        reportsPageController = new ReportsPageController(
                new ReportsPageController.View(
                        reportWarehouseSelector,
                        reportCategorySelector,
                        reportManufacturerSelector,
                        reportSortSelector,
                        reportSummaryLabel,
                        reportTable,
                        reportWarehouseColumn,
                        reportCodeColumn,
                        reportProductColumn,
                        reportManufacturerColumn,
                        reportCategoryColumn,
                        reportQuantityColumn
                ),
                () -> currentSession
        );
        reportsPageController.initialize();
    }

    private void configureOperationsPageController() {
        operationsPageController = new OperationsPageController(
                new OperationsPageController.View(
                        supplyWarehouseSelector,
                        supplyProductSelector,
                        supplyNewProductCheckBox,
                        supplyNewProductFields,
                        supplyNewProductCodeField,
                        supplyNewProductNameField,
                        supplyNewProductUnitField,
                        supplyNewProductManufacturerSelector,
                        supplyNewProductCategorySelector,
                        supplyNewProductDescriptionField,
                        supplyQuantityField,
                        supplySupplierField,
                        supplyNoteField,
                        addSupplyLineButton,
                        removeSupplyLineButton,
                        clearSupplyDraftButton,
                        recordSupplyButton,
                        supplyStatusLabel,
                        supplyDraftTable,
                        supplyDraftCodeColumn,
                        supplyDraftProductColumn,
                        supplyDraftQuantityColumn,
                        dispatchWarehouseSelector,
                        dispatchInventorySearchField,
                        dispatchInventorySummaryLabel,
                        dispatchInventoryTable,
                        dispatchInventoryCodeColumn,
                        dispatchInventoryProductColumn,
                        dispatchInventoryManufacturerColumn,
                        dispatchInventoryCategoryColumn,
                        dispatchInventoryQuantityColumn,
                        dispatchQuantityField,
                        dispatchDestinationField,
                        dispatchNoteField,
                        addDispatchLineButton,
                        removeDispatchLineButton,
                        clearDispatchDraftButton,
                        recordDispatchButton,
                        dispatchStatusLabel,
                        dispatchDraftTable,
                        dispatchDraftCodeColumn,
                        dispatchDraftProductColumn,
                        dispatchDraftQuantityColumn
                ),
                () -> currentSession,
                this::refreshDashboard
        );
        operationsPageController.initialize();
    }

    private void configureRequestsPageController() {
        requestsPageController = new RequestsPageController(
                new RequestsPageController.View(
                        inventorySummaryLabel,
                        inventorySearchField,
                        inventoryCategoryFilterSelector,
                        inventoryManufacturerFilterSelector,
                        requestSummaryLabel,
                        requestIncomingViewButton,
                        requestOutgoingViewButton,
                        requestHistoryViewButton,
                        movementLogSummaryLabel,
                        movementTypeFilterSelector,
                        movementFromDatePicker,
                        movementToDatePicker,
                        inventoryTable,
                        inventoryWarehouseColumn,
                        inventoryCodeColumn,
                        inventoryProductColumn,
                        inventoryManufacturerColumn,
                        inventoryCategoryColumn,
                        inventoryQuantityColumn,
                        requestSourceWarehouseSelector,
                        requestTargetWarehouseSelector,
                        requestInventorySearchField,
                        requestInventorySummaryLabel,
                        requestInventoryTable,
                        requestInventoryCodeColumn,
                        requestInventoryProductColumn,
                        requestInventoryManufacturerColumn,
                        requestInventoryCategoryColumn,
                        requestInventoryQuantityColumn,
                        requestQuantityField,
                        requestNoteField,
                        requestEditorTitleLabel,
                        createRequestButton,
                        saveRequestEditsButton,
                        approveRequestButton,
                        rejectRequestButton,
                        editSelectedRequestButton,
                        cancelSelectedRequestButton,
                        requestActionStatusLabel,
                        requestDraftTable,
                        requestDraftCodeColumn,
                        requestDraftProductColumn,
                        requestDraftQuantityColumn,
                        addRequestLineButton,
                        removeRequestLineButton,
                        clearRequestDraftButton,
                        requestTable,
                        requestNumberColumn,
                        requestSourceColumn,
                        requestDestinationColumn,
                        requestRequestedByColumn,
                        requestStatusColumn,
                        requestLineCountColumn,
                        requestTotalQuantityColumn,
                        requestCreatedAtColumn,
                        requestDetailNumberLabel,
                        requestDetailStatusLabel,
                        requestDetailRouteLabel,
                        requestDetailUsersLabel,
                        requestDetailNoteLabel,
                        requestDetailUpdatedAtLabel,
                        requestDetailLineTable,
                        requestDetailCodeColumn,
                        requestDetailProductColumn,
                        requestDetailRequestedColumn,
                        requestDetailApprovedColumn,
                        movementLogTable,
                        timestampColumn,
                        movementTypeColumn,
                        productColumn,
                        manufacturerColumn,
                        quantityColumn,
                        sourceWarehouseColumn,
                        destinationWarehouseColumn,
                        userColumn,
                        detailsColumn
                ),
                () -> currentSession,
                this::refreshDashboard
        );
        requestsPageController.initialize();
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
        catalogPageController.updateControls(currentSession, snapshot.usingLiveDatabase());
        adminPageController.updateControls(currentSession, snapshot.usingLiveDatabase());
        operationsPageController.updateControls(currentSession, snapshot.usingLiveDatabase());
        requestsPageController.updateControls(currentSession, snapshot.usingLiveDatabase());
        requestsPageController.refreshInventoryOnly();
        requestsPageController.refreshRequestsOnly();
        requestsPageController.updateMovementLogs(snapshot.movementLogEntries(), filterOption);
        reportsPageController.updateControls();
        reportsPageController.updateView();

        refreshStatusLabel.setText("Last refresh: " + TIMESTAMP_FORMATTER.format(LocalDateTime.now())
                + " | Manual refresh only");
    }

    private void refreshProductsOnly() {
        catalogPageController.refreshProductsOnly();
    }

    private void refreshInventoryOnly() {
        requestsPageController.refreshInventoryOnly();
    }

    private void refreshCategoriesOnly() {
        catalogPageController.refreshCategoriesOnly();
    }

    private void refreshManufacturersOnly() {
        catalogPageController.refreshManufacturersOnly();
    }

    private void refreshReportOnly() {
        reportsPageController.refreshReportOnly();
    }

    private void refreshWarehousesOnly() {
        adminPageController.refreshWarehousesOnly();
    }

    private void refreshUsersOnly() {
        adminPageController.refreshUsersOnly();
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

    private void refreshRequestsOnly() {
        requestsPageController.refreshRequestsOnly();
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

    private WarehouseFilterOption getSelectedWarehouseFilter() {
        WarehouseFilterOption filterOption = warehouseFilterSelector.getSelectionModel().getSelectedItem();
        if (filterOption == null) {
            filterOption = warehouseManagementService.loadWarehouseFilterOptions().get(0);
            warehouseFilterSelector.getSelectionModel().select(filterOption);
        }
        return filterOption;
    }
}
