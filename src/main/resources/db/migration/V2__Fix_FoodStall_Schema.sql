-- Migration to add missing columns in case V1 was baselined on an older schema version
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='food_stalls' AND column_name='trigger_radius') THEN
        ALTER TABLE food_stalls ADD COLUMN trigger_radius INTEGER DEFAULT 15;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='food_stalls' AND column_name='min_price') THEN
        ALTER TABLE food_stalls ADD COLUMN min_price INTEGER;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='food_stalls' AND column_name='max_price') THEN
        ALTER TABLE food_stalls ADD COLUMN max_price INTEGER;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='food_stalls' AND column_name='audio_duration') THEN
        ALTER TABLE food_stalls ADD COLUMN audio_duration INTEGER;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='food_stalls' AND column_name='featured_reviews') THEN
        ALTER TABLE food_stalls ADD COLUMN featured_reviews JSONB;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='food_stalls' AND column_name='rating') THEN
        ALTER TABLE food_stalls ADD COLUMN rating DOUBLE PRECISION;
    END IF;
END $$;
