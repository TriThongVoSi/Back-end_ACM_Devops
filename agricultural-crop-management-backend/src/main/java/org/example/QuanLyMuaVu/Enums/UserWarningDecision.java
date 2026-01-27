package org.example.QuanLyMuaVu.Enums;

public enum UserWarningDecision {
    WARNING,
    LOCK_1_DAY,
    LOCK_PERMANENT;

    public static UserWarningDecision fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return UserWarningDecision.valueOf(code.trim().toUpperCase());
    }
}
