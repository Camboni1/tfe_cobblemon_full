package be.loic.tfe_cobblemon.dataset.importer.translation.service.impl;

import be.loic.tfe_cobblemon.common.translation.repository.TranslationRepository;
import be.loic.tfe_cobblemon.common.translation.service.TranslationService;
import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.translation.service.TranslationImportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TranslationImportServiceImpl implements TranslationImportService {

    private static final Logger log = LoggerFactory.getLogger(TranslationImportServiceImpl.class);

    private static final Map<String, String> LANG_FILES = Map.of(
            "en", "translations/en_us.json",  // ← était lang/en_us.json
            "fr", "translations/fr_fr.json"   // ← était lang/fr_fr.json
    );

    private final TranslationRepository translationRepository;
    private final TranslationService translationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importTranslations(DatasetVersion datasetVersion, Path datasetRoot) {
        int total = 0;

        for (Map.Entry<String, String> entry : LANG_FILES.entrySet()) {
            String locale = entry.getKey();
            Path filePath = datasetRoot.resolve(entry.getValue());

            if (!Files.exists(filePath)) {
                log.warn("Fichier de traduction introuvable : {}", filePath);
                continue;
            }

            try {
                String raw = Files.readString(filePath, StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(raw);

                int count = 0;
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    JsonNode valueNode = field.getValue();
                    if (!valueNode.isTextual()) continue;
                    String value = valueNode.asText();

                    if (key.isBlank() || value.isBlank()) continue;

                    translationRepository.upsert(key, locale, value);
                    count++;
                }

                total += count;
                log.info("Traductions upsertées depuis {} : {} entrées", filePath.getFileName(), count);

            } catch (IOException e) {
                throw new IllegalStateException("Erreur lecture traduction : " + filePath, e);
            }
        }

        translationService.reload();
        log.info("Import des traductions terminé : {} entrées au total", total);
    }
}