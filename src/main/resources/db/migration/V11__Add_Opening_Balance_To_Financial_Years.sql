-- Add opening balance to financial years for brought-forward funds
ALTER TABLE financial_years ADD COLUMN IF NOT EXISTS opening_balance DECIMAL(15, 2) NOT NULL DEFAULT 0;
