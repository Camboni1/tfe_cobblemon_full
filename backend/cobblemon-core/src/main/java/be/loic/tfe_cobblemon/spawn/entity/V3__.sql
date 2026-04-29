ALTER TABLE spawn_rule
DROP
CONSTRAINT fk_spawn_rule_form_matches_pokemon;

ALTER TABLE spawn_rule
    ADD form_selector VARCHAR(160);