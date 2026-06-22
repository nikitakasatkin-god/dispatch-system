package org.dispatch.model;

public enum SyncStatus {
    PENDING("Ожидает отправки"),
    SYNCED("Синхронизирован");

    private final String displayName;

    SyncStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}