INSERT INTO translation (key, locale, value) VALUES
                                                 ('bucket.COMMON',     'en', 'Common'),
                                                 ('bucket.COMMON',     'fr', 'Commun'),
                                                 ('bucket.UNCOMMON',   'en', 'Uncommon'),
                                                 ('bucket.UNCOMMON',   'fr', 'Peu commun'),
                                                 ('bucket.RARE',       'en', 'Rare'),
                                                 ('bucket.RARE',       'fr', 'Rare'),
                                                 ('bucket.ULTRA_RARE', 'en', 'Ultra Rare'),
                                                 ('bucket.ULTRA_RARE', 'fr', 'Ultra Rare'),
                                                 ('position.GROUNDED',  'en', 'Ground'),
                                                 ('position.GROUNDED',  'fr', 'Sol'),
                                                 ('position.SURFACE',   'en', 'Surface'),
                                                 ('position.SURFACE',   'fr', 'Surface'),
                                                 ('position.SUBMERGED', 'en', 'Underwater'),
                                                 ('position.SUBMERGED', 'fr', 'Sous l''eau'),
                                                 ('position.SEAFLOOR',  'en', 'Seafloor'),
                                                 ('position.SEAFLOOR',  'fr', 'Fond marin'),
                                                 ('position.FISHING',   'en', 'Fishing'),
                                                 ('position.FISHING',   'fr', 'Pêche'),
                                                 ('spawnType.POKEMON',      'en', 'Pokémon'),
                                                 ('spawnType.POKEMON',      'fr', 'Pokémon'),
                                                 ('spawnType.POKEMON_HERD', 'en', 'Pokémon Herd'),
                                                 ('spawnType.POKEMON_HERD', 'fr', 'Troupeau de Pokémon'),
                                                 ('token.BIOME',        'en', 'Biome'),
                                                 ('token.BIOME',        'fr', 'Biome'),
                                                 ('token.STRUCTURE',    'en', 'Structure'),
                                                 ('token.STRUCTURE',    'fr', 'Structure'),
                                                 ('token.DIMENSION',    'en', 'Dimension'),
                                                 ('token.DIMENSION',    'fr', 'Dimension'),
                                                 ('token.NEARBY_BLOCK', 'en', 'Nearby block'),
                                                 ('token.NEARBY_BLOCK', 'fr', 'Bloc proche'),
                                                 ('token.BASE_BLOCK',   'en', 'Base block'),
                                                 ('token.BASE_BLOCK',   'fr', 'Bloc de base'),
                                                 ('token.LABEL',        'en', 'Label'),
                                                 ('token.LABEL',        'fr', 'Label')
    ON CONFLICT (key, locale) DO UPDATE SET value = EXCLUDED.value;

-- Sides
INSERT INTO translation (key, locale, value) VALUES
                                                 ('side.CONDITION',     'en', 'Condition'),
                                                 ('side.CONDITION',     'fr', 'Condition'),
                                                 ('side.ANTICONDITION', 'en', 'Anti-condition'),
                                                 ('side.ANTICONDITION', 'fr', 'Anti-condition')
    ON CONFLICT (key, locale) DO UPDATE SET value = EXCLUDED.value;

-- Générations
INSERT INTO translation (key, locale, value) VALUES
                                                 ('generation.GEN_1', 'en', 'Generation I'),
                                                 ('generation.GEN_1', 'fr', 'Génération I'),
                                                 ('generation.GEN_2', 'en', 'Generation II'),
                                                 ('generation.GEN_2', 'fr', 'Génération II'),
                                                 ('generation.GEN_3', 'en', 'Generation III'),
                                                 ('generation.GEN_3', 'fr', 'Génération III'),
                                                 ('generation.GEN_4', 'en', 'Generation IV'),
                                                 ('generation.GEN_4', 'fr', 'Génération IV'),
                                                 ('generation.GEN_5', 'en', 'Generation V'),
                                                 ('generation.GEN_5', 'fr', 'Génération V'),
                                                 ('generation.GEN_6', 'en', 'Generation VI'),
                                                 ('generation.GEN_6', 'fr', 'Génération VI'),
                                                 ('generation.GEN_7', 'en', 'Generation VII'),
                                                 ('generation.GEN_7', 'fr', 'Génération VII'),
                                                 ('generation.GEN_8', 'en', 'Generation VIII'),
                                                 ('generation.GEN_8', 'fr', 'Génération VIII'),
                                                 ('generation.GEN_9', 'en', 'Generation IX'),
                                                 ('generation.GEN_9', 'fr', 'Génération IX')
    ON CONFLICT (key, locale) DO UPDATE SET value = EXCLUDED.value;