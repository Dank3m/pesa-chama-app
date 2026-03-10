-- V4: Group Settings and Super Admin Role
-- Adds extended group settings table and SUPER_ADMIN role support

-- Update users table role constraint to include SUPER_ADMIN
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'TREASURER', 'MEMBER', 'SERVICE'));

-- Create group_settings table for extended configuration
CREATE TABLE group_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL UNIQUE,

    -- Financial Year Settings
    financial_year_start_month INTEGER NOT NULL DEFAULT 12,
    financial_year_end_month INTEGER NOT NULL DEFAULT 11,

    -- Contribution Settings
    default_contribution_amount DECIMAL(15, 2) NOT NULL DEFAULT 3500.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'KES',
    allow_partial_contributions BOOLEAN NOT NULL DEFAULT true,

    -- Loan Settings
    interest_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.10,
    interest_calculation_method VARCHAR(30) NOT NULL DEFAULT 'SIMPLE',
    max_loan_duration_months INTEGER NOT NULL DEFAULT 12,
    grace_period_days INTEGER NOT NULL DEFAULT 0,
    max_loan_multiplier DECIMAL(5, 2) NOT NULL DEFAULT 3.00,
    require_guarantors BOOLEAN NOT NULL DEFAULT false,
    min_guarantors INTEGER NOT NULL DEFAULT 1,

    -- Scheduler Settings
    contribution_check_cron VARCHAR(50) DEFAULT '0 0 9 L * ?',
    interest_accrual_cron VARCHAR(50) DEFAULT '0 0 0 * * ?',
    overdue_check_cron VARCHAR(50) DEFAULT '0 0 8 * * ?',
    reminder_days_before_due INTEGER NOT NULL DEFAULT 3,

    -- Penalty Settings
    late_penalty_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.05,
    enable_penalties BOOLEAN NOT NULL DEFAULT true,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_group_settings_group FOREIGN KEY (group_id)
        REFERENCES banking_groups(id) ON DELETE CASCADE,
    CONSTRAINT chk_interest_calculation_method
        CHECK (interest_calculation_method IN ('SIMPLE', 'DAILY_COMPOUND', 'MONTHLY_COMPOUND', 'FLAT_RATE')),
    CONSTRAINT chk_start_month CHECK (financial_year_start_month BETWEEN 1 AND 12),
    CONSTRAINT chk_end_month CHECK (financial_year_end_month BETWEEN 1 AND 12)
);

-- Create index on group_id for fast lookups
CREATE INDEX idx_group_settings_group_id ON group_settings(group_id);

-- Add comment for documentation
COMMENT ON TABLE group_settings IS 'Extended settings for banking groups including financial, loan, and scheduling configurations';
COMMENT ON COLUMN group_settings.interest_calculation_method IS 'SIMPLE: Simple interest, DAILY_COMPOUND: Daily compound, MONTHLY_COMPOUND: Monthly compound, FLAT_RATE: Fixed rate on principal';
COMMENT ON COLUMN group_settings.max_loan_multiplier IS 'Maximum loan amount as multiple of member total contributions';
COMMENT ON COLUMN group_settings.contribution_check_cron IS 'Cron expression for monthly contribution check. Default: Last day of month at 9 AM';

-- Create default group_settings for existing groups
INSERT INTO group_settings (group_id, default_contribution_amount, interest_rate)
SELECT id, contribution_amount, interest_rate
FROM banking_groups
WHERE NOT EXISTS (
    SELECT 1 FROM group_settings gs WHERE gs.group_id = banking_groups.id
);
