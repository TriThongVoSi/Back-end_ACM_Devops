package org.example.QuanLyMuaVu.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported weight units for user preferences.
 */
public enum WeightUnit {
    KG("KG"),
    G("G"),
    TON("TON");

    private final String code;

    WeightUnit(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @JsonCreator
    public static WeightUnit fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WeightUnit value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown WeightUnit: " + code);
    }

    @JsonValue
    public String toJson() {
        return this.code;
    }
}
