package org.dispatch.model;

public enum Role {
    ADMIN("Администратор"),
    DISPATCHER("Диспетчер");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}