package be.loic.tfe_cobblemon.common.translation.dto;

public record TranslationResponse(
        String key,
        String locale,
        String value
) {}