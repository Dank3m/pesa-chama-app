-- V7__Multi_Group_Support.sql
-- Support for users belonging to multiple groups via email matching

-- Add default_group_id to user_settings for users with multiple group memberships
ALTER TABLE user_settings
ADD COLUMN IF NOT EXISTS default_group_id UUID REFERENCES banking_groups(id);

-- Add index for email lookups across groups
CREATE INDEX IF NOT EXISTS idx_members_email ON members(email) WHERE email IS NOT NULL;

-- Add index for faster group membership lookups
CREATE INDEX IF NOT EXISTS idx_members_email_group ON members(email, group_id) WHERE email IS NOT NULL;
