package be.loic.tfe_cobblemon.spawn.repository;

import be.loic.tfe_cobblemon.spawn.entity.SpawnPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpawnPresetRepository extends JpaRepository<SpawnPreset, Long> {

    Optional<SpawnPreset> findByDatasetVersionIdAndCode(Long datasetVersionId, String code);

    boolean existsByDatasetVersionIdAndCode(Long datasetVersionId, String code);

    List<SpawnPreset> findAllByDatasetVersionIdOrderByCodeAsc(Long datasetVersionId);
}
