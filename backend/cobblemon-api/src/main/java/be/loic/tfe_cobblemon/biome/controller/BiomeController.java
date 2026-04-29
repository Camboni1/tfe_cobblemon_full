package be.loic.tfe_cobblemon.biome.controller;

import be.loic.tfe_cobblemon.biome.dto.BiomeListItemResponse;
import be.loic.tfe_cobblemon.biome.dto.BiomePokemonResponse;
import be.loic.tfe_cobblemon.biome.service.BiomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biomes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")  // ← Ajout de @CrossOrigin pour autoriser les requêtes depuis le frontend
public class BiomeController {

    private final BiomeService biomeService;

    @GetMapping
    public Page<BiomeListItemResponse> listBiomes(         // ← Page au lieu de List
                                                           @RequestParam(required = false) String search,
                                                           @PageableDefault(size = 50) Pageable pageable
    ) {
        return biomeService.listBiomes(search, pageable);  // ← pageable en paramètre
    }

    @GetMapping("/pokemon")
    public BiomePokemonResponse getPokemonByBiome(
            @RequestParam String biome
    ) {
        return biomeService.getPokemonByBiome(biome);
    }
}