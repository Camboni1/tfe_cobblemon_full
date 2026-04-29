// =========================================================================
// SpawnControllerTest.java
// =========================================================================
package be.loic.tfe_cobblemon.spawn.controller;

import be.loic.tfe_cobblemon.common.config.SecurityConfig;
import be.loic.tfe_cobblemon.common.exception.GlobalExceptionHandler;
import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.spawn.dto.SpawnRuleResponse;
import be.loic.tfe_cobblemon.spawn.enums.SpawnBucket;
import be.loic.tfe_cobblemon.spawn.enums.SpawnType;
import be.loic.tfe_cobblemon.spawn.enums.SpawnablePositionType;
import be.loic.tfe_cobblemon.spawn.service.SpawnRuleService;
import be.loic.tfe_cobblemon.spawn.service.command.CreateSpawnRuleCommand;
import be.loic.tfe_cobblemon.spawn.service.command.UpdateSpawnRuleCommand;
import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpawnController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class SpawnControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean SpawnRuleService spawnRuleService;

    // SpawnRuleResponse actuel :
    // id, externalId, pokemonSlug, pokemonDisplayName, formCode,
    // targetExpression, spawnType(String), spawnablePositionType(String),
    // bucket(String), levelMin, levelMax, weight, maxHerdSize,
    // sourceFilename, condition
    // SpawnRuleResponse — 15 champs (pokemonDisplayName + condition)
    private SpawnRuleResponse buildResponse(Long id) {
        return new SpawnRuleResponse(
                id,
                "ext-001",
                "bulbasaur",
                "Bulbizarre",   // pokemonDisplayName
                null,           // formCode
                "land",         // targetExpression
                "Pokémon",      // spawnType (String)
                "Sol",          // spawnablePositionType (String)
                "Commun",       // bucket (String)
                (short) 5,
                (short) 15,
                BigDecimal.valueOf(10.0),
                null,           // maxHerdSize
                "spawns/bulbasaur.json",
                null            // condition
        );
    }

    // CreateSpawnRuleCommand :
    // spawnSourceFileId, externalId, pokemonId, pokemonFormId,
    // targetExpression, spawnType, spawnablePositionType, bucket,
    // levelMin, levelMax, weight, maxHerdSize, minDistanceBetweenSpawns,
    // weightMultiplierJson, weightMultipliersJson, herdablePokemonJson,
    // rawJson, presetIds, condition
    private CreateSpawnRuleCommand buildCreateCommand() {
        return new CreateSpawnRuleCommand(
                1L, "ext-001", 1L, null, "land",
                SpawnType.POKEMON, SpawnablePositionType.GROUNDED,
                SpawnBucket.COMMON, (short) 5, (short) 15,
                BigDecimal.TEN, null, null, null, null, null,
                "{}", Set.of(), null
        );
    }

    private UpdateSpawnRuleCommand buildUpdateCommand() {
        return new UpdateSpawnRuleCommand(
                1L, "ext-001", 1L, null, "land",
                SpawnType.POKEMON, SpawnablePositionType.GROUNDED,
                SpawnBucket.COMMON, (short) 5, (short) 15,
                BigDecimal.TEN, null, null, null, null, null,
                "{}", Set.of(), null
        );
    }

    @Test
    void create_retourne_201_avec_la_regle_creee() throws Exception {
        SpawnRule fakeRule = new SpawnRule();
        fakeRule.setId(42L);

        when(spawnRuleService.create(any())).thenReturn(fakeRule);
        when(spawnRuleService.getResponseById(42L)).thenReturn(buildResponse(42L));

        mockMvc.perform(post("/api/v1/spawns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateCommand())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(42)))
                .andExpect(jsonPath("$.pokemonSlug", is("bulbasaur")))
                .andExpect(jsonPath("$.pokemonDisplayName", is("Bulbizarre")));
    }

    @Test
    void update_retourne_200_avec_la_regle_modifiee() throws Exception {
        SpawnRule fakeRule = new SpawnRule();
        fakeRule.setId(1L);

        when(spawnRuleService.update(eq(1L), any())).thenReturn(fakeRule);
        when(spawnRuleService.getResponseById(1L)).thenReturn(buildResponse(1L));

        mockMvc.perform(put("/api/v1/spawns/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateCommand())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bucket", is("Commun")));
    }

    @Test
    void update_retourne_404_si_regle_inexistante() throws Exception {
        when(spawnRuleService.update(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("SpawnRule introuvable : 999"));

        mockMvc.perform(put("/api/v1/spawns/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateCommand())))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_retourne_204_si_succes() throws Exception {
        doNothing().when(spawnRuleService).delete(1L);

        mockMvc.perform(delete("/api/v1/spawns/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_retourne_404_si_regle_inexistante() throws Exception {
        doThrow(new ResourceNotFoundException("SpawnRule introuvable : 999"))
                .when(spawnRuleService).delete(999L);

        mockMvc.perform(delete("/api/v1/spawns/999"))
                .andExpect(status().isNotFound());
    }
}