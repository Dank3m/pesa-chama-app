-- V1__Initial_Schema.sql
-- Table Banking Loan Management System - Initial Schema

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Groups table (for scalability - multiple banking groups)
CREATE TABLE banking_groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    contribution_amount DECIMAL(15, 2) NOT NULL DEFAULT 3500.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'KES',
    interest_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.10,  -- 10% = 0.10
    financial_year_start_month INTEGER NOT NULL DEFAULT 12,  -- December
    financial_year_start_day INTEGER NOT NULL DEFAULT 1,
    max_members INTEGER DEFAULT 50,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT valid_start_month CHECK (financial_year_start_month BETWEEN 1 AND 12)
);

-- Members table
CREATE TABLE members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES banking_groups(id),
    member_number VARCHAR(20) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(20) NOT NULL,
    national_id VARCHAR(20),
    date_of_birth DATE,
    address TEXT,
    join_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_member_number_per_group UNIQUE (group_id, member_number),
    CONSTRAINT unique_phone_per_group UNIQUE (group_id, phone_number),
    CONSTRAINT valid_member_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LEFT'))
);

-- Users table (for authentication)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    member_id UUID REFERENCES members(id),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_user_role CHECK (role IN ('ADMIN', 'TREASURER', 'MEMBER'))
);

-- Financial years table
CREATE TABLE financial_years (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES banking_groups(id),
    year_name VARCHAR(20) NOT NULL,  -- e.g., "2024/2025"
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    total_contributions DECIMAL(15, 2) DEFAULT 0,
    total_loans_disbursed DECIMAL(15, 2) DEFAULT 0,
    total_interest_earned DECIMAL(15, 2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_financial_year_per_group UNIQUE (group_id, year_name),
    CONSTRAINT valid_date_range CHECK (end_date > start_date)
);

-- Contribution cycles (monthly periods)
CREATE TABLE contribution_cycles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    financial_year_id UUID NOT NULL REFERENCES financial_years(id),
    cycle_month DATE NOT NULL,  -- First day of the month
    due_date DATE NOT NULL,     -- Usually last day of month
    expected_amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    total_collected DECIMAL(15, 2) DEFAULT 0,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_cycle_per_year UNIQUE (financial_year_id, cycle_month),
    CONSTRAINT valid_cycle_status CHECK (status IN ('OPEN', 'CLOSED', 'PROCESSING'))
);

-- Contributions table
CREATE TABLE contributions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    member_id UUID NOT NULL REFERENCES members(id),
    cycle_id UUID NOT NULL REFERENCES contribution_cycles(id),
    expected_amount DECIMAL(15, 2) NOT NULL,
    paid_amount DECIMAL(15, 2) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP WITH TIME ZONE,
    converted_to_loan BOOLEAN NOT NULL DEFAULT FALSE,
    loan_id UUID,  -- Reference to loan if converted
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_contribution_per_member_cycle UNIQUE (member_id, cycle_id),
    CONSTRAINT valid_contribution_status CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'DEFAULTED', 'CONVERTED_TO_LOAN'))
);

-- Loans table
CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_number VARCHAR(30) NOT NULL UNIQUE,
    member_id UUID NOT NULL REFERENCES members(id),
    financial_year_id UUID NOT NULL REFERENCES financial_years(id),
    loan_type VARCHAR(30) NOT NULL,  -- REGULAR, CONTRIBUTION_DEFAULT
    principal_amount DECIMAL(15, 2) NOT NULL,
    interest_rate DECIMAL(5, 4) NOT NULL,  -- Monthly rate as decimal
    daily_interest_rate DECIMAL(10, 8) NOT NULL,  -- Calculated daily rate
    disbursement_date DATE NOT NULL,
    expected_end_date DATE NOT NULL,
    actual_end_date DATE,
    total_interest_accrued DECIMAL(15, 2) DEFAULT 0,
    total_amount_due DECIMAL(15, 2) NOT NULL,
    total_amount_paid DECIMAL(15, 2) DEFAULT 0,
    outstanding_balance DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    source_contribution_id UUID REFERENCES contributions(id),  -- If from defaulted contribution
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_loan_type CHECK (loan_type IN ('REGULAR', 'CONTRIBUTION_DEFAULT', 'EMERGENCY')),
    CONSTRAINT valid_loan_status CHECK (status IN ('PENDING', 'APPROVED', 'DISBURSED', 'ACTIVE', 'PAID_OFF', 'DEFAULTED', 'WRITTEN_OFF'))
);

-- Add foreign key for contribution loan reference
ALTER TABLE contributions ADD CONSTRAINT fk_contribution_loan 
    FOREIGN KEY (loan_id) REFERENCES loans(id);

-- Loan interest accruals (daily tracking)
CREATE TABLE loan_interest_accruals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id UUID NOT NULL REFERENCES loans(id),
    accrual_date DATE NOT NULL,
    opening_balance DECIMAL(15, 2) NOT NULL,
    interest_amount DECIMAL(15, 2) NOT NULL,
    closing_balance DECIMAL(15, 2) NOT NULL,
    is_compounded BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_accrual_per_loan_date UNIQUE (loan_id, accrual_date)
);

-- Loan repayments
CREATE TABLE loan_repayments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id UUID NOT NULL REFERENCES loans(id),
    payment_number INTEGER NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    principal_portion DECIMAL(15, 2) NOT NULL,
    interest_portion DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(30),
    reference_number VARCHAR(50),
    received_by UUID REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_payment_number UNIQUE (loan_id, payment_number)
);

