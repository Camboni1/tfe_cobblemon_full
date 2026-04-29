package be.loic.tfe_cobblemon.pokemon.controller;

import be.loic.tfe_cobblemon.common.config.SecurityConfig;
import be.loic.tfe_cobblemon.common.exception.GlobalExceptionHandler;
import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonDetailsResponse;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonListItemResponse;
import be.loic.tfe_cobblemon.pokemon.service.PokemonFormService;
import be.loic.tfe_cobblemon.pokemon.service.PokemonService;
import be.loic.tfe_cobblemon.spawn.dto.SpawnRuleResponse;
import be.loic.tfe_cobblemon.spawn.service.SpawnRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PokemonController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})  // ← ajout
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class PokemonControllerTest
 {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PokemonService pokemonService;

    @MockitoBean
    SpawnRuleService spawnRuleService;

    @MockitoBean
    PokemonFormService pokemonFormService;

    // -----------------------------------------------------------------------
    // GET /api/v1/pokemon
    // -----------------------------------------------------------------------

    @Test
    void search_retourne_une_page_de_pokemon() throws Exception {
        PokemonListItemResponse bulbasaur = new PokemonListItemResponse(
                1L, "bulbasaur", "Bulbizarre", (short) 1, "gen1", true, null
        );

        PageImpl<PokemonListItemResponse> page =
                new PageImpl<>(List.of(bulbasaur), PageRequest.of(0, 20), 1);

        when(pokemonService.search(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/pokemon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug", is("bulbasaur")))
                .andExpect(jsonPath("$.content[0].displayName", is("Bulbizarre")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));  // ← adapté
    }

     @Test
     void search_retourne_page_vide_si_aucun_resultat() throws Exception {
         PageImpl<PokemonListItemResponse> page =
                 new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

         when(pokemonService.search(any(), any())).thenReturn(page);

         mockMvc.perform(get("/api/v1/pokemon").param("search", "xxxxxxinexistant"))
                 .andExpect(status().isOk())              // ← était isNotFound(), doit être isOk()
                 .andExpect(jsonPath("$.content", hasSize(0)))
                 .andExpect(jsonPath("$.page.totalElements", is(0)));
     }

    // -----------------------------------------------------------------------
    // GET /api/v1/pokemon/{slug}
    // -----------------------------------------------------------------------

    @Test
    void getBySlug_retourne_les_details_du_pokemon() throws Exception {
        PokemonDetailsResponse details = new PokemonDetailsResponse(
                1L, "bulbasaur", "Bulbizarre", (short) 1, "gen1", true,
                null,      // spriteUrl
                null,      // iconUrl
                List.of()  // forms
        );

        when(pokemonService.getBySlug("bulbasaur")).thenReturn(details);

        mockMvc.perform(get("/api/v1/pokemon/bulbasaur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", is("bulbasaur")))
                .andExpect(jsonPath("$.displayName", is("Bulbizarre")));
    }

    @Test
    void getBySlug_retourne_404_si_slug_inconnu() throws Exception {
        when(pokemonService.getBySlug("inexistant"))
                .thenThrow(new ResourceNotFoundException("Pokémon introuvable"));

        mockMvc.perform(get("/api/v1/pokemon/inexistant"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/pokemon/{slug}/spawns
    // -----------------------------------------------------------------------

     // Dans PokemonControllerTest.java
// Remplace le test getSpawns_retourne_la_liste_des_regles_de_spawn

     @Test
     void getSpawns_retourne_la_liste_des_regles_de_spawn() throws Exception {
         SpawnRuleResponse spawn = new SpawnRuleResponse(
                 10L,
                 "ext-001",
                 "bulbasaur",
                 "Bulbizarre",        // ← pokemonDisplayName ajouté
                 null,                // formCode
                 null,                // targetExpression
                 "Pokémon",           // spawnType (String)
                 "Sol",               // spawnablePositionType (String)
                 "Ultra Rare",        // bucket (String)
                 (short) 5,
                 (short) 15,
                 BigDecimal.valueOf(10.0),
                 (short) 4,
                 "spawns/bulbasaur.json",
                 null                 // condition
         );

         when(spawnRuleService.findByPokemonSlug("bulbasaur")).thenReturn(List.of(spawn));

         mockMvc.perform(get("/api/v1/pokemon/bulbasaur/spawns"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", hasSize(1)))
                 .andExpect(jsonPath("$[0].pokemonSlug", is("bulbasaur")))
                 .andExpect(jsonPath("$[0].bucket", is("Ultra Rare")))
                 .andExpect(jsonPath("$[0].levelMin", is(5)));
     }

     @Test
     void getSpawns_retourne_liste_vide_si_aucun_spawn() throws Exception {
         when(spawnRuleService.findByPokemonSlug("mewtwo")).thenReturn(List.of());

         mockMvc.perform(get("/api/v1/pokemon/mewtwo/spawns"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", hasSize(0)));
     }
}
