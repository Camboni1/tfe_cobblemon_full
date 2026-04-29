package be.loic.tfe_cobblemon.spawn.repository;

import be.loic.tfe_cobblemon.spawn.entity.SpawnSourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpawnSourceFileRepository extends JpaRepository<SpawnSourceFile, Long> {

    Optional<SpawnSourceFile> findByDatasetVersionIdAndFilename(Long datasetVersionId, String filename);

    boolean existsByDatasetVersionIdAndFilename(Long datasetVersionId, String filename);

    List<SpawnSourceFile> findAllByDatasetVersionIdOrderByFilenameAsc(Long datasetVersionId);

}
