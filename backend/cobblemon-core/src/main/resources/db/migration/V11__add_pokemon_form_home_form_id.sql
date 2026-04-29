-- Home form ID (sprite Pokémon Home pour formes alternatives : mega, gmax, régionales…)
-- Ex. Charizard mega-x -> 10034. Null = forme utilisant le sprite de base.
ALTER TABLE pokemon_form
    ADD COLUMN home_form_id INTEGER NULL;

CREATE INDEX idx_pokemon_form_home_form_id
    ON pokemon_form (home_form_id);