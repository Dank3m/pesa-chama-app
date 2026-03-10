-- V6__Fix_Member_Uniqueness_Per_Group.sql
-- Fix member uniqueness constraints to be per-group instead of global
-- This prevents account mixing when same email/phone exists in different groups

-- Add unique constraint for email per group (only if email is not null)
-- Using a partial unique index since email can be null
CREATE UNIQUE INDEX IF NOT EXISTS unique_email_per_group
    ON members (group_id, email)
    WHERE email IS NOT NULL;

-- Add unique constraint for national_id per group (only if national_id is not null)
CREATE UNIQUE INDEX IF NOT EXISTS unique_national_id_per_group
    ON members (group_id, national_id)
    WHERE national_id IS NOT NULL;

-- Note: unique_phone_per_group already exists from V1 migration
-- Note: unique_member_number_per_group already exists from V1 migration
