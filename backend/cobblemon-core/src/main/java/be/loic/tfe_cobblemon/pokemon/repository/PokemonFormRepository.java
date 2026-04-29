package be.loic.tfe_cobblemon.pokemon.repository;

import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PokemonFormRepository extends JpaRepository<PokemonForm, Long> {
    Optional<PokemonForm> findByPokemonIdAndCode(Long pokemonId, String code);

    Optional<PokemonForm> findByPokemonIdAndIsDefaultTrue(Long pokemonId);

    Optional<PokemonForm> findByIdAndPokemonId(Long id, Long pokemonId);

    boolean existsByPokemonIdAndCode(Long pokemonId, String code);

    List<PokemonForm> findAllByPokemonIdOrderByIsDefaultDescDisplayNameAsc(Long pokemonId);
}
