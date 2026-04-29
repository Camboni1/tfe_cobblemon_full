package be.loic.tfe_cobblemon.item.repository;

import be.loic.tfe_cobblemon.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByDatasetVersionIdAndNamespacedId(Long datasetVersionId, String namespacedId);

    boolean existsByDatasetVersionIdAndNamespacedId(Long datasetVersionId, String namespacedId);

    List<Item> findAllByDatasetVersionIdOrderByNamespacedIdAsc(Long datasetVersionId);

    List<Item> findAllByDatasetVersionIdAndGeneratedPlaceholderTrueOrderByNamespacedIdAsc(Long datasetVersionId);

    List<Item> findByDatasetVersionIdAndDisplayNameContainingIgnoreCaseOrderByNamespacedIdAsc(
            Long datasetVersionId,
            String displayName
    );
}
