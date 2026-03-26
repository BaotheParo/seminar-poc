-- Drop the hardcoded check constraint created by Hibernate for the old Enum values
ALTER TABLE user_activities DROP CONSTRAINT IF EXISTS user_activities_action_type_check;

-- Optional: Re-add it with new values if desired, or just let Java handle validation.
-- For now, dropping it is enough to resolve the DataIntegrityViolation.
