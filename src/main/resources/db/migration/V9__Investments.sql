-- V9__Investments.sql
-- Investment tracking for group investments (land, money market, fixed deposits, etc.)

CREATE TABLE investments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID NOT NULL REFERENCES banking_groups(id),
    financial_year_id UUID NOT NULL REFERENCES financial_years(id),
    member_id       UUID REFERENCES members(id),
    type            VARCHAR(30) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    amount          NUMERIC(15, 2) NOT NULL,
    current_value   NUMERIC(15, 2),
    investment_date DATE NOT NULL,
    maturity_date   DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    receipt_number  VARCHAR(50),
    notes           TEXT,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      UUID,
    updated_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT chk_investment_type CHECK (type IN ('LAND', 'MONEY_MARKET', 'FIXED_DEPOSIT', 'BONDS', 'SHARES', 'OTHER')),
    CONSTRAINT chk_investment_status CHECK (status IN ('ACTIVE', 'MATURED', 'REDEEMED', 'CANCELLED')),
    CONSTRAINT chk_investment_amount_positive CHECK (amount > 0)
);

-- Indexes
CREATE INDEX idx_investments_group_id ON investments(group_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_investments_financial_year ON investments(financial_year_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_investments_member_id ON investments(member_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_investments_status ON investments(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_investments_type ON investments(type) WHERE is_deleted = FALSE;
CREATE INDEX idx_investments_date ON investments(investment_date) WHERE is_deleted = FALSE;

-- Trigger to auto-update updated_at
CREATE TRIGGER update_investments_updated_at
    BEFORE UPDATE ON investments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
