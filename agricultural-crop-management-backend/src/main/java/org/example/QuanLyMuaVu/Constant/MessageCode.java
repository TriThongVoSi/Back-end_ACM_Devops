package org.example.QuanLyMuaVu.Constant;

/**
 * Message codes and messages as specified in Demo Gen Code.docx Business Rules.
 * These constants are used for standardized user feedback across the
 * application.
 */
public final class MessageCode {

    private MessageCode() {
        // Prevent instantiation
    }

    // ═══════════════════════════════════════════════════════════════
    // MSG CODES (as referenced in Business Rules)
    // ═══════════════════════════════════════════════════════════════

    /** MSG1: Mandatory field validation failure */
    public static final String MSG1_CODE = "MSG1";
    public static final String MSG1 = "Please enter mandatory data.";

    /** MSG4: Format validation failure */
    public static final String MSG4_CODE = "MSG4";
    public static final String MSG4 = "Invalid format. Please enter again.";

    /** MSG7: Successful save operation */
    public static final String MSG7_CODE = "MSG7";
    public static final String MSG7 = "Save data successful.";

    /** MSG9: Database/system constraint violation */
    public static final String MSG9_CODE = "MSG9";
    public static final String MSG9 = "Your action is failed due to constraints in the system.";

    /** MSG10: Data not found */
    public static final String MSG10_CODE = "MSG10";
    public static final String MSG10 = "Data not found.";

    /** MSG11: Confirmation prompt */
    public static final String MSG11_CODE = "MSG11";
    public static final String MSG11 = "Are you sure you want to proceed with this action?";

    // ═══════════════════════════════════════════════════════════════
    // SPECIALIZED MESSAGES
    // ═══════════════════════════════════════════════════════════════

    public static final String SEASON_NOT_FOUND = "Season not found.";
    public static final String EXPENSE_NOT_FOUND = "Expense not found.";
    public static final String SEASON_NAME_EXISTS_IN_PLOT = "Season name already exists in this plot.";
    public static final String SEASON_OVERLAP = "Season dates overlap with an existing season in the same plot.";
    public static final String INVALID_STATUS_TRANSITION = "Status transition is not allowed.";
    public static final String ACCESS_DENIED_INACTIVE = "Access denied: account is inactive or suspended.";
}
