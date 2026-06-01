package com.pcwarehouse.model;

public enum RequestViewMode {
    INCOMING("Incoming", "pending requests sent to this warehouse"),
    OUTGOING("Outgoing", "pending requests created by this user"),
    HISTORY("History", "approved, rejected, cancelled, and completed requests");

    private final String displayName;
    private final String summaryLabel;

    RequestViewMode(String displayName, String summaryLabel) {
        this.displayName = displayName;
        this.summaryLabel = summaryLabel;
    }

    public String displayName() {
        return displayName;
    }

    public String summaryLabel() {
        return summaryLabel;
    }
}
