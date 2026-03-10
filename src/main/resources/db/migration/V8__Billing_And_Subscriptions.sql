-- V8__Billing_And_Subscriptions.sql
-- Billing and Subscription System for Table Banking

-- ============================================
-- Subscription Plans (system-wide, seeded)
-- ============================================
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,                 -- FREE, STANDARD, PREMIUM
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'KES',
    billing_period VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',  -- MONTHLY, YEARLY
    max_members INTEGER,                              -- NULL = unlimited
    features JSONB NOT NULL DEFAULT '{}',             -- {"loans": true, "sms": true, ...}
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_billing_period CHECK (billing_period IN ('MONTHLY', 'YEARLY'))
);

-- Apply updated_at trigger
CREATE TRIGGER update_subscription_plans_updated_at BEFORE UPDATE ON subscription_plans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Group Subscriptions
-- ============================================
CREATE TABLE group_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES banking_groups(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',     -- ACTIVE, EXPIRED, CANCELLED, SUSPENDED
    start_date DATE NOT NULL,
    end_date DATE,                                    -- NULL for free tier or grandfathered
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    is_grandfathered BOOLEAN NOT NULL DEFAULT FALSE,  -- True for existing groups migrated to PREMIUM
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_subscription_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED'))
);

-- Only one active subscription per group
CREATE UNIQUE INDEX idx_unique_active_subscription ON group_subscriptions(group_id) WHERE status = 'ACTIVE';

-- Apply updated_at trigger
CREATE TRIGGER update_group_subscriptions_updated_at BEFORE UPDATE ON group_subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Indexes for group subscriptions
CREATE INDEX idx_group_subscriptions_group ON group_subscriptions(group_id);
CREATE INDEX idx_group_subscriptions_plan ON group_subscriptions(plan_id);
CREATE INDEX idx_group_subscriptions_status ON group_subscriptions(status);
CREATE INDEX idx_group_subscriptions_end_date ON group_subscriptions(end_date) WHERE end_date IS NOT NULL;

-- ============================================
-- Subscription Payments
-- ============================================
CREATE TABLE subscription_payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_number VARCHAR(30) NOT NULL UNIQUE,
    subscription_id UUID NOT NULL REFERENCES group_subscriptions(id),
    group_id UUID NOT NULL REFERENCES banking_groups(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'KES',
    payment_method VARCHAR(30) NOT NULL,              -- MPESA, BANK, PESALINK, CARD
    payment_reference VARCHAR(100),                   -- External ref (M-Pesa code, etc.)
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',    -- PENDING, COMPLETED, FAILED, REFUNDED
    paid_by UUID REFERENCES users(id),
    paid_at TIMESTAMP WITH TIME ZONE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    failure_reason TEXT,
    metadata JSONB,                                   -- Payment provider details
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_payment_method CHECK (payment_method IN ('MPESA', 'BANK', 'PESALINK', 'CARD')),
    CONSTRAINT valid_payment_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'))
);

-- Apply updated_at trigger
CREATE TRIGGER update_subscription_payments_updated_at BEFORE UPDATE ON subscription_payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Indexes for subscription payments
CREATE INDEX idx_subscription_payments_subscription ON subscription_payments(subscription_id);
CREATE INDEX idx_subscription_payments_group ON subscription_payments(group_id);
CREATE INDEX idx_subscription_payments_status ON subscription_payments(status);
CREATE INDEX idx_subscription_payments_reference ON subscription_payments(payment_reference) WHERE payment_reference IS NOT NULL;
CREATE INDEX idx_subscription_payments_paid_at ON subscription_payments(paid_at) WHERE paid_at IS NOT NULL;

-- Sequence for payment numbers
CREATE SEQUENCE IF NOT EXISTS subscription_payment_seq START WITH 1;

-- Function to generate subscription payment numbers
CREATE OR REPLACE FUNCTION generate_subscription_payment_number()
RETURNS VARCHAR(30) AS $$
DECLARE
    new_number VARCHAR(30);
BEGIN
    new_number := 'SUB' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') ||
                  LPAD(NEXTVAL('subscription_payment_seq')::TEXT, 6, '0');
    RETURN new_number;
END;
$$ language 'plpgsql';

-- ============================================
-- Add subscription reference to banking_groups
-- ============================================
ALTER TABLE banking_groups
ADD COLUMN IF NOT EXISTS current_subscription_id UUID REFERENCES group_subscriptions(id);

CREATE INDEX idx_banking_groups_subscription ON banking_groups(current_subscription_id) WHERE current_subscription_id IS NOT NULL;

-- ============================================
-- Seed Subscription Plans
-- ============================================
INSERT INTO subscription_plans (id, name, display_name, description, price, max_members, features, sort_order) VALUES
(
    uuid_generate_v4(),
    'FREE',
    'Starter',
    'Perfect for small groups just getting started with contributions tracking',
    0,
    10,
    '{"contributions": true, "basicReports": true, "loans": false, "sms": false, "externalLoans": false, "apiAccess": false, "prioritySupport": false}',
    1
),
(
    uuid_generate_v4(),
    'STANDARD',
    'Standard',
    'Full-featured plan for growing groups that need loan management',
    1000,
    30,
    '{"contributions": true, "basicReports": true, "loans": true, "sms": true, "externalLoans": false, "apiAccess": false, "prioritySupport": false}',
    2
),
(
    uuid_generate_v4(),
    'PREMIUM',
    'Premium',
    'Complete solution for large groups with all advanced features',
    10000,
    NULL,
    '{"contributions": true, "basicReports": true, "loans": true, "sms": true, "externalLoans": true, "apiAccess": true, "prioritySupport": true}',
    3
);

-- ============================================
-- Grandfather Existing Groups to PREMIUM
-- ============================================
-- Existing groups get PREMIUM plan with no end date (free forever)
INSERT INTO group_subscriptions (id, group_id, plan_id, status, start_date, end_date, auto_renew, is_grandfathered)
SELECT
    uuid_generate_v4(),
    g.id,
    (SELECT id FROM subscription_plans WHERE name = 'PREMIUM'),
    'ACTIVE',
    CURRENT_DATE,
    NULL,           -- No end date = grandfathered indefinitely
    FALSE,          -- No auto-renew since it's free
    TRUE            -- Mark as grandfathered
FROM banking_groups g
WHERE NOT EXISTS (
    SELECT 1 FROM group_subscriptions gs
    WHERE gs.group_id = g.id AND gs.status = 'ACTIVE'
);

-- Update banking_groups with their subscription reference
UPDATE banking_groups g
SET current_subscription_id = (
    SELECT gs.id
    FROM group_subscriptions gs
    WHERE gs.group_id = g.id AND gs.status = 'ACTIVE'
    LIMIT 1
)
WHERE current_subscription_id IS NULL
  AND EXISTS (
    SELECT 1 FROM group_subscriptions gs
    WHERE gs.group_id = g.id AND gs.status = 'ACTIVE'
);
