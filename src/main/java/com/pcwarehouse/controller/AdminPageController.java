package com.pcwarehouse.controller;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.ManagedUserRecord;
import com.pcwarehouse.model.Role;
import com.pcwarehouse.model.UserChangeResult;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.model.WarehouseAssignmentOption;
import com.pcwarehouse.model.WarehouseChangeResult;
import com.pcwarehouse.model.WarehouseRecord;
import com.pcwarehouse.service.UserManagementService;
import com.pcwarehouse.service.WarehouseManagementService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.Supplier;

final class AdminPageController {

    private static final String ACTIVE_LABEL = "Active";
    private static final String INACTIVE_LABEL = "Inactive";

    private final View view;
    private final Supplier<UserSession> sessionSupplier;
    private final Runnable refreshAll;
    private final WarehouseManagementService warehouseManagementService = new WarehouseManagementService();
    private final UserManagementService userManagementService = new UserManagementService();

    AdminPageController(View view, Supplier<UserSession> sessionSupplier, Runnable refreshAll) {
        this.view = view;
        this.sessionSupplier = sessionSupplier;
        this.refreshAll = refreshAll;
    }

    void initialize() {
        configureSelectors();
        configureWarehouseTable();
        configureUserTable();
    }

    void handleWarehouseSearch() {
        refreshWarehousesOnly();
    }

    void handleClearWarehouseSearch() {
        view.warehouseSearchField.clear();
        refreshWarehousesOnly();
    }

    void handleUserSearch() {
        refreshUsersOnly();
    }

    void handleClearUserSearch() {
        view.userSearchField.clear();
        refreshUsersOnly();
    }

    void handleCreateWarehouse() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        WarehouseChangeResult result = warehouseManagementService.createWarehouse(
                session,
                view.warehouseCodeField.getText(),
                view.warehouseNameField.getText(),
                view.warehouseCityField.getText(),
                view.warehouseAddressField.getText()
        );

