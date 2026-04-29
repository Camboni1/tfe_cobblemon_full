package be.loic.tfe_cobblemon.spawn.repository;

import be.loic.tfe_cobblemon.spawn.entity.SpawnConditionToken;
import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpawnConditionTokenRepository extends JpaRepository<SpawnConditionToken, Long> {

    List<SpawnConditionToken> findAllBySpawnConditionIdOrderByIdAsc(Long spawnConditionId);

    List<SpawnConditionToken> findAllByTokenTypeAndTokenValueIgnoreCaseOrderByIdAsc(
            SpawnConditionTokenType tokenType,
            String tokenValue
    );

    // Liste tous les biomes distincts (côté condition uniquement)
    @Query("""
    select distinct t.tokenValue
    from SpawnConditionToken t
    where t.tokenType = be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenType.BIOME
      and t.side = be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenSide.CONDITION
      and lower(t.tokenValue) like :searchPattern
    order by t.tokenValue asc
""")
    List<String> findDistinctBiomeValues(@Param("searchPattern") String searchPattern);

    // Pokémon qui spawnent dans un biome donné (via le slug exact du token)
    @Query("""
        select distinct sr.pokemon.slug
        from SpawnConditionToken t
        join t.spawnCondition sc
        join sc.spawnRule sr
        join sr.pokemon p
        where t.tokenType = be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenType.BIOME
          and t.side = be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenSide.CONDITION
          and lower(t.tokenValue) = lower(:biomeValue)
          and p.datasetVersion.id = :datasetVersionId
        order by sr.pokemon.slug asc
    """)
    List<String> findPokemonSlugsByBiomeValue(
            @Param("biomeValue") String biomeValue,
            @Param("datasetVersionId") Long datasetVersionId
    );
}