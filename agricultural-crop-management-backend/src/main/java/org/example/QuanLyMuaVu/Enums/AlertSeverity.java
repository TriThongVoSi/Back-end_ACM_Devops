package org.example.QuanLyMuaVu.Enums;

public enum AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static AlertSeverity fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return AlertSeverity.valueOf(code.trim().toUpperCase());
    }
}
