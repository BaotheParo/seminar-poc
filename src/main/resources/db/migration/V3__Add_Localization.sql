-- Migration V3: Tao bang food_stall_localizations cho da ngon ngu
CREATE TABLE IF NOT EXISTS food_stall_localizations (
    id BIGSERIAL PRIMARY KEY,
    food_stall_id BIGINT NOT NULL,
    language_code VARCHAR(10) NOT NULL,         -- vi, en, ja, ko, zh
    name VARCHAR(255),
    description TEXT,
    audio_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_localization_stall FOREIGN KEY (food_stall_id) REFERENCES food_stalls(id) ON DELETE CASCADE,
    CONSTRAINT uq_stall_lang UNIQUE (food_stall_id, language_code)
);

CREATE INDEX IF NOT EXISTS idx_localization_stall_lang
ON food_stall_localizations (food_stall_id, language_code);

-- Seed du lieu tieng Viet tu bang food_stalls hien co
INSERT INTO food_stall_localizations (food_stall_id, language_code, name, description, audio_url, created_at)
SELECT
    id,
    'vi',
    name,
    description,
    audio_url,
    CURRENT_TIMESTAMP
FROM food_stalls
ON CONFLICT (food_stall_id, language_code) DO NOTHING;
