package be.loic.tfe_cobblemon.drop.repository;

import be.loic.tfe_cobblemon.drop.entity.PokemonDrop;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PokemonDropRepository extends JpaRepository<PokemonDrop, Long> {

    @EntityGraph(attributePaths = {"item", "pokemonForm"})
    List<PokemonDrop> findAllByPokemonFormIdOrderByIdAsc(Long pokemonFormId);

    @EntityGraph(attributePaths = {"item", "pokemonForm"})
    List<PokemonDrop> findAllByItemIdOrderByIdAsc(Long itemId);

    boolean existsByItemId(Long itemId);
}
