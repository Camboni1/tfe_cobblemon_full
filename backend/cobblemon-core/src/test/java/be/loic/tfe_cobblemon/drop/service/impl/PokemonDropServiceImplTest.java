// =========================================================================
// PokemonDropServiceImplTest.java
// =========================================================================
package be.loic.tfe_cobblemon.drop.service.impl;

import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import be.loic.tfe_cobblemon.drop.entity.PokemonDrop;
import be.loic.tfe_cobblemon.drop.repository.PokemonDropRepository;
import be.loic.tfe_cobblemon.drop.service.command.CreatePokemonDropCommand;
import be.loic.tfe_cobblemon.drop.service.command.UpdatePokemonDropCommand;
import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.item.repository.ItemRepository;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonFormRepository;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PokemonDropServiceImplTest {

    @Mock PokemonDropRepository pokemonDropRepository;
    @Mock PokemonRepository pokemonRepository;
    @Mock PokemonFormRepository pokemonFormRepository;
    @Mock ItemRepository itemRepository;
    @Mock DatasetVersionService datasetVersionService;
    @InjectMocks PokemonDropServiceImpl service;

    private PokemonForm form;
    private Item item;

    @BeforeEach
    void setup() {
        when(datasetVersionService.getActiveDatasetVersionId()).thenReturn(1L);

        Pokemon pokemon = new Pokemon();
        pokemon.setId(1L);
        pokemon.setSlug("bulbasaur");

        form = new PokemonForm();
        form.setId(10L);
        form.setCode("default");

        item = new Item();
        item.setId(5L);
        item.setNamespacedId("minecraft:vine");
        item.setDisplayName("Vine");
        item.setGeneratedPlaceholder(false);

        when(pokemonRepository.findByDatasetVersionIdAndSlug(1L, "bulbasaur"))
                .thenReturn(Optional.of(pokemon));
        when(pokemonFormRepository.findByPokemonIdAndCode(1L, "default"))
                .thenReturn(Optional.of(form));
        when(itemRepository.findByDatasetVersionIdAndNamespacedId(1L, "minecraft:vine"))
                .thenReturn(Optional.of(item));
    }

    private PokemonDrop buildDrop(Long id, Short qMin, Short qMax, BigDecimal pct) {
        PokemonDrop drop = new PokemonDrop();
        drop.setId(id);
        drop.setPokemonForm(form);
        drop.setItem(item);
        drop.setQuantityMin(qMin);
        drop.setQuantityMax(qMax);
        drop.setPercentage(pct);
        drop.setRawJson("{}");
        return drop;
    }

    @Test
    void create_sauvegarde_et_retourne_le_drop() {
        when(pokemonDropRepository.save(any()))
                .thenReturn(buildDrop(100L, (short) 1, (short) 3, BigDecimal.valueOf(50)));

        var result = service.create("bulbasaur", "default",
                new CreatePokemonDropCommand("minecraft:vine", (short) 1, (short) 3,
                        BigDecimal.valueOf(50), null, null));

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.itemNamespacedId()).isEqualTo("minecraft:vine");
        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void create_lance_exception_si_item_introuvable() {
        when(itemRepository.findByDatasetVersionIdAndNamespacedId(1L, "minecraft:inexistant"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("bulbasaur", "default",
                new CreatePokemonDropCommand("minecraft:inexistant", (short) 1, (short) 1,
                        BigDecimal.valueOf(100), null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("minecraft:inexistant");
    }

    @Test
    void create_lance_exception_si_forme_introuvable() {
        when(pokemonFormRepository.findByPokemonIdAndCode(1L, "mega"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("bulbasaur", "mega",
                new CreatePokemonDropCommand("minecraft:vine", (short) 1, (short) 1,
                        BigDecimal.valueOf(100), null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_modifie_le_drop() {
        PokemonDrop existing = buildDrop(1L, (short) 1, (short) 1, BigDecimal.valueOf(100));
        PokemonDrop updated = buildDrop(1L, (short) 2, (short) 5, BigDecimal.valueOf(75));

        when(pokemonDropRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(pokemonDropRepository.save(any())).thenReturn(updated);

        var result = service.update(1L,
                new UpdatePokemonDropCommand((short) 2, (short) 5,
                        BigDecimal.valueOf(75), null, null));

        assertThat(result.quantityMin()).isEqualTo((short) 2);
        assertThat(result.quantityMax()).isEqualTo((short) 5);
        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.valueOf(75));
    }

    @Test
    void update_lance_exception_si_drop_introuvable() {
        when(pokemonDropRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L,
                new UpdatePokemonDropCommand((short) 1, (short) 1,
                        BigDecimal.valueOf(100), null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void delete_supprime_le_drop() {
        when(pokemonDropRepository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(pokemonDropRepository).deleteById(1L);
    }

    @Test
    void delete_lance_exception_si_drop_introuvable() {
        when(pokemonDropRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
