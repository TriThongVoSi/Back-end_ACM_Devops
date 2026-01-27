package org.example.QuanLyMuaVu.Util;

/**
 * Password Policy - Single source of truth for password validation rules.
 * 
 * Rules:
 * - Minimum 8 characters
 * - Maximum 64 characters
 * - At least 1 uppercase letter
 * - At least 1 lowercase letter
 * - At least 1 number
 * - At least 1 special character (@$!%*?&)
 * - No spaces allowed
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    /**
     * Regex pattern for password validation.
     * (?=.*[a-z]) - at least one lowercase
     * (?=.*[A-Z]) - at least one uppercase
     * (?=.*\\d) - at least one digit
     * (?=.*[@$!%*?&]) - at least one special character
     * [A-Za-z\\d@$!%*?&]{8,64} - allowed characters with length constraint
     */
    public static final String PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$";

    public static final String MESSAGE = "Password must be 8-64 characters with at least 1 uppercase, 1 lowercase, 1 number, and 1 special character (@$!%*?&)";

    public static final String MESSAGE_KEY = "PASSWORD_POLICY_INVALID";

    private PasswordPolicy() {
        // Utility class
    }

    /**
     * Validate a password against the policy.
     * 
     * @param password the password to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return password.matches(PATTERN);
    }
}
