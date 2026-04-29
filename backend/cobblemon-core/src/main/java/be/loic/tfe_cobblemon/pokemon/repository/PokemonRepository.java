package be.loic.tfe_cobblemon.pokemon.repository;

import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PokemonRepository extends JpaRepository<Pokemon, Long>, JpaSpecificationExecutor<Pokemon> {

    Optional<Pokemon> findByDatasetVersionIdAndNationalDexNumber(Long datasetVersionId, Short nationalDexNumber);

    boolean existsByDatasetVersionIdAndSlug(Long datasetVersionId, String slug);

    boolean existsByDatasetVersionIdAndNationalDexNumber(Long datasetVersionId, Short nationalDexNumber);

    List<Pokemon> findAllByDatasetVersionIdOrderByNationalDexNumberAsc(Long datasetVersionId);

    List<Pokemon> findByDatasetVersionIdAndDisplayNameContainingIgnoreCaseOrderByNationalDexNumberAsc(
            Long datasetVersionId,
            String displayName
    );
    Optional<Pokemon> findById(Long id);
    @EntityGraph(attributePaths = "forms")
    Optional<Pokemon> findByDatasetVersionIdAndSlug(Long datasetVersionId, String slug);

}