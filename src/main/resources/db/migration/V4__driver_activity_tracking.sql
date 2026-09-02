ALTER TABLE drivers ADD COLUMN IF NOT EXISTS last_action_description TEXT;
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS last_action_timestamp TIMESTAMP WITH TIME ZONE;
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS next_action_description TEXT;
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS current_location_name VARCHAR(255);
