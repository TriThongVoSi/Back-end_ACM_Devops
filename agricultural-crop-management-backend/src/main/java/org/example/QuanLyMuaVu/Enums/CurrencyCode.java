package org.example.QuanLyMuaVu.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported currency codes for user preferences.
 */
public enum CurrencyCode {
    VND("VND"),
    USD("USD");

    private final String code;

    CurrencyCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @JsonCreator
    public static CurrencyCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CurrencyCode value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown CurrencyCode: " + code);
    }

    @JsonValue
    public String toJson() {
        return this.code;
    }
}
