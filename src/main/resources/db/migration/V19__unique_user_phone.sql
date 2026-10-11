-- ============================================================
-- V19 : Unique user phone number constraint
-- ============================================================

-- Ensure phone numbers are unique among registered accounts, ignoring nulls and blank strings
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_telephone_unique 
ON users(telephone) 
WHERE telephone IS NOT NULL AND telephone <> '';

