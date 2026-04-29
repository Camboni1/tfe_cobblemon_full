package be.loic.tfe_cobblemon.dataset.importer.service.impl;

import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.service.DatasetCleanupService;
import be.loic.tfe_cobblemon.dataset.repository.DatasetVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatasetCleanupServiceImpl implements DatasetCleanupService {

    private final JdbcTemplate jdbcTemplate;
    private final DatasetVersionRepository datasetVersionRepository;

    @Override
    @Transactional
    public void deleteDatasetContent(Long datasetVersionId) {
        DatasetVersion datasetVersion = datasetVersionRepository.findById(datasetVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset version not found for id: " + datasetVersionId));

        if (datasetVersion.isActive()) {
            throw new IllegalStateException(
                    "Refus de nettoyer une dataset_version active. "
                            + "Importe dans une nouvelle version ou active la version seulement à la fin."
            );
        }

        deleteSpawnRulePreset(datasetVersionId);
        deleteSpawnConditionToken(datasetVersionId);
        deleteSpawnCondition(datasetVersionId);
        deletePokemonDrop(datasetVersionId);
        deleteSeasoningMobEffect(datasetVersionId);
        deleteBaitEffect(datasetVersionId);
        deleteSeasoning(datasetVersionId);
        deleteSpawnRule(datasetVersionId);
        deleteSpawnSourceFile(datasetVersionId);
        deleteSpawnPreset(datasetVersionId);
        deletePokemonForm(datasetVersionId);
        deletePokemon(datasetVersionId);
        deleteItem(datasetVersionId);
    }

    private void deleteSpawnRulePreset(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from spawn_rule_preset srp
                where exists (
                    select 1
                    from spawn_rule sr
                    join spawn_source_file ssf on ssf.id = sr.spawn_source_file_id
                    where sr.id = srp.spawn_rule_id
                      and ssf.dataset_version_id = ?
                )
                or exists (
                    select 1
                    from spawn_preset sp
                    where sp.id = srp.spawn_preset_id
                      and sp.dataset_version_id = ?
                )
                """, datasetVersionId, datasetVersionId);
    }

    private void deleteSpawnConditionToken(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from spawn_condition_token sct
                where exists (
                    select 1
                    from spawn_condition sc
                    join spawn_rule sr on sr.id = sc.spawn_rule_id
                    join spawn_source_file ssf on ssf.id = sr.spawn_source_file_id
                    where sc.id = sct.spawn_condition_id
                      and ssf.dataset_version_id = ?
                )
                """, datasetVersionId);
    }

    private void deleteSpawnCondition(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from spawn_condition sc
                where exists (
                    select 1
                    from spawn_rule sr
                    join spawn_source_file ssf on ssf.id = sr.spawn_source_file_id
                    where sr.id = sc.spawn_rule_id
                      and ssf.dataset_version_id = ?
                )
                """, datasetVersionId);
    }

    private void deletePokemonDrop(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from pokemon_drop pd
                where exists (
                    select 1
                    from pokemon_form pf
                    join pokemon p on p.id = pf.pokemon_id
                    where pf.id = pd.pokemon_form_id
                      and p.dataset_version_id = ?
                )
                """, datasetVersionId);
    }

    private void deleteSeasoningMobEffect(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from seasoning_mob_effect sme
                where exists (
                    select 1
                    from seasoning s
                    join item i on i.id = s.item_id
                    where s.id = sme.seasoning_id
                      and i.dataset_version_id = ?
                )
                """, datasetVersionId);
    }

    private void deleteBaitEffect(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from bait_effect be
                where exists (
                    select 1
                    from item i
                    where i.id = be.item_id
                      and i.dataset_version_id = ?
                )
                """, datasetVersionId);
    }

    private void deleteSeasoning(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from seasoning s
                where exists (
                    select 1
                    from item i
                    where i.id = s.item_id
                      and i.dataset_version_id = ?
                )
                """, datasetVersionId);
    }

    private void deleteSpawnRule(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from spawn_rule sr
                where exists (
                    select 1
                    from spawn_source_file ssf
                    where ssf.id = sr.spawn_source_file_id
                      and ssf.dataset_version_id = ?
                )
                """, datasetVersionId);
    }

    private void deleteSpawnSourceFile(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from spawn_source_file
                where dataset_version_id = ?
                """, datasetVersionId);
    }

    private void deleteSpawnPreset(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from spawn_preset
                where dataset_version_id = ?
                """, datasetVersionId);
    }

    private void deletePokemonForm(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from pokemon_form pf
                where exists (
                    select 1
                    from pokemon p
                    where p.id = pf.pokemon_id
                      and p.dataset_version_id = ?
                )
                """, datasetVersionId);
    }

    private void deletePokemon(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from pokemon
                where dataset_version_id = ?
                """, datasetVersionId);
    }

    private void deleteItem(Long datasetVersionId) {
        jdbcTemplate.update("""
                delete from item
                where dataset_version_id = ?
                """, datasetVersionId);
    }
}