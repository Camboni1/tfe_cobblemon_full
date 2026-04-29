ALTER TABLE spawn_rule
    ADD COLUMN form_selector VARCHAR(160) NULL;

CREATE INDEX idx_spawn_rule_form_selector
    ON spawn_rule (form_selector);