-- V5: Add Interest Rate Period to Group Settings
-- Allows specifying whether the interest rate is per day, week, month, or year

-- Add interest_rate_period column
ALTER TABLE group_settings
ADD COLUMN interest_rate_period VARCHAR(20) NOT NULL DEFAULT 'MONTHLY';

-- Add check constraint for valid values
ALTER TABLE group_settings
ADD CONSTRAINT chk_interest_rate_period
    CHECK (interest_rate_period IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'));

-- Add comment for documentation
COMMENT ON COLUMN group_settings.interest_rate_period IS 'Period for which the interest rate applies: DAILY, WEEKLY, MONTHLY, or YEARLY';
