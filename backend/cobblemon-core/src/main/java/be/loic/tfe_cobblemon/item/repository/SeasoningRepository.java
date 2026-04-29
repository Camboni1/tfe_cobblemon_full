package be.loic.tfe_cobblemon.item.repository;

import be.loic.tfe_cobblemon.item.entity.Seasoning;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeasoningRepository extends JpaRepository<Seasoning, Long> {

    Optional<Seasoning> findByItemId(Long itemId);

    boolean existsByItemId(Long itemId);

    @Override
    @EntityGraph(attributePaths = {"item", "mobEffects"})
    Optional<Seasoning> findById(Long id);

    @EntityGraph(attributePaths = {"item", "mobEffects"})
    Optional<Seasoning> findOneByItemId(Long itemId);
}