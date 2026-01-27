-- Expense enhancements for payment status, attachments, and soft delete
ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS supplier_id INT NULL,
    ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS attachment_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS attachment_mime VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS attachment_path VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

ALTER TABLE expenses
    ADD CONSTRAINT fk_expense_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL;

ALTER TABLE seasons
    ADD COLUMN IF NOT EXISTS budget_amount DECIMAL(15,2) NULL;

CREATE INDEX IF NOT EXISTS idx_expense_payment_status ON expenses(payment_status);
CREATE INDEX IF NOT EXISTS idx_expense_supplier_id ON expenses(supplier_id);
CREATE INDEX IF NOT EXISTS idx_expense_deleted_at ON expenses(deleted_at);
CREATE INDEX IF NOT EXISTS idx_expense_season_date ON expenses(season_id, expense_date);
