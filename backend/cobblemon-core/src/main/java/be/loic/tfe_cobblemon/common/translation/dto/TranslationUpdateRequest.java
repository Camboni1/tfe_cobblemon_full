package be.loic.tfe_cobblemon.common.translation.dto;

import jakarta.validation.constraints.NotBlank;

public record TranslationUpdateRequest(
        @NotBlank String locale,
        @NotBlank String value
) {}