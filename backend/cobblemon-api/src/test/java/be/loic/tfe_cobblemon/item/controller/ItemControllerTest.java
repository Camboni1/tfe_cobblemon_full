// =========================================================================
// ItemControllerTest.java
// =========================================================================
package be.loic.tfe_cobblemon.item.controller;

import be.loic.tfe_cobblemon.common.config.SecurityConfig;
import be.loic.tfe_cobblemon.common.exception.GlobalExceptionHandler;
import be.loic.tfe_cobblemon.item.dto.ItemResponse;
import be.loic.tfe_cobblemon.item.service.ItemService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class ItemControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ItemService itemService;

    // ItemResponse : id, namespacedId, namespace, path, displayName, isPlaceholder
    private ItemResponse buildItem(Long id, String namespacedId, String displayName) {
        String[] parts = namespacedId.split(":");
        return new ItemResponse(id, namespacedId, parts[0], parts[1], displayName, false);
    }

    @Test
    void search_sans_filtre_retourne_une_page_ditems() throws Exception {
        var vine = buildItem(1L, "minecraft:vine", "Vigne");
        var page = new PageImpl<>(List.of(vine), PageRequest.of(0, 20), 1);

        when(itemService.search(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].namespacedId", is("minecraft:vine")))
                .andExpect(jsonPath("$.content[0].displayName", is("Vigne")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void search_avec_filtre_retourne_items_correspondants() throws Exception {
        var bone = buildItem(2L, "minecraft:bone", "Os");
        var page = new PageImpl<>(List.of(bone), PageRequest.of(0, 20), 1);

        when(itemService.search(eq("bone"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/items").param("search", "bone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].namespacedId", is("minecraft:bone")));
    }

    @Test
    void search_retourne_page_vide_si_aucun_resultat() throws Exception {
        var page = new PageImpl<>(List.<ItemResponse>of(), PageRequest.of(0, 20), 0);

        when(itemService.search(eq("inexistant"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/items").param("search", "inexistant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    void search_supporte_la_pagination() throws Exception {
        var page = new PageImpl<>(List.<ItemResponse>of(), PageRequest.of(2, 10), 25);

        when(itemService.search(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/items")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size", is(10)))
                .andExpect(jsonPath("$.page.number", is(2)));
    }
}