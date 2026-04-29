package be.loic.tfe_cobblemon.spawn.repository;

import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpawnRuleRepository extends JpaRepository<SpawnRule, Long> {
    Optional<SpawnRule> findBySpawnSourceFileIdAndExternalId(Long spawnSourceFileId, String externalId);

    boolean existsBySpawnSourceFileIdAndExternalId(Long spawnSourceFileId, String externalId);

    @EntityGraph(attributePaths = {
            "pokemon",
            "pokemonForm",
            "spawnSourceFile",
            "presets",
            "spawnCondition",
            "spawnCondition.tokens"
    })
    Optional<SpawnRule> findById(Long id);

    @EntityGraph(attributePaths = {
            "pokemon",
            "pokemonForm",
            "spawnSourceFile",
            "presets",
            "spawnCondition",
            "spawnCondition.tokens"
    })
    List<SpawnRule> findAllByPokemonIdOrderByIdAsc(Long pokemonId);

    @EntityGraph(attributePaths = {
            "pokemon",
            "pokemonForm",
            "spawnSourceFile",
            "presets",
            "spawnCondition",
            "spawnCondition.tokens"
    })
    List<SpawnRule> findAllByPokemonIdAndPokemonFormIdOrderByIdAsc(Long pokemonId, Long pokemonFormId);

    @Query("""
        select sr
        from SpawnRule sr
        join fetch sr.pokemon p
        join fetch sr.spawnSourceFile ssf
        left join fetch sr.pokemonForm pf
        where p.slug = :slug
          and p.datasetVersion.id = :datasetVersionId
        order by sr.weight desc, sr.bucket asc
    """)
    List<SpawnRule> findAllByPokemonSlugAndDatasetVersionId(String slug, Long datasetVersionId);
}