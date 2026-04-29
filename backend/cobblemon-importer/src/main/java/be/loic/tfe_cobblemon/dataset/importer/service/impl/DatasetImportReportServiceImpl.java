package be.loic.tfe_cobblemon.dataset.importer.service.impl;

import be.loic.tfe_cobblemon.dataset.importer.service.DatasetImportReportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatasetImportReportServiceImpl implements DatasetImportReportService {

    private static final Logger log = LoggerFactory.getLogger(DatasetImportReportServiceImpl.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private static final String COBBLEMON_SYNTHETIC_SOURCE = "generated_from_cobblemon_kotlin_registry";
    private static final String MINECRAFT_SYNTHETIC_SOURCE = "minecraft_registry_reference";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void logFinalReport(
            Long datasetVersionId,
            String code,
            String label,
            String inputPath,
            boolean cleanBeforeImport,
            Instant startedAt
    ) {
        Instant finishedAt = Instant.now();

        boolean active = isActive(datasetVersionId);
        Instant importedAt = findImportedAt(datasetVersionId);

        long itemCount = count("""
                SELECT COUNT(*)
                FROM item
                WHERE dataset_version_id = ?
                """, datasetVersionId);

        long placeholderItemCount = count("""
                SELECT COUNT(*)
                FROM item
                WHERE dataset_version_id = ?
                  AND generated_placeholder = TRUE
                """, datasetVersionId);

        long syntheticHydratedItemCount = count("""
                SELECT COUNT(*)
                FROM item
                WHERE dataset_version_id = ?
                  AND generated_placeholder = FALSE
                  AND (raw_json ->> 'source') IN (?, ?)
                """,
                datasetVersionId,
                COBBLEMON_SYNTHETIC_SOURCE,
                MINECRAFT_SYNTHETIC_SOURCE
        );

        long realSourceItemCount = count("""
                SELECT COUNT(*)
                FROM item
                WHERE dataset_version_id = ?
                  AND generated_placeholder = FALSE
                  AND COALESCE(raw_json ->> 'source', '') NOT IN (?, ?)
                """,
                datasetVersionId,
                COBBLEMON_SYNTHETIC_SOURCE,
                MINECRAFT_SYNTHETIC_SOURCE
        );

        long placeholderItemsUsedInDropsCount = count("""
                SELECT COUNT(DISTINCT i.id)
                FROM item i
                JOIN pokemon_drop pd ON pd.item_id = i.id
                WHERE i.dataset_version_id = ?
                  AND i.generated_placeholder = TRUE
                """, datasetVersionId);

        long syntheticHydratedItemsUsedInDropsCount = count("""
                SELECT COUNT(DISTINCT i.id)
                FROM item i
                JOIN pokemon_drop pd ON pd.item_id = i.id
                WHERE i.dataset_version_id = ?
                  AND i.generated_placeholder = FALSE
                  AND (i.raw_json ->> 'source') IN (?, ?)
                """,
                datasetVersionId,
                COBBLEMON_SYNTHETIC_SOURCE,
                MINECRAFT_SYNTHETIC_SOURCE
        );

        long duplicateItemNamespacedIdGroupCount = count("""
                SELECT COUNT(*)
                FROM (
                    SELECT namespaced_id
                    FROM item
                    WHERE dataset_version_id = ?
                    GROUP BY namespaced_id
                    HAVING COUNT(*) > 1
                ) t
                """, datasetVersionId);

        long pokemonCount = count("""
                SELECT COUNT(*)
                FROM pokemon
                WHERE dataset_version_id = ?
                """, datasetVersionId);

        long pokemonFormCount = count("""
                SELECT COUNT(*)
                FROM pokemon_form pf
                JOIN pokemon p ON p.id = pf.pokemon_id
                WHERE p.dataset_version_id = ?
                """, datasetVersionId);

        long defaultPokemonFormCount = count("""
                SELECT COUNT(*)
                FROM pokemon_form pf
                JOIN pokemon p ON p.id = pf.pokemon_id
                WHERE p.dataset_version_id = ?
                  AND pf.is_default = TRUE
                """, datasetVersionId);

        long battleOnlyPokemonFormCount = count("""
                SELECT COUNT(*)
                FROM pokemon_form pf
                JOIN pokemon p ON p.id = pf.pokemon_id
                WHERE p.dataset_version_id = ?
                  AND pf.battle_only = TRUE
                """, datasetVersionId);

        long pokemonDropCount = count("""
                SELECT COUNT(*)
                FROM pokemon_drop pd
                JOIN pokemon_form pf ON pf.id = pd.pokemon_form_id
                JOIN pokemon p ON p.id = pf.pokemon_id
                WHERE p.dataset_version_id = ?
                """, datasetVersionId);

        long pokemonFormsWithDropsCount = count("""
                SELECT COUNT(DISTINCT pd.pokemon_form_id)
                FROM pokemon_drop pd
                JOIN pokemon_form pf ON pf.id = pd.pokemon_form_id
                JOIN pokemon p ON p.id = pf.pokemon_id
                WHERE p.dataset_version_id = ?
                """, datasetVersionId);

        long spawnPresetCount = count("""
                SELECT COUNT(*)
                FROM spawn_preset
                WHERE dataset_version_id = ?
                """, datasetVersionId);

        long spawnSourceFileCount = count("""
                SELECT COUNT(*)
                FROM spawn_source_file
                WHERE dataset_version_id = ?
                """, datasetVersionId);

        long spawnRuleCount = count("""
                SELECT COUNT(*)
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                """, datasetVersionId);

        long spawnRuleWithPokemonFormCount = count("""
                SELECT COUNT(*)
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                  AND sr.pokemon_form_id IS NOT NULL
                """, datasetVersionId);

        long spawnRuleWithFormSelectorCount = count("""
                SELECT COUNT(*)
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                  AND sr.form_selector IS NOT NULL
                """, datasetVersionId);

        long spawnRuleWithImplicitBaseFormCount = count("""
                SELECT COUNT(*)
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                  AND sr.pokemon_form_id IS NULL
                  AND sr.form_selector IS NULL
                """, datasetVersionId);

        long spawnRuleWithBothFormAndSelectorCount = count("""
                SELECT COUNT(*)
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                  AND sr.pokemon_form_id IS NOT NULL
                  AND sr.form_selector IS NOT NULL
                """, datasetVersionId);

        long duplicateSpawnRuleExternalIdGroupCount = count("""
                SELECT COUNT(*)
                FROM (
                    SELECT ssf.id, sr.external_id
                    FROM spawn_rule sr
                    JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                    WHERE ssf.dataset_version_id = ?
                    GROUP BY ssf.id, sr.external_id
                    HAVING COUNT(*) > 1
                ) t
                """, datasetVersionId);

        long spawnConditionCount = count("""
                SELECT COUNT(*)
                FROM spawn_condition sc
                JOIN spawn_rule sr ON sr.id = sc.spawn_rule_id
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                """, datasetVersionId);

        long spawnConditionWithJsonCount = count("""
                SELECT COUNT(*)
                FROM spawn_condition sc
                JOIN spawn_rule sr ON sr.id = sc.spawn_rule_id
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                  AND (
                      sc.condition_json IS NOT NULL
                      OR sc.anticondition_json IS NOT NULL
                      OR sc.effective_condition_json IS NOT NULL
                      OR sc.effective_anticondition_json IS NOT NULL
                  )
                """, datasetVersionId);

        long spawnConditionWithoutJsonAndTokensCount = count("""
                SELECT COUNT(*)
                FROM spawn_condition sc
                JOIN spawn_rule sr ON sr.id = sc.spawn_rule_id
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                  AND sc.condition_json IS NULL
                  AND sc.anticondition_json IS NULL
                  AND sc.effective_condition_json IS NULL
                  AND sc.effective_anticondition_json IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM spawn_condition_token sct
                      WHERE sct.spawn_condition_id = sc.id
                  )
                """, datasetVersionId);

        long spawnConditionTokenCount = count("""
                SELECT COUNT(*)
                FROM spawn_condition_token sct
                JOIN spawn_condition sc ON sc.id = sct.spawn_condition_id
                JOIN spawn_rule sr ON sr.id = sc.spawn_rule_id
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                """, datasetVersionId);

        long spawnRulePresetLinkCount = count("""
                SELECT COUNT(*)
                FROM spawn_rule_preset srp
                JOIN spawn_rule sr ON sr.id = srp.spawn_rule_id
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                """, datasetVersionId);

        long seasoningCount = count("""
                SELECT COUNT(*)
                FROM seasoning s
                JOIN item i ON i.id = s.item_id
                WHERE i.dataset_version_id = ?
                """, datasetVersionId);

        long seasoningMobEffectCount = count("""
                SELECT COUNT(*)
                FROM seasoning_mob_effect sme
                JOIN seasoning s ON s.id = sme.seasoning_id
                JOIN item i ON i.id = s.item_id
                WHERE i.dataset_version_id = ?
                """, datasetVersionId);

        long baitEffectCount = count("""
                SELECT COUNT(*)
                FROM bait_effect be
                JOIN item i ON i.id = be.item_id
                WHERE i.dataset_version_id = ?
                """, datasetVersionId);

        Map<String, Long> spawnRulesByType = groupedCounts("""
                SELECT sr.spawn_type AS label, COUNT(*) AS count_value
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                GROUP BY sr.spawn_type
                ORDER BY sr.spawn_type
                """, datasetVersionId);

        Map<String, Long> spawnRulesByBucket = groupedCounts("""
                SELECT sr.bucket AS label, COUNT(*) AS count_value
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                GROUP BY sr.bucket
                ORDER BY sr.bucket
                """, datasetVersionId);

        Map<String, Long> spawnConditionTokensByType = groupedCounts("""
                SELECT sct.token_type AS label, COUNT(*) AS count_value
                FROM spawn_condition_token sct
                JOIN spawn_condition sc ON sc.id = sct.spawn_condition_id
                JOIN spawn_rule sr ON sr.id = sc.spawn_rule_id
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                GROUP BY sct.token_type
                ORDER BY sct.token_type
                """, datasetVersionId);

        List<LabelCount> topFormSelectors = topCounts("""
                SELECT sr.form_selector AS label, COUNT(*) AS count_value
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                WHERE ssf.dataset_version_id = ?
                  AND sr.form_selector IS NOT NULL
                GROUP BY sr.form_selector
                ORDER BY COUNT(*) DESC, sr.form_selector
                LIMIT 20
                """, datasetVersionId);

        List<LabelCount> topPlaceholderItems = topCounts("""
                SELECT i.namespaced_id AS label, COUNT(pd.id) AS count_value
                FROM item i
                LEFT JOIN pokemon_drop pd ON pd.item_id = i.id
                WHERE i.dataset_version_id = ?
                  AND i.generated_placeholder = TRUE
                GROUP BY i.namespaced_id
                ORDER BY COUNT(pd.id) DESC, i.namespaced_id
                LIMIT 20
                """, datasetVersionId);

        List<LabelCount> topSyntheticHydratedItems = topCounts("""
                SELECT i.namespaced_id AS label, COUNT(pd.id) AS count_value
                FROM item i
                LEFT JOIN pokemon_drop pd ON pd.item_id = i.id
                WHERE i.dataset_version_id = ?
                  AND i.generated_placeholder = FALSE
                  AND (i.raw_json ->> 'source') IN (?, ?)
                GROUP BY i.namespaced_id
                ORDER BY COUNT(pd.id) DESC, i.namespaced_id
                LIMIT 20
                """,
                datasetVersionId,
                COBBLEMON_SYNTHETIC_SOURCE,
                MINECRAFT_SYNTHETIC_SOURCE
        );

        List<LabelCount> topSpawnRulesByPokemon = topCounts("""
                SELECT p.slug AS label, COUNT(sr.id) AS count_value
                FROM spawn_rule sr
                JOIN spawn_source_file ssf ON ssf.id = sr.spawn_source_file_id
                JOIN pokemon p ON p.id = sr.pokemon_id
                WHERE ssf.dataset_version_id = ?
                GROUP BY p.slug
                ORDER BY COUNT(sr.id) DESC, p.slug
                LIMIT 20
                """, datasetVersionId);

        List<LabelCount> topDropsByPokemonForm = topCounts("""
                SELECT p.slug || ' [' || pf.code || ']' AS label, COUNT(pd.id) AS count_value
                FROM pokemon_drop pd
                JOIN pokemon_form pf ON pf.id = pd.pokemon_form_id
                JOIN pokemon p ON p.id = pf.pokemon_id
                WHERE p.dataset_version_id = ?
                GROUP BY p.slug, pf.code
                ORDER BY COUNT(pd.id) DESC, p.slug, pf.code
                LIMIT 20
                """, datasetVersionId);

        // Après les autres counts, avant la construction du rapport :

        Map<String, Long> translationsByLocale = groupedCounts("""
    SELECT locale AS label, COUNT(*) AS count_value
    FROM translation
    GROUP BY locale
    ORDER BY locale
""");

        long translationTotal = count("SELECT COUNT(*) FROM translation");

        StringBuilder report = new StringBuilder();
        report.append("\n");
        report.append("============================================================\n");
        report.append("RAPPORT D'IMPORT DATASET COBBLEMON\n");
        report.append("============================================================\n");
        report.append("Dataset version id : ").append(datasetVersionId).append("\n");
        report.append("Code               : ").append(code).append("\n");
        report.append("Label              : ").append(label).append("\n");
        report.append("Input path         : ").append(inputPath).append("\n");
        report.append("Clean before import: ").append(cleanBeforeImport).append("\n");
        report.append("Actif              : ").append(active).append("\n");
        report.append("Imported at        : ").append(importedAt != null ? DATE_TIME_FORMATTER.format(importedAt) : "n/a").append("\n");
        report.append("Started at         : ").append(DATE_TIME_FORMATTER.format(startedAt)).append("\n");
        report.append("Finished at        : ").append(DATE_TIME_FORMATTER.format(finishedAt)).append("\n");
        report.append("Durée              : ").append(formatDuration(Duration.between(startedAt, finishedAt))).append("\n");
        report.append("------------------------------------------------------------\n");
        report.append("ITEMS\n");
        report.append("  - total                          : ").append(itemCount).append("\n");
        report.append("  - items réels source             : ").append(realSourceItemCount)
                .append(" (").append(formatPercent(realSourceItemCount, itemCount)).append(")\n");
        report.append("  - items synthétiques hydratés    : ").append(syntheticHydratedItemCount)
                .append(" (").append(formatPercent(syntheticHydratedItemCount, itemCount)).append(")\n");
        report.append("  - synthétiques utilisés en drops : ").append(syntheticHydratedItemsUsedInDropsCount).append("\n");
        report.append("  - placeholders générés           : ").append(placeholderItemCount)
                .append(" (").append(formatPercent(placeholderItemCount, itemCount)).append(")\n");
        report.append("  - placeholders utilisés en drops : ").append(placeholderItemsUsedInDropsCount).append("\n");
        report.append("  - anomalies doublons items       : ").append(duplicateItemNamespacedIdGroupCount).append("\n");
        report.append("------------------------------------------------------------\n");
        report.append("POKÉMON\n");
        report.append("  - total                          : ").append(pokemonCount).append("\n");
        report.append("  - forms                          : ").append(pokemonFormCount).append("\n");
        report.append("  - forms par défaut               : ").append(defaultPokemonFormCount).append("\n");
        report.append("  - forms battle only              : ").append(battleOnlyPokemonFormCount).append("\n");
        report.append("------------------------------------------------------------\n");
        report.append("DROPS\n");
        report.append("  - pokemon_drop                   : ").append(pokemonDropCount).append("\n");
        report.append("  - forms avec drops               : ").append(pokemonFormsWithDropsCount).append("\n");
        report.append("------------------------------------------------------------\n");
        report.append("SPAWNS\n");
        report.append("  - spawn_presets                  : ").append(spawnPresetCount).append("\n");
        report.append("  - spawn_source_files             : ").append(spawnSourceFileCount).append("\n");
        report.append("  - spawn_rules                    : ").append(spawnRuleCount).append("\n");
        report.append("  - spawn_rules avec form          : ").append(spawnRuleWithPokemonFormCount).append("\n");
        report.append("  - spawn_rules avec formSelector  : ").append(spawnRuleWithFormSelectorCount)
                .append(" (").append(formatPercent(spawnRuleWithFormSelectorCount, spawnRuleCount)).append(")\n");
        report.append("  - spawn_rules forme implicite    : ").append(spawnRuleWithImplicitBaseFormCount).append("\n");
        report.append("  - anomalies form+selector        : ").append(spawnRuleWithBothFormAndSelectorCount).append("\n");
        report.append("  - anomalies externalId dupliqué  : ").append(duplicateSpawnRuleExternalIdGroupCount).append("\n");
        report.append("  - spawn_conditions               : ").append(spawnConditionCount).append("\n");
        report.append("  - conditions avec JSON           : ").append(spawnConditionWithJsonCount).append("\n");
        report.append("  - conditions vides               : ").append(spawnConditionWithoutJsonAndTokensCount).append("\n");
        report.append("  - condition tokens               : ").append(spawnConditionTokenCount).append("\n");
        report.append("  - rule/preset links              : ").append(spawnRulePresetLinkCount).append("\n");
        report.append("  - rules par type                 : ").append(formatMap(spawnRulesByType)).append("\n");
        report.append("  - rules par bucket               : ").append(formatMap(spawnRulesByBucket)).append("\n");
        report.append("  - tokens par type                : ").append(formatMap(spawnConditionTokensByType)).append("\n");
        report.append("------------------------------------------------------------\n");
        report.append("ITEM EFFECTS\n");
        report.append("  - seasonings                     : ").append(seasoningCount).append("\n");
        report.append("  - seasoning_mob_effect           : ").append(seasoningMobEffectCount).append("\n");
        report.append("  - bait_effect                    : ").append(baitEffectCount).append("\n");
        report.append("------------------------------------------------------------\n");
        report.append("TOP 20 FORM SELECTORS\n");
        appendRanked(report, topFormSelectors);
        report.append("------------------------------------------------------------\n");
        report.append("TOP 20 ITEMS SYNTHÉTIQUES HYDRATÉS\n");
        appendRanked(report, topSyntheticHydratedItems);
        report.append("------------------------------------------------------------\n");
        report.append("TOP 20 ITEMS PLACEHOLDERS\n");
        appendRanked(report, topPlaceholderItems);
        report.append("------------------------------------------------------------\n");
        report.append("TOP 20 SPAWN RULES PAR POKÉMON\n");
        appendRanked(report, topSpawnRulesByPokemon);
        report.append("------------------------------------------------------------\n");
        report.append("TOP 20 DROPS PAR POKÉMON/FORME\n");
        appendRanked(report, topDropsByPokemonForm);
        report.append("============================================================");
        report.append("------------------------------------------------------------\n");
        report.append("TRADUCTIONS\n");
        report.append("  - total                          : ").append(translationTotal).append("\n");
        for (Map.Entry<String, Long> entry : translationsByLocale.entrySet()) {
            report.append(String.format("  - locale %-28s : %d%n", entry.getKey(), entry.getValue()));
        }

        log.info(report.toString());
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value != null ? value : 0L;
    }

    private boolean isActive(Long datasetVersionId) {
        Boolean value = jdbcTemplate.queryForObject(
                "SELECT is_active FROM dataset_version WHERE id = ?",
                Boolean.class,
                datasetVersionId
        );
        return Boolean.TRUE.equals(value);
    }

    private Instant findImportedAt(Long datasetVersionId) {
        Timestamp value = jdbcTemplate.queryForObject(
                "SELECT imported_at FROM dataset_version WHERE id = ?",
                Timestamp.class,
                datasetVersionId
        );
        return value != null ? value.toInstant() : null;
    }

    private Map<String, Long> groupedCounts(String sql, Object... args) {
        return jdbcTemplate.query(sql, rs -> {
            Map<String, Long> result = new java.util.LinkedHashMap<>();
            while (rs.next()) {
                result.put(
                        normalizeLabel(rs.getString("label")),
                        rs.getLong("count_value")
                );
            }
            return result;
        }, args);
    }

    private List<LabelCount> topCounts(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new LabelCount(
                                normalizeLabel(rs.getString("label")),
                                rs.getLong("count_value")
                        ),
                args
        );
    }

    private void appendRanked(StringBuilder report, List<LabelCount> rows) {
        if (rows.isEmpty()) {
            report.append("  - aucun\n");
            return;
        }

        for (int index = 0; index < rows.size(); index++) {
            LabelCount row = rows.get(index);
            report.append(String.format(Locale.ROOT, "  %2d. %-50s %d%n", index + 1, row.label(), row.count()));
        }
    }

    private String formatMap(Map<String, Long> values) {
        if (values.isEmpty()) {
            return "{}";
        }

        StringBuilder builder = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Long> entry : values.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }

        builder.append("}");
        return builder.toString();
    }

    private String formatPercent(long part, long total) {
        if (total <= 0) {
            return "0.00%";
        }

        double value = (part * 100.0d) / total;
        return String.format(Locale.ROOT, "%.2f%%", value);
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long millis = duration.toMillisPart();

        return String.format(Locale.ROOT, "%02dh %02dm %02ds %03dms", hours, minutes, seconds, millis);
    }

    private String normalizeLabel(String label) {
        return label == null || label.isBlank() ? "<null>" : label.trim();
    }

    private record LabelCount(String label, long count) {
    }
}
