package com.acm.auth.constant;

/**
 * Predefined role codes used in the application.
 * ADMIN can create additional roles via CRUD operations.
 */
public final class PredefinedRole {

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String USER_ROLE = "USER";

    private PredefinedRole() {
        // Prevent instantiation
    }
}
