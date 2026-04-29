// =========================================================================
// BiomeControllerTest.java
// =========================================================================
package be.loic.tfe_cobblemon.biome.controller;

import be.loic.tfe_cobblemon.biome.dto.BiomeListItemResponse;
import be.loic.tfe_cobblemon.biome.dto.BiomePokemonResponse;
import be.loic.tfe_cobblemon.biome.service.BiomeService;
import be.loic.tfe_cobblemon.common.config.SecurityConfig;
import be.loic.tfe_cobblemon.common.exception.GlobalExceptionHandler;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonListItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BiomeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class BiomeControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean BiomeService biomeService;

    // BiomeListItemResponse : value, isTag
    // PokemonListItemResponse : id, slug, displayName, nationalDexNumber, generationCode, implemented

    @Test
    void listBiomes_retourne_une_page_de_biomes() throws Exception {
        var freshwater = new BiomeListItemResponse("#cobblemon:is_freshwater", true);
        var jungle = new BiomeListItemResponse("#cobblemon:is_jungle", true);
        var page = new PageImpl<>(List.of(freshwater, jungle), PageRequest.of(0, 50), 2);

        when(biomeService.listBiomes(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/biomes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].value", is("#cobblemon:is_freshwater")))
                .andExpect(jsonPath("$.content[0].isTag", is(true)))
                .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    void listBiomes_avec_search_filtre_les_resultats() throws Exception {
        var jungle = new BiomeListItemResponse("#cobblemon:is_jungle", true);
        var page = new PageImpl<>(List.of(jungle), PageRequest.of(0, 50), 1);

        when(biomeService.listBiomes(eq("jungle"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/biomes").param("search", "jungle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].value", containsString("jungle")));
    }

    @Test
    void listBiomes_retourne_page_vide_si_aucun_resultat() throws Exception {
        var page = new PageImpl<>(List.<BiomeListItemResponse>of(), PageRequest.of(0, 50), 0);

        when(biomeService.listBiomes(eq("inexistant"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/biomes").param("search", "inexistant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    void listBiomes_supporte_la_pagination() throws Exception {
        var page = new PageImpl<>(List.<BiomeListItemResponse>of(), PageRequest.of(1, 10), 100);

        when(biomeService.listBiomes(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/biomes")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size", is(10)))
                .andExpect(jsonPath("$.page.number", is(1)));
    }

    @Test
    void getPokemonByBiome_retourne_les_pokemon_du_biome() throws Exception {
        var squirtle = new PokemonListItemResponse(
                1L, "squirtle", "Carapuce", (short) 7, "GEN_1", true, null
        );
        var response = new BiomePokemonResponse("#cobblemon:is_freshwater", List.of(squirtle));

        when(biomeService.getPokemonByBiome("#cobblemon:is_freshwater")).thenReturn(response);

        mockMvc.perform(get("/api/v1/biomes/pokemon")
                        .param("biome", "#cobblemon:is_freshwater"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biomeValue", is("#cobblemon:is_freshwater")))
                .andExpect(jsonPath("$.pokemon", hasSize(1)))
                .andExpect(jsonPath("$.pokemon[0].slug", is("squirtle")));
    }

    @Test
    void getPokemonByBiome_retourne_liste_vide_si_biome_inconnu() throws Exception {
        var response = new BiomePokemonResponse("inexistant", List.of());

        when(biomeService.getPokemonByBiome("inexistant")).thenReturn(response);

        mockMvc.perform(get("/api/v1/biomes/pokemon").param("biome", "inexistant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pokemon", hasSize(0)));
    }
}