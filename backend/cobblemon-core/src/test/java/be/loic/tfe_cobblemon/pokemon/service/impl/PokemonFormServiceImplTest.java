// =========================================================================
// PokemonFormServiceImplTest.java
// =========================================================================
package be.loic.tfe_cobblemon.pokemon.service.impl;

import be.loic.tfe_cobblemon.common.asset.AssetUrlResolver;
import be.loic.tfe_cobblemon.common.exception.BusinessValidationException;
import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import be.loic.tfe_cobblemon.drop.repository.PokemonDropRepository;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonFormRepository;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import be.loic.tfe_cobblemon.pokemon.service.command.CreatePokemonFormCommand;
import be.loic.tfe_cobblemon.pokemon.service.command.UpdatePokemonFormCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PokemonFormServiceImplTest {

    @Mock PokemonRepository pokemonRepository;
    @Mock PokemonFormRepository pokemonFormRepository;
    @Mock PokemonDropRepository pokemonDropRepository;
    @Mock DatasetVersionService datasetVersionService;
    @Mock AssetUrlResolver assetUrlResolver;
    @InjectMocks PokemonFormServiceImpl service;

    private Pokemon bulbasaur;

    @BeforeEach
    void setup() {
        bulbasaur = new Pokemon();
        bulbasaur.setId(1L);
        bulbasaur.setSlug("bulbasaur");

        when(datasetVersionService.getActiveDatasetVersionId()).thenReturn(1L);
        when(pokemonRepository.findByDatasetVersionIdAndSlug(1L, "bulbasaur"))
                .thenReturn(Optional.of(bulbasaur));
        when(pokemonDropRepository.findAllByPokemonFormIdOrderByIdAsc(any()))
                .thenReturn(List.of());
    }

    private CreatePokemonFormCommand buildCreateCommand(String code) {
        return new CreatePokemonFormCommand(
                code, "Méga Bulbizarre", false, false,
                "grass", "poison",
                (short) 80, (short) 100, (short) 123,
                (short) 122, (short) 120, (short) 80
        );
    }

    // PokemonFormResponse — 14 champs (+ drops)
    private PokemonForm buildSavedForm(String code) {
        PokemonForm form = new PokemonForm();
        form.setId(10L);
        form.setPokemon(bulbasaur);
        form.setCode(code);
        form.setDisplayName("Méga Bulbizarre");
        form.setIsDefault(false);
        form.setBattleOnly(false);
        form.setPrimaryType("grass");
        form.setSecondaryType("poison");
        form.setBaseHp((short) 80);
        form.setBaseAttack((short) 100);
        form.setBaseDefense((short) 123);
        form.setBaseSpecialAttack((short) 122);
        form.setBaseSpecialDefense((short) 120);
        form.setBaseSpeed((short) 80);
        form.setRawJson("{}");
        return form;
    }

    @Test
    void create_sauvegarde_et_retourne_la_forme() {
        when(pokemonFormRepository.existsByPokemonIdAndCode(1L, "mega")).thenReturn(false);
        when(pokemonFormRepository.save(any())).thenReturn(buildSavedForm("mega"));

        var result = service.create("bulbasaur", buildCreateCommand("mega"));

        assertThat(result.code()).isEqualTo("mega");
        assertThat(result.displayName()).isEqualTo("Méga Bulbizarre");
        assertThat(result.baseHp()).isEqualTo((short) 80);
    }

    @Test
    void create_lance_exception_si_code_deja_existant() {
        when(pokemonFormRepository.existsByPokemonIdAndCode(1L, "mega")).thenReturn(true);

        assertThatThrownBy(() -> service.create("bulbasaur", buildCreateCommand("mega")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("mega");
    }

    @Test
    void create_lance_exception_si_pokemon_introuvable() {
        when(pokemonRepository.findByDatasetVersionIdAndSlug(1L, "inexistant"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("inexistant", buildCreateCommand("default")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_modifie_et_retourne_la_forme() {
        PokemonForm existing = new PokemonForm();
        existing.setId(5L);
        existing.setCode("default");
        existing.setRawJson("{}");

        PokemonForm updated = buildSavedForm("default");
        updated.setId(5L);
        updated.setDisplayName("Bulbizarre Modifié");
        updated.setBaseHp((short) 50);

        when(pokemonFormRepository.findByPokemonIdAndCode(1L, "default"))
                .thenReturn(Optional.of(existing));
        when(pokemonFormRepository.save(any())).thenReturn(updated);

        var command = new UpdatePokemonFormCommand(
                "Bulbizarre Modifié", true, false, "grass", null,
                (short) 50, (short) 55, (short) 55,
                (short) 70, (short) 70, (short) 50
        );

        var result = service.update("bulbasaur", "default", command);

        assertThat(result.displayName()).isEqualTo("Bulbizarre Modifié");
        assertThat(result.baseHp()).isEqualTo((short) 50);
    }

    @Test
    void update_lance_exception_si_forme_introuvable() {
        when(pokemonFormRepository.findByPokemonIdAndCode(1L, "inexistant"))
                .thenReturn(Optional.empty());

        var command = new UpdatePokemonFormCommand(
                "Test", false, false, "fire", null,
                (short) 45, (short) 49, (short) 49,
                (short) 65, (short) 65, (short) 45
        );

        assertThatThrownBy(() -> service.update("bulbasaur", "inexistant", command))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_supprime_la_forme_non_default() {
        PokemonForm form = buildSavedForm("mega");
        form.setIsDefault(false);

        when(pokemonFormRepository.findByPokemonIdAndCode(1L, "mega"))
                .thenReturn(Optional.of(form));
        when(pokemonFormRepository.findAllByPokemonIdOrderByIsDefaultDescDisplayNameAsc(1L))
                .thenReturn(List.of(new PokemonForm(), form));

        service.delete("bulbasaur", "mega");

        verify(pokemonFormRepository).delete(form);
    }

    @Test
    void delete_empeche_suppression_forme_default_si_dautres_formes_existent() {
        PokemonForm defaultForm = buildSavedForm("default");
        defaultForm.setIsDefault(true);

        when(pokemonFormRepository.findByPokemonIdAndCode(1L, "default"))
                .thenReturn(Optional.of(defaultForm));
        when(pokemonFormRepository.findAllByPokemonIdOrderByIsDefaultDescDisplayNameAsc(1L))
                .thenReturn(List.of(defaultForm, new PokemonForm())); // 2 formes

        assertThatThrownBy(() -> service.delete("bulbasaur", "default"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("défaut");
    }
}
