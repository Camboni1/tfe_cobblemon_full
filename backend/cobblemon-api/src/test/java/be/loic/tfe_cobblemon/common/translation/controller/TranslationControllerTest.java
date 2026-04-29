// =========================================================================
// TranslationControllerTest.java
// =========================================================================
package be.loic.tfe_cobblemon.common.translation.controller;

import be.loic.tfe_cobblemon.common.config.SecurityConfig;
import be.loic.tfe_cobblemon.common.exception.GlobalExceptionHandler;
import be.loic.tfe_cobblemon.common.translation.dto.TranslationUpdateRequest;
import be.loic.tfe_cobblemon.common.translation.entity.Translation;
import be.loic.tfe_cobblemon.common.translation.repository.TranslationRepository;
import be.loic.tfe_cobblemon.common.translation.service.TranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TranslationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class TranslationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TranslationRepository translationRepository;
    @MockitoBean TranslationService translationService;

    private Translation buildTranslation(String key, String locale, String value) {
        Translation t = new Translation();
        t.setKey(key);
        t.setLocale(locale);
        t.setValue(value);
        return t;
    }

    @Test
    void search_retourne_toutes_les_traductions_sans_filtre() throws Exception {
        when(translationRepository.findAll()).thenReturn(List.of(
                buildTranslation("bucket.COMMON", "fr", "Commun"),
                buildTranslation("bucket.COMMON", "en", "Common")
        ));

        mockMvc.perform(get("/api/v1/translations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void search_filtre_par_locale() throws Exception {
        when(translationRepository.findAll()).thenReturn(List.of(
                buildTranslation("bucket.COMMON", "fr", "Commun"),
                buildTranslation("bucket.COMMON", "en", "Common")
        ));

        mockMvc.perform(get("/api/v1/translations").param("locale", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].locale", is("fr")))
                .andExpect(jsonPath("$[0].value", is("Commun")));
    }

    @Test
    void search_filtre_par_search() throws Exception {
        when(translationRepository.findAll()).thenReturn(List.of(
                buildTranslation("bucket.COMMON", "fr", "Commun"),
                buildTranslation("position.GROUNDED", "fr", "Sol")
        ));

        mockMvc.perform(get("/api/v1/translations").param("search", "bucket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].key", is("bucket.COMMON")));
    }

    @Test
    void update_retourne_la_traduction_modifiee() throws Exception {
        // upsert est @Modifying native query → void → doNothing
        doNothing().when(translationRepository)
                .upsert("bucket.COMMON", "fr", "Très Commun");
        doNothing().when(translationService).reload();

        String body = objectMapper.writeValueAsString(
                new TranslationUpdateRequest("fr", "Très Commun")
        );

        mockMvc.perform(put("/api/v1/translations/bucket.COMMON")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key", is("bucket.COMMON")))
                .andExpect(jsonPath("$.locale", is("fr")))
                .andExpect(jsonPath("$.value", is("Très Commun")));
    }

    @Test
    void update_retourne_400_si_body_invalide() throws Exception {
        // locale vide → @NotBlank → 400
        String body = objectMapper.writeValueAsString(
                new TranslationUpdateRequest("", "Valeur")
        );

        mockMvc.perform(put("/api/v1/translations/bucket.COMMON")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}