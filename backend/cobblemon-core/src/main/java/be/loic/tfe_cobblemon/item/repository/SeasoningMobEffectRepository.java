package be.loic.tfe_cobblemon.item.repository;

import be.loic.tfe_cobblemon.item.entity.SeasoningMobEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasoningMobEffectRepository extends JpaRepository<SeasoningMobEffect, Long> {

    List<SeasoningMobEffect> findAllBySeasoningIdOrderByIdAsc(Long seasoningId);
}