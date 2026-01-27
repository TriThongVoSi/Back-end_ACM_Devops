package org.example.QuanLyMuaVu.Enums;

public enum AlertStatus {
    NEW,
    SENT,
    ACKNOWLEDGED,
    RESOLVED,
    DISMISSED;

    public static AlertStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return AlertStatus.valueOf(code.trim().toUpperCase());
    }
}
