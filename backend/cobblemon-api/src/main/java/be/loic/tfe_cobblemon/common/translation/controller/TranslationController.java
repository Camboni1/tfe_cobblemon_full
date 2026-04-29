package be.loic.tfe_cobblemon.common.translation.controller;

import be.loic.tfe_cobblemon.common.translation.dto.TranslationResponse;
import be.loic.tfe_cobblemon.common.translation.dto.TranslationUpdateRequest;
import be.loic.tfe_cobblemon.common.translation.entity.Translation;
import be.loic.tfe_cobblemon.common.translation.repository.TranslationRepository;
import be.loic.tfe_cobblemon.common.translation.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/translations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class TranslationController {

    private final TranslationRepository translationRepository;
    private final TranslationService translationService;

    @GetMapping
    public List<TranslationResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String locale
    ) {
        String pattern = (search == null || search.isBlank()) ? "%" : "%" + search.trim() + "%";
        String loc = (locale == null || locale.isBlank()) ? null : locale.trim();

        return translationRepository.findAll().stream()
                .filter(t -> loc == null || t.getLocale().equals(loc))
                .filter(t -> t.getKey().contains(search != null ? search.trim() : "")
                        || t.getValue().contains(search != null ? search.trim() : ""))
                .map(t -> new TranslationResponse(t.getKey(), t.getLocale(), t.getValue()))
                .toList();
    }

    @PutMapping("/{key}")
    public TranslationResponse update(
            @PathVariable String key,
            @RequestBody @Validated TranslationUpdateRequest request
    ) {
        translationRepository.upsert(key, request.locale(), request.value());
        translationService.reload();
        return new TranslationResponse(key, request.locale(), request.value());
    }
}