-- Transactions ledger (for audit trail)
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_number VARCHAR(30) NOT NULL UNIQUE,
    group_id UUID NOT NULL REFERENCES banking_groups(id),
    member_id UUID REFERENCES members(id),
    financial_year_id UUID NOT NULL REFERENCES financial_years(id),
    transaction_type VARCHAR(30) NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(15, 2) NOT NULL,
    debit_credit VARCHAR(6) NOT NULL,
    reference_type VARCHAR(30),  -- CONTRIBUTION, LOAN, REPAYMENT, etc.
    reference_id UUID,
    description TEXT,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_transaction_type CHECK (transaction_type IN (
        'CONTRIBUTION', 'LOAN_DISBURSEMENT', 'LOAN_REPAYMENT', 
        'INTEREST_ACCRUAL', 'PENALTY', 'DIVIDEND', 'WITHDRAWAL', 'ADJUSTMENT'
    )),
    CONSTRAINT valid_debit_credit CHECK (debit_credit IN ('DEBIT', 'CREDIT'))
);

-- Member balances (cached for quick access)
CREATE TABLE member_balances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    member_id UUID NOT NULL REFERENCES members(id),
    financial_year_id UUID NOT NULL REFERENCES financial_years(id),
    total_contributions DECIMAL(15, 2) DEFAULT 0,
    total_loans_taken DECIMAL(15, 2) DEFAULT 0,
    total_loan_repayments DECIMAL(15, 2) DEFAULT 0,
    outstanding_loan_balance DECIMAL(15, 2) DEFAULT 0,
    share_value DECIMAL(15, 2) DEFAULT 0,
    last_calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_balance_per_member_year UNIQUE (member_id, financial_year_id)
);

-- Notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    member_id UUID NOT NULL REFERENCES members(id),
    notification_type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_via VARCHAR(20),  -- SMS, EMAIL, PUSH
    sent_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_notification_type CHECK (notification_type IN (
        'CONTRIBUTION_REMINDER', 'CONTRIBUTION_RECEIVED', 'LOAN_APPROVED',
        'LOAN_DISBURSED', 'PAYMENT_REMINDER', 'PAYMENT_RECEIVED',
        'OVERDUE_NOTICE', 'GENERAL'
    ))
);

-- Audit log
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_members_group ON members(group_id);
CREATE INDEX idx_members_status ON members(status);
CREATE INDEX idx_members_phone ON members(phone_number);

CREATE INDEX idx_financial_years_group ON financial_years(group_id);
CREATE INDEX idx_financial_years_current ON financial_years(group_id, is_current) WHERE is_current = TRUE;

CREATE INDEX idx_contribution_cycles_year ON contribution_cycles(financial_year_id);
CREATE INDEX idx_contribution_cycles_status ON contribution_cycles(status);
CREATE INDEX idx_contribution_cycles_month ON contribution_cycles(cycle_month);

CREATE INDEX idx_contributions_member ON contributions(member_id);
CREATE INDEX idx_contributions_cycle ON contributions(cycle_id);
CREATE INDEX idx_contributions_status ON contributions(status);

CREATE INDEX idx_loans_member ON loans(member_id);
CREATE INDEX idx_loans_year ON loans(financial_year_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_type ON loans(loan_type);

CREATE INDEX idx_interest_accruals_loan ON loan_interest_accruals(loan_id);
CREATE INDEX idx_interest_accruals_date ON loan_interest_accruals(accrual_date);

CREATE INDEX idx_repayments_loan ON loan_repayments(loan_id);
CREATE INDEX idx_repayments_date ON loan_repayments(payment_date);

CREATE INDEX idx_transactions_group ON transactions(group_id);
CREATE INDEX idx_transactions_member ON transactions(member_id);
CREATE INDEX idx_transactions_year ON transactions(financial_year_id);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);

CREATE INDEX idx_member_balances_member ON member_balances(member_id);
CREATE INDEX idx_member_balances_year ON member_balances(financial_year_id);

CREATE INDEX idx_notifications_member ON notifications(member_id);
CREATE INDEX idx_notifications_unread ON notifications(member_id, is_read) WHERE is_read = FALSE;

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_date ON audit_logs(created_at);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_banking_groups_updated_at BEFORE UPDATE ON banking_groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_members_updated_at BEFORE UPDATE ON members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_financial_years_updated_at BEFORE UPDATE ON financial_years
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_contribution_cycles_updated_at BEFORE UPDATE ON contribution_cycles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_contributions_updated_at BEFORE UPDATE ON contributions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_loans_updated_at BEFORE UPDATE ON loans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_member_balances_updated_at BEFORE UPDATE ON member_balances
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to generate transaction numbers
CREATE OR REPLACE FUNCTION generate_transaction_number()
RETURNS VARCHAR(30) AS $$
DECLARE
    new_number VARCHAR(30);
BEGIN
    new_number := 'TXN' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || 
                  LPAD(NEXTVAL('transaction_seq')::TEXT, 6, '0');
    RETURN new_number;
END;
$$ language 'plpgsql';

CREATE SEQUENCE IF NOT EXISTS transaction_seq START WITH 1;

-- Function to generate loan numbers
CREATE OR REPLACE FUNCTION generate_loan_number()
RETURNS VARCHAR(30) AS $$
DECLARE
    new_number VARCHAR(30);
BEGIN
    new_number := 'LN' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || 
                  LPAD(NEXTVAL('loan_seq')::TEXT, 6, '0');
    RETURN new_number;
END;
$$ language 'plpgsql';

CREATE SEQUENCE IF NOT EXISTS loan_seq START WITH 1;
