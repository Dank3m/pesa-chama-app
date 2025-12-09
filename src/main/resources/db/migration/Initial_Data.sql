-- =============================================
-- SAMPLE TEST DATA FOR TABLE BANKING APP
-- =============================================
-- Run this after the application has started and Flyway has created the schema
-- Password for all users: password123 (BCrypt encoded)

-- =============================================
-- 1. BANKING GROUP
-- =============================================
INSERT INTO banking_groups (id, name, description, contribution_amount, currency, interest_rate, financial_year_start_month, financial_year_start_day, max_members, is_active, created_at, updated_at)
VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Pesa Chama Savings Group', 'A community savings and loans group for mutual financial support', 3500.00, 'KES', 0.10, 12, 1, 30, true, NOW(), NOW());

-- =============================================
-- 2. MEMBERS (21 members)
-- =============================================
INSERT INTO members (id, group_id, member_number, first_name, last_name, email, phone_number, national_id, date_of_birth, join_date, status, is_admin, created_at, updated_at)
VALUES
    -- Admins/Treasurers
    ('11111111-1111-1111-1111-111111111111', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0001', 'John', 'Kamau', 'john.kamau@email.com', '0712345001', '12345678', '1985-03-15', '2024-01-01', 'ACTIVE', true, NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0002', 'Mary', 'Wanjiku', 'mary.wanjiku@email.com', '0712345002', '12345679', '1988-07-22', '2024-01-01', 'ACTIVE', true, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0003', 'Peter', 'Ochieng', 'peter.ochieng@email.com', '0712345003', '12345680', '1990-11-08', '2024-01-01', 'ACTIVE', true, NOW(), NOW()),

    -- Regular Members
    ('44444444-4444-4444-4444-444444444444', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0004', 'Grace', 'Muthoni', 'grace.muthoni@email.com', '0712345004', '12345681', '1992-05-30', '2024-01-15', 'ACTIVE', false, NOW(), NOW()),
    ('55555555-5555-5555-5555-555555555555', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0005', 'James', 'Kiprop', 'james.kiprop@email.com', '0712345005', '12345682', '1987-09-12', '2024-01-15', 'ACTIVE', false, NOW(), NOW()),
    ('66666666-6666-6666-6666-666666666666', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0006', 'Lucy', 'Akinyi', 'lucy.akinyi@email.com', '0712345006', '12345683', '1995-02-18', '2024-02-01', 'ACTIVE', false, NOW(), NOW()),
    ('77777777-7777-7777-7777-777777777777', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0007', 'Daniel', 'Mutua', 'daniel.mutua@email.com', '0712345007', '12345684', '1983-12-05', '2024-02-01', 'ACTIVE', false, NOW(), NOW()),
    ('88888888-8888-8888-8888-888888888888', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0008', 'Susan', 'Njeri', 'susan.njeri@email.com', '0712345008', '12345685', '1991-08-25', '2024-02-15', 'ACTIVE', false, NOW(), NOW()),
    ('99999999-9999-9999-9999-999999999999', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0009', 'Michael', 'Otieno', 'michael.otieno@email.com', '0712345009', '12345686', '1989-04-14', '2024-02-15', 'ACTIVE', false, NOW(), NOW()),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0010', 'Faith', 'Chebet', 'faith.chebet@email.com', '0712345010', '12345687', '1994-06-07', '2024-03-01', 'ACTIVE', false, NOW(), NOW()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0011', 'Joseph', 'Wekesa', 'joseph.wekesa@email.com', '0712345011', '12345688', '1986-01-20', '2024-03-01', 'ACTIVE', false, NOW(), NOW()),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0012', 'Anne', 'Wairimu', 'anne.wairimu@email.com', '0712345012', '12345689', '1993-10-11', '2024-03-15', 'ACTIVE', false, NOW(), NOW()),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0013', 'David', 'Njuguna', 'david.njuguna@email.com', '0712345013', '12345690', '1988-03-28', '2024-03-15', 'ACTIVE', false, NOW(), NOW()),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0014', 'Elizabeth', 'Adhiambo', 'elizabeth.adhiambo@email.com', '0712345014', '12345691', '1990-07-03', '2024-04-01', 'ACTIVE', false, NOW(), NOW()),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0015', 'Samuel', 'Kibet', 'samuel.kibet@email.com', '0712345015', '12345692', '1984-11-16', '2024-04-01', 'ACTIVE', false, NOW(), NOW()),

    -- More members
    ('11111111-aaaa-1111-aaaa-111111111111', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0016', 'Catherine', 'Moraa', 'catherine.moraa@email.com', '0712345016', '12345693', '1992-09-09', '2024-04-15', 'ACTIVE', false, NOW(), NOW()),
    ('22222222-bbbb-2222-bbbb-222222222222', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0017', 'Patrick', 'Mwangi', 'patrick.mwangi@email.com', '0712345017', '12345694', '1987-02-14', '2024-04-15', 'ACTIVE', false, NOW(), NOW()),
    ('33333333-cccc-3333-cccc-333333333333', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0018', 'Joyce', 'Nyambura', 'joyce.nyambura@email.com', '0712345018', '12345695', '1995-05-21', '2024-05-01', 'ACTIVE', false, NOW(), NOW()),
    ('44444444-dddd-4444-dddd-444444444444', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0019', 'Stephen', 'Kariuki', 'stephen.kariuki@email.com', '0712345019', '12345696', '1989-08-08', '2024-05-01', 'ACTIVE', false, NOW(), NOW()),
    ('55555555-eeee-5555-eeee-555555555555', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0020', 'Rachel', 'Kemunto', 'rachel.kemunto@email.com', '0712345020', '12345697', '1991-12-25', '2024-05-15', 'ACTIVE', false, NOW(), NOW()),

    -- Inactive member for testing
    ('66666666-ffff-6666-ffff-666666666666', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'MEM0021', 'Thomas', 'Odera', 'thomas.odera@email.com', '0712345021', '12345698', '1986-04-30', '2024-01-01', 'INACTIVE', false, NOW(), NOW());

-- =============================================
-- 3. USERS (for authentication)
-- Password: password123 (BCrypt hash)
-- =============================================
INSERT INTO users (id, member_id, username, password_hash, role, is_enabled, failed_login_attempts, created_at, updated_at)
VALUES
    -- Admin user
    ('a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQoEqhS2q2h3k4LxGkIqK8hkKXKS', 'ADMIN', true, 0, NOW(), NOW()),

    -- Treasurer users
    ('a2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'treasurer1', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQoEqhS2q2h3k4LxGkIqK8hkKXKS', 'TREASURER', true, 0, NOW(), NOW()),
    ('a3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'treasurer2', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQoEqhS2q2h3k4LxGkIqK8hkKXKS', 'TREASURER', true, 0, NOW(), NOW()),

    -- Regular member users
    ('a4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'grace.m', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQoEqhS2q2h3k4LxGkIqK8hkKXKS', 'MEMBER', true, 0, NOW(), NOW()),
    ('a5555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'james.k', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQoEqhS2q2h3k4LxGkIqK8hkKXKS', 'MEMBER', true, 0, NOW(), NOW()),
    ('a6666666-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 'lucy.a', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQoEqhS2q2h3k4LxGkIqK8hkKXKS', 'MEMBER', true, 0, NOW(), NOW());

-- =============================================
-- 4. FINANCIAL YEAR (2024/2025: Dec 2024 - Nov 2025)
-- =============================================
INSERT INTO financial_years (id, group_id, year_name, start_date, end_date, is_current, is_closed, total_contributions, total_loans_disbursed, total_interest_earned, created_at, updated_at)
VALUES
    ('f1111111-1111-1111-1111-111111111111', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '2024/2025', '2024-12-01', '2025-11-30', true, false, 0.00, 0.00, 0.00, NOW(), NOW());

-- =============================================
-- 5. CONTRIBUTION CYCLES (Dec 2024)
-- =============================================
INSERT INTO contribution_cycles (id, financial_year_id, cycle_month, due_date, expected_amount, status, total_collected, is_processed, created_at, updated_at)
VALUES
    ('c1111111-1111-1111-1111-111111111111', 'f1111111-1111-1111-1111-111111111111', '2024-12-01', '2024-12-31', 3500.00, 'OPEN', 0.00, false, NOW(), NOW());

-- =============================================
-- 6. MEMBER BALANCES (Initialize for current financial year)
-- =============================================
INSERT INTO member_balances (id, member_id, financial_year_id, total_contributions, total_loans_taken, total_loan_repayments, outstanding_loan_balance, share_value, last_calculated_at, created_at, updated_at)
VALUES
    ('b1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('b2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('b3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('b4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('b5555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('b6666666-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('b7777777-7777-7777-7777-777777777777', '77777777-7777-7777-7777-777777777777', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('b8888888-8888-8888-8888-888888888888', '88888888-8888-8888-8888-888888888888', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('b9999999-9999-9999-9999-999999999999', '99999999-9999-9999-9999-999999999999', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW()),
    ('baaaaaa0-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'f1111111-1111-1111-1111-111111111111', 0.00, 0.00, 0.00, 0.00, 0.00, NOW(), NOW(), NOW());

-- =============================================
-- 7. CONTRIBUTIONS (Initialize for December cycle)
-- =============================================
INSERT INTO contributions (id, member_id, cycle_id, expected_amount, paid_amount, status, payment_date, converted_to_loan, notes, created_at, updated_at)
VALUES
    -- Some members have paid
    ('d1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 3500.00, 3500.00, 'PAID', '2024-12-05 10:00:00+03', false, 'MPESA TXN001DEC2024', NOW(), NOW()),
    ('d2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', 3500.00, 3500.00, 'PAID', '2024-12-06 11:30:00+03', false, 'MPESA TXN002DEC2024', NOW(), NOW()),
    ('d3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'c1111111-1111-1111-1111-111111111111', 3500.00, 3500.00, 'PAID', '2024-12-07 09:15:00+03', false, 'BANK TXN003DEC2024', NOW(), NOW()),
    ('d4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'c1111111-1111-1111-1111-111111111111', 3500.00, 3500.00, 'PAID', '2024-12-08 14:20:00+03', false, 'MPESA TXN004DEC2024', NOW(), NOW()),

    -- Partial payment
    ('d5555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'c1111111-1111-1111-1111-111111111111', 3500.00, 2000.00, 'PARTIAL', '2024-12-10 16:45:00+03', false, 'MPESA TXN005DEC2024 - Partial', NOW(), NOW()),

    -- Pending payments
    ('d6666666-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 'c1111111-1111-1111-1111-111111111111', 3500.00, 0.00, 'PENDING', NULL, false, NULL, NOW(), NOW()),
    ('d7777777-7777-7777-7777-777777777777', '77777777-7777-7777-7777-777777777777', 'c1111111-1111-1111-1111-111111111111', 3500.00, 0.00, 'PENDING', NULL, false, NULL, NOW(), NOW()),
    ('d8888888-8888-8888-8888-888888888888', '88888888-8888-8888-8888-888888888888', 'c1111111-1111-1111-1111-111111111111', 3500.00, 0.00, 'PENDING', NULL, false, NULL, NOW(), NOW()),
    ('d9999999-9999-9999-9999-999999999999', '99999999-9999-9999-9999-999999999999', 'c1111111-1111-1111-1111-111111111111', 3500.00, 0.00, 'PENDING', NULL, false, NULL, NOW(), NOW()),
    ('daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c1111111-1111-1111-1111-111111111111', 3500.00, 0.00, 'PENDING', NULL, false, NULL, NOW(), NOW());

-- Update cycle total collected
UPDATE contribution_cycles
SET total_collected = 16000.00
WHERE id = 'c1111111-1111-1111-1111-111111111111';

-- Update member balances for those who paid
UPDATE member_balances SET total_contributions = 3500.00, share_value = 3500.00 WHERE member_id = '11111111-1111-1111-1111-111111111111';
UPDATE member_balances SET total_contributions = 3500.00, share_value = 3500.00 WHERE member_id = '22222222-2222-2222-2222-222222222222';
UPDATE member_balances SET total_contributions = 3500.00, share_value = 3500.00 WHERE member_id = '33333333-3333-3333-3333-333333333333';
UPDATE member_balances SET total_contributions = 3500.00, share_value = 3500.00 WHERE member_id = '44444444-4444-4444-4444-444444444444';
UPDATE member_balances SET total_contributions = 2000.00, share_value = 2000.00 WHERE member_id = '55555555-5555-5555-5555-555555555555';

-- Update financial year totals
UPDATE financial_years SET total_contributions = 16000.00 WHERE id = 'f1111111-1111-1111-1111-111111111111';

-- =============================================
-- 8. SAMPLE LOAN (for member who has contributed)
-- =============================================
INSERT INTO loans (id, loan_number, member_id, financial_year_id, loan_type, principal_amount, interest_rate, daily_interest_rate, disbursement_date, expected_end_date, total_interest_accrued, total_amount_due, total_amount_paid, outstanding_balance, status, approved_by, approved_at, notes, created_at, updated_at)
VALUES
    ('e1111111-1111-1111-1111-111111111111', 'LN20241201000001', '44444444-4444-4444-4444-444444444444', 'f1111111-1111-1111-1111-111111111111', 'REGULAR', 10000.00, 0.10, 0.00318100, '2024-12-03', '2025-03-03', 500.00, 10500.00, 0.00, 10500.00, 'ACTIVE', 'a1111111-1111-1111-1111-111111111111', '2024-12-02 10:00:00+03', 'Business expansion loan', NOW(), NOW());

-- Update member balance with loan
UPDATE member_balances
SET total_loans_taken = 10000.00, outstanding_loan_balance = 10500.00, share_value = -7000.00
WHERE member_id = '44444444-4444-4444-4444-444444444444';

-- Update financial year with loan disbursement
UPDATE financial_years SET total_loans_disbursed = 10000.00 WHERE id = 'f1111111-1111-1111-1111-111111111111';

-- =============================================
-- 9. SAMPLE LOAN INTEREST ACCRUALS
-- =============================================
INSERT INTO loan_interest_accruals (id, loan_id, accrual_date, opening_balance, interest_amount, closing_balance, is_compounded, created_at)
VALUES
    ('ac111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', '2024-12-03', 10000.00, 31.81, 10031.81, true, '2024-12-04 01:00:00+03'),
    ('ac222222-2222-2222-2222-222222222222', 'e1111111-1111-1111-1111-111111111111', '2024-12-04', 10031.81, 31.91, 10063.72, true, '2024-12-05 01:00:00+03'),
    ('ac333333-3333-3333-3333-333333333333', 'e1111111-1111-1111-1111-111111111111', '2024-12-05', 10063.72, 32.01, 10095.73, true, '2024-12-06 01:00:00+03');

-- =============================================
-- 10. SAMPLE TRANSACTIONS (Audit Trail)
-- =============================================
INSERT INTO transactions (id, transaction_number, group_id, member_id, financial_year_id, transaction_type, transaction_date, amount, debit_credit, reference_type, reference_id, description, created_at)
VALUES
    -- Contribution transactions
    ('e2111111-1111-1111-1111-111111111111', 'TXN20241205000001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11111111-1111-1111-1111-111111111111', 'f1111111-1111-1111-1111-111111111111', 'CONTRIBUTION', '2024-12-05 10:00:00+03', 3500.00, 'CREDIT', 'CONTRIBUTION', 'd1111111-1111-1111-1111-111111111111', 'December 2024 contribution - MPESA', NOW()),
    ('e2222222-2222-2222-2222-222222222222', 'TXN20241206000001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '22222222-2222-2222-2222-222222222222', 'f1111111-1111-1111-1111-111111111111', 'CONTRIBUTION', '2024-12-06 11:30:00+03', 3500.00, 'CREDIT', 'CONTRIBUTION', 'd2222222-2222-2222-2222-222222222222', 'December 2024 contribution - MPESA', NOW()),
    ('e2333333-3333-3333-3333-333333333333', 'TXN20241207000001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '33333333-3333-3333-3333-333333333333', 'f1111111-1111-1111-1111-111111111111', 'CONTRIBUTION', '2024-12-07 09:15:00+03', 3500.00, 'CREDIT', 'CONTRIBUTION', 'd3333333-3333-3333-3333-333333333333', 'December 2024 contribution - BANK', NOW()),
    ('e2444444-4444-4444-4444-444444444444', 'TXN20241208000001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '44444444-4444-4444-4444-444444444444', 'f1111111-1111-1111-1111-111111111111', 'CONTRIBUTION', '2024-12-08 14:20:00+03', 3500.00, 'CREDIT', 'CONTRIBUTION', 'd4444444-4444-4444-4444-444444444444', 'December 2024 contribution - MPESA', NOW()),
    ('e2555555-5555-5555-5555-555555555555', 'TXN20241210000001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '55555555-5555-5555-5555-555555555555', 'f1111111-1111-1111-1111-111111111111', 'CONTRIBUTION', '2024-12-10 16:45:00+03', 2000.00, 'CREDIT', 'CONTRIBUTION', 'd5555555-5555-5555-5555-555555555555', 'December 2024 partial contribution - MPESA', NOW()),

    -- Loan disbursement transaction
    ('e2666666-6666-6666-6666-666666666666', 'TXN20241203000001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '44444444-4444-4444-4444-444444444444', 'f1111111-1111-1111-1111-111111111111', 'LOAN_DISBURSEMENT', '2024-12-03 10:00:00+03', 10000.00, 'DEBIT', 'LOAN', 'e1111111-1111-1111-1111-111111111111', 'Loan disbursement - Business expansion', NOW()),

    -- Interest accrual transactions
    ('e2777777-7777-7777-7777-777777777777', 'TXN20241204000001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '44444444-4444-4444-4444-444444444444', 'f1111111-1111-1111-1111-111111111111', 'INTEREST_ACCRUAL', '2024-12-04 01:00:00+03', 31.81, 'CREDIT', 'LOAN', 'e1111111-1111-1111-1111-111111111111', 'Daily interest accrual', NOW()),
    ('e2888888-8888-8888-8888-888888888888', 'TXN20241205000002', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '44444444-4444-4444-4444-444444444444', 'f1111111-1111-1111-1111-111111111111', 'INTEREST_ACCRUAL', '2024-12-05 01:00:00+03', 31.91, 'CREDIT', 'LOAN', 'e1111111-1111-1111-1111-111111111111', 'Daily interest accrual', NOW());

-- =============================================
-- 11. SAMPLE NOTIFICATIONS
-- =============================================
INSERT INTO notifications (id, member_id, notification_type, title, message, is_read, sent_via, sent_at, created_at)
VALUES
    ('fa111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', 'LOAN_APPROVED', 'Loan Approved', 'Your loan application for KES 10,000 has been approved.', true, 'SMS', '2024-12-02 10:05:00+03', NOW()),
    ('fa222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'LOAN_DISBURSED', 'Loan Disbursed', 'KES 10,000 has been disbursed to your account.', true, 'SMS', '2024-12-03 10:05:00+03', NOW()),
    ('fa333333-3333-3333-3333-333333333333', '66666666-6666-6666-6666-666666666666', 'CONTRIBUTION_REMINDER', 'Contribution Reminder', 'Your December 2024 contribution of KES 3,500 is due by 31st December.', false, 'SMS', '2024-12-08 09:00:00+03', NOW()),
    ('fa444444-4444-4444-4444-444444444444', '77777777-7777-7777-7777-777777777777', 'CONTRIBUTION_REMINDER', 'Contribution Reminder', 'Your December 2024 contribution of KES 3,500 is due by 31st December.', false, 'SMS', '2024-12-08 09:00:00+03', NOW());

-- =============================================
-- QUICK REFERENCE
-- =============================================
--
-- LOGIN CREDENTIALS:
-- ==================
-- Admin:      username: admin        password: password123
-- Treasurer:  username: treasurer1   password: password123
-- Treasurer:  username: treasurer2   password: password123
-- Member:     username: grace.m      password: password123
-- Member:     username: james.k      password: password123
-- Member:     username: lucy.a       password: password123
--
-- KEY IDs:
-- ========
-- GROUP ID:           a1b2c3d4-e5f6-7890-abcd-ef1234567890
-- FINANCIAL YEAR ID:  f1111111-1111-1111-1111-111111111111
-- CURRENT CYCLE ID:   c1111111-1111-1111-1111-111111111111
-- ACTIVE LOAN ID:     e1111111-1111-1111-1111-111111111111
--
-- SAMPLE MEMBER IDs:
-- - John Kamau (Admin):    11111111-1111-1111-1111-111111111111
-- - Mary Wanjiku:          22222222-2222-2222-2222-222222222222
-- - Peter Ochieng:         33333333-3333-3333-3333-333333333333
-- - Grace Muthoni:         44444444-4444-4444-4444-444444444444 (has active loan)
-- - James Kiprop:          55555555-5555-5555-5555-555555555555 (partial payment)
-- - Lucy Akinyi:           66666666-6666-6666-6666-666666666666 (pending payment)
--
-- USER IDs:
-- - Admin user:            a1111111-1111-1111-1111-111111111111
-- - Treasurer 1:           a2222222-2222-2222-2222-222222222222
-- - Treasurer 2:           a3333333-3333-3333-3333-333333333333
--
-- =============================================