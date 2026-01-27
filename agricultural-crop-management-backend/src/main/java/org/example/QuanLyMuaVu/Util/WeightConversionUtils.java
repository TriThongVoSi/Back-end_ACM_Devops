package org.example.QuanLyMuaVu.Util;

import org.example.QuanLyMuaVu.Enums.WeightUnit;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Utility for weight conversions using KG as the canonical unit.
 */
public final class WeightConversionUtils {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;

    private WeightConversionUtils() {
    }

    public static BigDecimal convertKgTo(BigDecimal valueKg, WeightUnit unit) {
        if (valueKg == null || unit == null) {
            return valueKg;
        }
        return switch (unit) {
            case KG -> valueKg;
            case G -> valueKg.multiply(THOUSAND, MATH_CONTEXT);
            case TON -> valueKg.divide(THOUSAND, MATH_CONTEXT);
        };
    }

    public static BigDecimal convertToKg(BigDecimal value, WeightUnit unit) {
        if (value == null || unit == null) {
            return value;
        }
        return switch (unit) {
            case KG -> value;
            case G -> value.divide(THOUSAND, MATH_CONTEXT);
            case TON -> value.multiply(THOUSAND, MATH_CONTEXT);
        };
    }
}
