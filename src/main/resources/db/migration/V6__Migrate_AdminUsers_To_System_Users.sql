-- V6__Migrate_AdminUsers_To_System_Users.sql

-- Step 1: Create ENUM type for user roles if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('ADMIN', 'RESTAURANT_OWNER');
    END IF;
END $$;

ALTER TABLE food_stalls ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE';
ALTER TABLE food_stalls ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE food_stalls ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Step 3: Rename admin_users table to users (only if admin_users exists and users doesn't)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'admin_users')
         AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        ALTER TABLE admin_users RENAME TO users;
    ELSIF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        -- Create users table from scratch if neither table exists
        CREATE TABLE users (
            id BIGSERIAL PRIMARY KEY,
            username VARCHAR(100) UNIQUE NOT NULL,
            email VARCHAR(255) UNIQUE NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            oauth_provider VARCHAR(50),
            oauth_subject VARCHAR(255),
            restaurant_id BIGINT,
            role VARCHAR(30) DEFAULT 'ADMIN',
            enabled BOOLEAN DEFAULT true,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END $$;

ALTER TABLE users ADD COLUMN IF NOT EXISTS restaurant_id BIGINT;

UPDATE users SET role = 'ADMIN' WHERE role = 'ADMIN';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_food_stalls_user_id'
    ) THEN
        ALTER TABLE food_stalls
            ADD CONSTRAINT fk_food_stalls_user_id
            FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_restaurant_id ON users(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_food_stalls_status ON food_stalls(status);
CREATE INDEX IF NOT EXISTS idx_food_stalls_owner_id ON food_stalls(owner_id);

CREATE TABLE IF NOT EXISTS food_stall_updates (
    id BIGSERIAL PRIMARY KEY,
    food_stall_id BIGINT NOT NULL REFERENCES food_stalls(id) ON DELETE CASCADE,
    owner_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    changes JSONB NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_food_stall_updates_status ON food_stall_updates(status);
CREATE INDEX IF NOT EXISTS idx_food_stall_updates_owner_id ON food_stall_updates(owner_id);
