-- Members: disbursement preferences
ALTER TABLE members
  ADD COLUMN preferred_disbursement_channel VARCHAR(10) DEFAULT 'MPESA',
  ADD COLUMN bank_account_number VARCHAR(30),
  ADD COLUMN bank_code VARCHAR(10),
  ADD COLUMN bank_name VARCHAR(100);

-- Loans: disbursement tracking
ALTER TABLE loans
  ADD COLUMN disbursement_channel VARCHAR(10),
  ADD COLUMN disbursement_status VARCHAR(20),
  ADD COLUMN disbursement_reference VARCHAR(100),
  ADD COLUMN disbursement_failure_reason TEXT;
