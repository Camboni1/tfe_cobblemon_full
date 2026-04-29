package be.loic.tfe_cobblemon.dataset.repository;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, Long> {

    Optional<DatasetVersion> findByCode(String code);

    boolean existsByCode(String code);

    Optional<DatasetVersion> findByIsActiveTrue();

    List<DatasetVersion> findAllByOrderByImportedAtDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DatasetVersion dv
            set dv.isActive = false
            where dv.isActive = true
              and dv.id <> :datasetVersionId
            """)
    int deactivateOtherActiveVersions(@Param("datasetVersionId") Long datasetVersionId);
}
