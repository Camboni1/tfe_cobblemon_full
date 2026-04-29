ALTER TABLE spawn_rule
    ADD COLUMN IF NOT EXISTS form_selector VARCHAR(160) NULL;

CREATE INDEX IF NOT EXISTS idx_spawn_rule_form_selector
    ON spawn_rule (form_selector);