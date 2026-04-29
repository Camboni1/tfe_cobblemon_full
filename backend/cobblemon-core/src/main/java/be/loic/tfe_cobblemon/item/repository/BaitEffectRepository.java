package be.loic.tfe_cobblemon.item.repository;

import be.loic.tfe_cobblemon.item.entity.BaitEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaitEffectRepository extends JpaRepository<BaitEffect, Long> {

    List<BaitEffect> findAllByItemIdOrderByIdAsc(Long itemId);

    List<BaitEffect> findAllByEffectTypeAndSubcategoryOrderByIdAsc(String effectType, String subcategory);

    List<BaitEffect> findAllByEffectTypeOrderByIdAsc(String effectType);

    boolean existsByItemId(Long itemId);
}
