package org.example.QuanLyMuaVu.Enums;

public enum AlertType {
    INVENTORY_EXPIRING,
    INVENTORY_EXPIRED,
    TASK_OVERDUE,
    BUDGET_OVERSPEND,
    INCIDENT_OPEN;

    public static AlertType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return AlertType.valueOf(code.trim().toUpperCase());
    }
}
