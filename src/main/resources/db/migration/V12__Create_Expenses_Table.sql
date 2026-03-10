-- Create expenses table
CREATE TABLE IF NOT EXISTS expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES banking_groups(id),
    financial_year_id UUID NOT NULL REFERENCES financial_years(id),
    category VARCHAR(30) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    expense_date DATE NOT NULL,
    description TEXT NOT NULL,
    vendor VARCHAR(100),
    receipt_number VARCHAR(50),
    loan_id UUID REFERENCES loans(id),
    notes TEXT,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_expense_category CHECK (category IN (
        'TRANSACTION_FEE', 'DISBURSEMENT_FEE', 'AGM_VENUE', 'AGM_CATERING',
        'AGM_OTHER', 'ADMINISTRATIVE', 'BANK_CHARGES', 'COMMUNICATION', 'LEGAL', 'OTHER'
    ))
);

CREATE INDEX IF NOT EXISTS idx_expenses_group ON expenses(group_id);
CREATE INDEX IF NOT EXISTS idx_expenses_year ON expenses(financial_year_id);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category);
CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(expense_date);

CREATE TRIGGER update_expenses_updated_at BEFORE UPDATE ON expenses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