        if (result.success()) {
            clearWarehouseForm();
            refreshAll.run();
        }
        view.warehouseStatusLabel.setText(result.message());
    }

    void handleUpdateWarehouse() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        WarehouseRecord selectedWarehouse = view.warehouseTable.getSelectionModel().getSelectedItem();
        String originalWarehouseCode = selectedWarehouse == null ? null : selectedWarehouse.warehouseCode();

        WarehouseChangeResult result = warehouseManagementService.updateWarehouse(
                session,
                originalWarehouseCode,
                view.warehouseCodeField.getText(),
                view.warehouseNameField.getText(),
                view.warehouseCityField.getText(),
                view.warehouseAddressField.getText(),
                isActiveSelection(view.warehouseActiveSelector, true)
        );

        if (result.success()) {
            refreshAll.run();
        }
        view.warehouseStatusLabel.setText(result.message());
    }

    void handleDeleteWarehouse() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        WarehouseChangeResult result = warehouseManagementService.deleteWarehouse(
                session,
                view.warehouseTable.getSelectionModel().getSelectedItem()
        );

        if (result.success()) {
            clearWarehouseForm();
            refreshAll.run();
        }
        view.warehouseStatusLabel.setText(result.message());
    }

    void handleForceDeleteWarehouse() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        WarehouseRecord selectedWarehouse = view.warehouseTable.getSelectionModel().getSelectedItem();
        if (selectedWarehouse == null) {
            view.warehouseStatusLabel.setText("Select a warehouse row to force delete.");
            return;
        }
        if (!UiDialogs.confirmForceDelete(
                "Force Delete Warehouse",
                "Force delete warehouse " + selectedWarehouse.warehouseCode() + "?",
                "This also deletes assigned users, inventory, requests, and stock movement history connected to the warehouse."
        )) {
            view.warehouseStatusLabel.setText("Force delete cancelled.");
            return;
        }

        WarehouseChangeResult result = warehouseManagementService.forceDeleteWarehouse(session, selectedWarehouse);

        if (result.success()) {
            clearWarehouseForm();
            refreshAll.run();
        }
        view.warehouseStatusLabel.setText(result.message());
    }

    void handleClearWarehouseForm() {
        clearWarehouseForm();
        view.warehouseTable.getSelectionModel().clearSelection();
        view.warehouseStatusLabel.setText("Warehouse form cleared.");
    }

    void handleCreateUser() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        UserChangeResult result = userManagementService.createUser(
                session,
                view.userUsernameField.getText(),
                view.userPasswordField.getText(),
                view.userFullNameField.getText(),
                view.userRoleSelector.getSelectionModel().getSelectedItem(),
                view.userWarehouseSelector.getSelectionModel().getSelectedItem(),
                isActiveSelection(view.userActiveSelector, true)
        );

        if (result.success()) {
            clearUserForm();
            refreshAll.run();
        }
        view.userStatusLabel.setText(result.message());
    }

    void handleUpdateUser() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        ManagedUserRecord selectedUser = view.userTable.getSelectionModel().getSelectedItem();
        String originalUsername = selectedUser == null ? null : selectedUser.username();

        UserChangeResult result = userManagementService.updateUser(
                session,
                originalUsername,
                view.userUsernameField.getText(),
                view.userPasswordField.getText(),
                view.userFullNameField.getText(),
                view.userRoleSelector.getSelectionModel().getSelectedItem(),
                view.userWarehouseSelector.getSelectionModel().getSelectedItem(),
                isActiveSelection(view.userActiveSelector, true)
        );

        if (result.success()) {
            refreshAll.run();
        }
        view.userStatusLabel.setText(result.message());
    }

    void handleDeleteUser() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        UserChangeResult result = userManagementService.deleteUser(
                session,
                view.userTable.getSelectionModel().getSelectedItem()
        );

        if (result.success()) {
            clearUserForm();
            refreshAll.run();
        }
        view.userStatusLabel.setText(result.message());
    }

    void handleForceDeleteUser() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }

        ManagedUserRecord selectedUser = view.userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            view.userStatusLabel.setText("Select a user row to force delete.");
            return;
        }
        if (!UiDialogs.confirmForceDelete(
                "Force Delete User",
                "Force delete user " + selectedUser.username() + "?",
                "This also deletes requests and stock movement history connected to the user."
        )) {
            view.userStatusLabel.setText("Force delete cancelled.");
            return;
        }

        UserChangeResult result = userManagementService.forceDeleteUser(session, selectedUser);

        if (result.success()) {
            clearUserForm();
            refreshAll.run();
        }
        view.userStatusLabel.setText(result.message());
    }

    void handleClearUserForm() {
        clearUserForm();
        view.userTable.getSelectionModel().clearSelection();
        view.userStatusLabel.setText("User form cleared.");
    }

    void refreshWarehousesOnly() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }
        updateWarehouseControls(session, DatabaseConnection.canConnect());
    }

    void refreshUsersOnly() {
        UserSession session = sessionSupplier.get();
        if (session == null) {
            return;
        }
        updateUserControls(session, DatabaseConnection.canConnect());
    }

    void updateControls(UserSession session, boolean usingLiveDatabase) {
        updateWarehouseControls(session, usingLiveDatabase);
        updateUserControls(session, usingLiveDatabase);
    }

    List<com.pcwarehouse.model.WarehouseFilterOption> loadWarehouseFilterOptions() {
        return warehouseManagementService.loadWarehouseFilterOptions();
    }

    private void configureSelectors() {
        view.warehouseActiveSelector.setItems(FXCollections.observableArrayList(ACTIVE_LABEL, INACTIVE_LABEL));
        view.warehouseActiveSelector.getSelectionModel().select(ACTIVE_LABEL);

        view.userRoleSelector.setItems(FXCollections.observableArrayList(Role.values()));
        view.userRoleSelector.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        });
        view.userRoleSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        });
        view.userRoleSelector.getSelectionModel().select(Role.WAREHOUSE_MANAGER);
        view.userRoleSelector.setOnAction(event -> refreshUserWarehouseSelectorState());

        view.userWarehouseSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(WarehouseAssignmentOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public WarehouseAssignmentOption fromString(String string) {
                return null;
            }
        });

        view.userActiveSelector.setItems(FXCollections.observableArrayList(ACTIVE_LABEL, INACTIVE_LABEL));
        view.userActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
    }

    private void configureWarehouseTable() {
        view.warehouseCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseCode()));
        view.warehouseNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseName()));
        view.warehouseCityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().city()));
        view.warehouseAddressColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().addressLine()));
        view.warehouseActiveColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().active() ? "Yes" : "No"));
        view.warehouseTable.setPlaceholder(new Label("No warehouses match the current search."));
        view.warehouseTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateWarehouseForm(newValue));
    }

    private void configureUserTable() {
        view.userUsernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().username()));
        view.userFullNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().fullName()));
        view.userRoleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().role().displayName()));
        view.userWarehouseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().warehouseLabel()));
        view.userActiveColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().active() ? "Yes" : "No"));
        view.userTable.setPlaceholder(new Label("No users match the current search."));
        view.userTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> populateUserForm(newValue));
    }

    private void updateWarehouseControls(UserSession session, boolean usingLiveDatabase) {
        WarehouseRecord selectedWarehouse = view.warehouseTable.getSelectionModel().getSelectedItem();
        String selectedWarehouseCode = selectedWarehouse == null ? null : selectedWarehouse.warehouseCode();
        String selectedWarehouseActive = view.warehouseActiveSelector.getSelectionModel().getSelectedItem();

        List<WarehouseRecord> warehouses = warehouseManagementService.loadWarehouses(view.warehouseSearchField.getText());
        view.warehouseTable.getItems().setAll(warehouses);

        if (selectedWarehouseCode != null) {
            warehouses.stream()
                    .filter(warehouse -> warehouse.warehouseCode().equals(selectedWarehouseCode))
                    .findFirst()
                    .ifPresent(warehouse -> view.warehouseTable.getSelectionModel().select(warehouse));
        }

        if (selectedWarehouseActive != null) {
            view.warehouseActiveSelector.getSelectionModel().select(selectedWarehouseActive);
        } else if (view.warehouseActiveSelector.getSelectionModel().isEmpty()) {
            view.warehouseActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
        }

        boolean canManageWarehouses = session.isAdmin() && usingLiveDatabase;
        view.warehouseCodeField.setDisable(!canManageWarehouses);
        view.warehouseNameField.setDisable(!canManageWarehouses);
        view.warehouseCityField.setDisable(!canManageWarehouses);
        view.warehouseAddressField.setDisable(!canManageWarehouses);
        view.warehouseActiveSelector.setDisable(!canManageWarehouses);
        view.createWarehouseButton.setDisable(!canManageWarehouses);
        view.updateWarehouseButton.setDisable(!canManageWarehouses);
        view.deleteWarehouseButton.setDisable(!canManageWarehouses);
        view.forceDeleteWarehouseButton.setDisable(!canManageWarehouses);

        view.warehouseSummaryLabel.setText(warehouses.size() + " warehouses shown");
        if (!usingLiveDatabase) {
            view.warehouseStatusLabel.setText("Warehouse edits need a live database connection.");
        } else if (!canManageWarehouses) {
            view.warehouseStatusLabel.setText("Warehouse administration is read-only for this session.");
        } else if (view.warehouseStatusLabel.getText() == null || view.warehouseStatusLabel.getText().isBlank()) {
            view.warehouseStatusLabel.setText("Search warehouses or select a row to update it.");
        }
    }

    private void updateUserControls(UserSession session, boolean usingLiveDatabase) {
        ManagedUserRecord selectedUser = view.userTable.getSelectionModel().getSelectedItem();
        String selectedUsername = selectedUser == null ? null : selectedUser.username();
        Role selectedRole = view.userRoleSelector.getSelectionModel().getSelectedItem();
        WarehouseAssignmentOption selectedWarehouse = view.userWarehouseSelector.getSelectionModel().getSelectedItem();
        String selectedActive = view.userActiveSelector.getSelectionModel().getSelectedItem();

        List<ManagedUserRecord> users = userManagementService.loadUsers(view.userSearchField.getText());
        List<WarehouseAssignmentOption> warehouseAssignments = userManagementService.loadWarehouseAssignments();

        view.userTable.getItems().setAll(users);
        view.userWarehouseSelector.setItems(FXCollections.observableArrayList(warehouseAssignments));

        if (selectedUsername != null) {
            users.stream()
                    .filter(user -> user.username().equals(selectedUsername))
                    .findFirst()
                    .ifPresent(user -> view.userTable.getSelectionModel().select(user));
        }

        if (selectedRole != null) {
            view.userRoleSelector.getSelectionModel().select(selectedRole);
        } else if (view.userRoleSelector.getSelectionModel().isEmpty()) {
            view.userRoleSelector.getSelectionModel().select(Role.WAREHOUSE_MANAGER);
        }

        WarehouseAssignmentOption targetWarehouseOption = selectedWarehouse;
        ManagedUserRecord selectedTableUser = view.userTable.getSelectionModel().getSelectedItem();
        if (selectedTableUser != null) {
            targetWarehouseOption = findWarehouseAssignmentOption(selectedTableUser.warehouseCode());
        } else if (selectedWarehouse != null) {
            targetWarehouseOption = findWarehouseAssignmentOption(selectedWarehouse.warehouseCode());
        }

        if (targetWarehouseOption != null) {
            view.userWarehouseSelector.getSelectionModel().select(targetWarehouseOption);
        } else if (!warehouseAssignments.isEmpty()) {
            view.userWarehouseSelector.getSelectionModel().selectFirst();
        }

        if (selectedActive != null) {
            view.userActiveSelector.getSelectionModel().select(selectedActive);
        } else if (view.userActiveSelector.getSelectionModel().isEmpty()) {
            view.userActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
        }

        boolean canManageUsers = session.role().canManageUsers() && usingLiveDatabase;
        view.userUsernameField.setDisable(!canManageUsers);
        view.userPasswordField.setDisable(!canManageUsers);
        view.userFullNameField.setDisable(!canManageUsers);
        view.userRoleSelector.setDisable(!canManageUsers);
        view.userActiveSelector.setDisable(!canManageUsers);
        view.createUserButton.setDisable(!canManageUsers);
        view.updateUserButton.setDisable(!canManageUsers);
        view.deleteUserButton.setDisable(!canManageUsers);
        view.forceDeleteUserButton.setDisable(!canManageUsers);
        refreshUserWarehouseSelectorState();

        view.userSummaryLabel.setText(users.size() + " users shown");
        if (!usingLiveDatabase) {
            view.userStatusLabel.setText("User edits need a live database connection.");
        } else if (!canManageUsers) {
            view.userStatusLabel.setText("User administration is read-only for this session.");
        } else if (view.userStatusLabel.getText() == null || view.userStatusLabel.getText().isBlank()) {
            view.userStatusLabel.setText("Search users or select a row to update it. Leave password blank to keep it unchanged.");
        }
    }

    private void populateWarehouseForm(WarehouseRecord warehouse) {
        if (warehouse == null) {
            return;
        }

        view.warehouseCodeField.setText(warehouse.warehouseCode());
        view.warehouseNameField.setText(warehouse.warehouseName());
        view.warehouseCityField.setText(warehouse.city());
        view.warehouseAddressField.setText(warehouse.addressLine());
        view.warehouseActiveSelector.getSelectionModel().select(warehouse.active() ? ACTIVE_LABEL : INACTIVE_LABEL);
    }

    private void populateUserForm(ManagedUserRecord user) {
        if (user == null) {
            return;
        }

        view.userUsernameField.setText(user.username());
        view.userPasswordField.clear();
        view.userFullNameField.setText(user.fullName());
        view.userRoleSelector.getSelectionModel().select(user.role());
        WarehouseAssignmentOption option = findWarehouseAssignmentOption(user.warehouseCode());
        if (option != null) {
            view.userWarehouseSelector.getSelectionModel().select(option);
        }
        view.userActiveSelector.getSelectionModel().select(user.active() ? ACTIVE_LABEL : INACTIVE_LABEL);
        refreshUserWarehouseSelectorState();
    }

    private void clearWarehouseForm() {
        view.warehouseCodeField.clear();
        view.warehouseNameField.clear();
        view.warehouseCityField.clear();
        view.warehouseAddressField.clear();
        view.warehouseActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
    }

    private void clearUserForm() {
        view.userUsernameField.clear();
        view.userPasswordField.clear();
        view.userFullNameField.clear();
        view.userRoleSelector.getSelectionModel().select(Role.WAREHOUSE_MANAGER);
        if (!view.userWarehouseSelector.getItems().isEmpty()) {
            WarehouseAssignmentOption option = view.userWarehouseSelector.getItems().stream()
                    .filter(WarehouseAssignmentOption::isAssignedWarehouse)
                    .findFirst()
                    .orElse(view.userWarehouseSelector.getItems().get(0));
            if (option != null) {
                view.userWarehouseSelector.getSelectionModel().select(option);
            } else {
                view.userWarehouseSelector.getSelectionModel().selectFirst();
            }
        }
        view.userActiveSelector.getSelectionModel().select(ACTIVE_LABEL);
        refreshUserWarehouseSelectorState();
    }

    private void refreshUserWarehouseSelectorState() {
        UserSession session = sessionSupplier.get();
        Role selectedRole = view.userRoleSelector.getSelectionModel().getSelectedItem();
        boolean canManageUsers = session != null && session.role().canManageUsers() && DatabaseConnection.canConnect();

        if (selectedRole == Role.ADMIN) {
            WarehouseAssignmentOption globalOption = findWarehouseAssignmentOption(null);
            if (globalOption != null) {
                view.userWarehouseSelector.getSelectionModel().select(globalOption);
            }
            view.userWarehouseSelector.setDisable(true);
            return;
        }

        view.userWarehouseSelector.setDisable(!canManageUsers);
        if (view.userWarehouseSelector.getSelectionModel().isEmpty() && !view.userWarehouseSelector.getItems().isEmpty()) {
            view.userWarehouseSelector.getSelectionModel().selectFirst();
        }
    }

    private WarehouseAssignmentOption findWarehouseAssignmentOption(String warehouseCode) {
        return view.userWarehouseSelector.getItems().stream()
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

    record View(
            TextField warehouseSearchField,
            Label warehouseSummaryLabel,
            Label warehouseStatusLabel,
            TextField warehouseCodeField,
            TextField warehouseNameField,
            TextField warehouseCityField,
            TextField warehouseAddressField,
            ComboBox<String> warehouseActiveSelector,
            Button createWarehouseButton,
            Button updateWarehouseButton,
            Button deleteWarehouseButton,
            Button forceDeleteWarehouseButton,
            TableView<WarehouseRecord> warehouseTable,
            TableColumn<WarehouseRecord, String> warehouseCodeColumn,
            TableColumn<WarehouseRecord, String> warehouseNameColumn,
            TableColumn<WarehouseRecord, String> warehouseCityColumn,
            TableColumn<WarehouseRecord, String> warehouseAddressColumn,
            TableColumn<WarehouseRecord, String> warehouseActiveColumn,
            TextField userSearchField,
            Label userSummaryLabel,
            Label userStatusLabel,
            TextField userUsernameField,
            TextField userPasswordField,
            TextField userFullNameField,
            ComboBox<Role> userRoleSelector,
            ComboBox<WarehouseAssignmentOption> userWarehouseSelector,
            ComboBox<String> userActiveSelector,
            Button createUserButton,
            Button updateUserButton,
            Button deleteUserButton,
            Button forceDeleteUserButton,
            TableView<ManagedUserRecord> userTable,
            TableColumn<ManagedUserRecord, String> userUsernameColumn,
            TableColumn<ManagedUserRecord, String> userFullNameColumn,
            TableColumn<ManagedUserRecord, String> userRoleColumn,
            TableColumn<ManagedUserRecord, String> userWarehouseColumn,
            TableColumn<ManagedUserRecord, String> userActiveColumn
    ) {
    }
}
