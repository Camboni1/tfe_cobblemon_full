package be.loic.tfe_cobblemon.spawn.repository;

import be.loic.tfe_cobblemon.spawn.entity.SpawnCondition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpawnConditionRepository extends JpaRepository<SpawnCondition, Long> {

    Optional<SpawnCondition> findBySpawnRuleId(Long spawnRuleId);

    boolean existsBySpawnRuleId(Long spawnRuleId);

    @EntityGraph(attributePaths = {"tokens"})
    Optional<SpawnCondition> findOneBySpawnRuleId(Long spawnRuleId);
}