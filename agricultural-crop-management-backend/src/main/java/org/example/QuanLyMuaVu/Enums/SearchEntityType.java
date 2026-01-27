package org.example.QuanLyMuaVu.Enums;

public enum SearchEntityType {
    PLOT,
    SEASON,
    TASK,
    EXPENSE,
    DOCUMENT,
    FARM,
    USER;

    public static SearchEntityType fromParam(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.endsWith("S")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            return SearchEntityType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
