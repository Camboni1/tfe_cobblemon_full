package be.loic.tfe_cobblemon.dataset.importer.service.impl;

import be.loic.tfe_cobblemon.common.io.ConfiguredPathResolver;
import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.drop.service.PokemonDropImportService;
import be.loic.tfe_cobblemon.dataset.importer.item.service.BaitEffectImportService;
import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemImportService;
import be.loic.tfe_cobblemon.dataset.importer.item.service.SeasoningImportService;
import be.loic.tfe_cobblemon.dataset.importer.pokemon.service.PokemonFormImportService;
import be.loic.tfe_cobblemon.dataset.importer.pokemon.service.PokemonImportService;
import be.loic.tfe_cobblemon.dataset.importer.service.DatasetCleanupService;
import be.loic.tfe_cobblemon.dataset.importer.service.DatasetImportReportService;
import be.loic.tfe_cobblemon.dataset.importer.service.DatasetRefreshService;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnConditionImportService;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnPresetImportService;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnRuleImportService;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnRulePresetImportService;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnSourceFileImportService;
import be.loic.tfe_cobblemon.dataset.importer.translation.service.TranslationImportService;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DatasetRefreshServiceImpl implements DatasetRefreshService {

    private static final Logger log = LoggerFactory.getLogger(DatasetRefreshServiceImpl.class);

    private final DatasetVersionService datasetVersionService;
    private final DatasetCleanupService datasetCleanupService;
    private final ItemImportService itemImportService;
    private final PokemonImportService pokemonImportService;
    private final PokemonFormImportService pokemonFormImportService;
    private final PokemonDropImportService pokemonDropImportService;
    private final SpawnPresetImportService spawnPresetImportService;
    private final SpawnSourceFileImportService spawnSourceFileImportService;
    private final SpawnRuleImportService spawnRuleImportService;
    private final SpawnConditionImportService spawnConditionImportService;
    private final SpawnRulePresetImportService spawnRulePresetImportService;
    private final SeasoningImportService seasoningImportService;
    private final BaitEffectImportService baitEffectImportService;
    private final DatasetImportReportService datasetImportReportService;
    private final TransactionTemplate transactionTemplate;
    private final TranslationImportService translationImportService;

    @Override
    public void refresh(String code, String label, String inputPath, boolean cleanBeforeImport) {
        Instant startedAt = Instant.now();
        Long datasetVersionId = null;

        log.info("============================================================");
        log.info("DEMARRAGE IMPORT DATASET");
        log.info("code={} | label={} | inputPath={} | cleanBeforeImport={}", code, label, inputPath, cleanBeforeImport);
        log.info("============================================================");

        try {
            Path root = validateInputDirectory(inputPath);

            DatasetVersion datasetVersion = datasetVersionService.createOrUpdateForImport(code, label);
            datasetVersionId = datasetVersion.getId();

            boolean shouldDeactivate = cleanBeforeImport && datasetVersion.isActive();
            int totalSteps = 11 + 1 + 1 + (cleanBeforeImport ? 1 : 0) + (shouldDeactivate ? 1 : 0);
            int currentStep = 0;

            log.info(
                    "Import pret | datasetVersionId={} | totalSteps={}",
                    datasetVersionId,
                    totalSteps
            );
                currentStep++;
                runStep(
                        currentStep,
                        totalSteps,
                        "Desactivation du dataset actif",
                        startedAt,
                        () -> {
                            datasetVersionService.deactivate(datasetVersion.getId());
                            datasetVersion.setActive(false);
                        }
                );


            if (cleanBeforeImport) {
                currentStep++;
                runStep(
                        currentStep,
                        totalSteps,
                        "Nettoyage des donnees existantes du dataset",
                        startedAt,
                        () -> datasetCleanupService.deleteDatasetContent(datasetVersion.getId())
                );
            }

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des items",
                    startedAt,
                    () -> itemImportService.importItems(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des pokemons",
                    startedAt,
                    () -> pokemonImportService.importPokemon(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des forms de pokemons",
                    startedAt,
                    () -> pokemonFormImportService.importPokemonForms(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des drops de pokemons",
                    startedAt,
                    () -> pokemonDropImportService.importPokemonDrops(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des spawn presets",
                    startedAt,
                    () -> spawnPresetImportService.importSpawnPresets(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des fichiers source de spawn",
                    startedAt,
                    () -> spawnSourceFileImportService.importSpawnSourceFiles(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des spawn rules",
                    startedAt,
                    () -> spawnRuleImportService.importSpawnRules(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des spawn conditions",
                    startedAt,
                    () -> spawnConditionImportService.importSpawnConditions(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des liens spawn rule / preset",
                    startedAt,
                    () -> spawnRulePresetImportService.importSpawnRulePresets(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des seasonings",
                    startedAt,
                    () -> seasoningImportService.importSeasonings(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des bait effects",
                    startedAt,
                    () -> baitEffectImportService.importBaitEffects(datasetVersion, root)
            );

            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Import des traductions (en/fr)",
                    startedAt,
                    () -> translationImportService.importTranslations(datasetVersion, root)
            );


            currentStep++;
            runStep(
                    currentStep,
                    totalSteps,
                    "Activation de la version du dataset et generation du rapport final",
                    startedAt,
                    () -> transactionTemplate.executeWithoutResult(status -> {
                        datasetVersionService.activate(datasetVersion.getId());
                        datasetImportReportService.logFinalReport(
                                datasetVersion.getId(),
                                code,
                                label,
                                inputPath,
                                cleanBeforeImport,
                                startedAt
                        );
                    })
            );

            log.info("============================================================");
            log.info(
                    "IMPORT DATASET TERMINE | datasetVersionId={} | duree={}",
                    datasetVersion.getId(),
                    formatDuration(Duration.between(startedAt, Instant.now()))
            );
            log.info("============================================================");
        } catch (RuntimeException ex) {
            log.error(
                    "IMPORT DATASET ECHOUE | datasetVersionId={} | duree={} | message={}",
                    datasetVersionId,
                    formatDuration(Duration.between(startedAt, Instant.now())),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    private void runStep(
            int currentStep,
            int totalSteps,
            String label,
            Instant importStartedAt,
            Runnable action
    ) {
        Instant stepStartedAt = Instant.now();

        log.info(
                "[IMPORT][{}/{} - {}%] {} | demarrage",
                currentStep,
                totalSteps,
                percentage(currentStep, totalSteps),
                label
        );

        try {
            action.run();
        } catch (RuntimeException ex) {
            log.error(
                    "[IMPORT][{}/{} - {}%] {} | echec apres {}",
                    currentStep,
                    totalSteps,
                    percentage(currentStep, totalSteps),
                    label,
                    formatDuration(Duration.between(stepStartedAt, Instant.now())),
                    ex
            );
            throw ex;
        }

        log.info(
                "[IMPORT][{}/{} - {}%] {} | termine en {} | duree totale={}",
                currentStep,
                totalSteps,
                percentage(currentStep, totalSteps),
                label,
                formatDuration(Duration.between(stepStartedAt, Instant.now())),
                formatDuration(Duration.between(importStartedAt, Instant.now()))
        );
    }

    private int percentage(int currentStep, int totalSteps) {
        if (totalSteps <= 0) {
            return 100;
        }
        return (int) Math.round((currentStep * 100.0) / totalSteps);
    }

    private String formatDuration(Duration duration) {
        long totalMillis = duration.toMillis();
        long hours = totalMillis / 3_600_000;
        long minutes = (totalMillis % 3_600_000) / 60_000;
        long seconds = (totalMillis % 60_000) / 1_000;
        long millis = totalMillis % 1_000;

        return String.format("%02dh %02dm %02ds %03dms", hours, minutes, seconds, millis);
    }

    private Path validateInputDirectory(String inputPath) {
        Path root = ConfiguredPathResolver.resolve(inputPath);

        if (!Files.exists(root)) {
            throw new IllegalStateException("Le dossier d'import n'existe pas : " + inputPath);
        }

        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Le chemin d'import n'est pas un dossier : " + inputPath);
        }

        return root;
    }
}
