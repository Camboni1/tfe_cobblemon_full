ALTER TABLE item
    ADD COLUMN IF NOT EXISTS generated_placeholder BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE item
SET generated_placeholder = TRUE
WHERE raw_json IS NULL;

CREATE INDEX IF NOT EXISTS idx_item_dataset_version_generated_placeholder
    ON item (dataset_version_id, generated_placeholder);