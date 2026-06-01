package com.pcwarehouse.model;

public enum Role {
    ADMIN("Admin", true, true, true, true, true),
    WAREHOUSE_MANAGER("Warehouse Manager", false, true, true, true, true),
    WAREHOUSE_STAFF("Warehouse Staff", false, false, true, true, true),
    VIEWER("Viewer", false, false, false, false, false);

    private final String displayName;
    private final boolean canManageUsers;
    private final boolean canApproveRequests;
    private final boolean canSupply;
    private final boolean canDispatch;
    private final boolean canCreateRequests;

    Role(
            String displayName,
            boolean canManageUsers,
            boolean canApproveRequests,
            boolean canSupply,
            boolean canDispatch,
            boolean canCreateRequests
    ) {
        this.displayName = displayName;
        this.canManageUsers = canManageUsers;
        this.canApproveRequests = canApproveRequests;
        this.canSupply = canSupply;
        this.canDispatch = canDispatch;
        this.canCreateRequests = canCreateRequests;
    }

    public String displayName() {
        return displayName;
    }

    public boolean canManageUsers() {
        return canManageUsers;
    }

    public boolean canApproveRequests() {
        return canApproveRequests;
    }

    public boolean canSupply() {
        return canSupply;
    }

    public boolean canDispatch() {
        return canDispatch;
    }

    public boolean canCreateRequests() {
        return canCreateRequests;
    }
}
