package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemFileCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ItemFileCollectorImpl implements ItemFileCollector {

    @Override
    public List<Path> collectItemJsonFiles(Path datasetRoot) {
        Path itemsDirectory = datasetRoot.resolve("items");

        if (!Files.exists(itemsDirectory)) {
            throw new IllegalStateException("Le dossier items est introuvable : " + itemsDirectory);
        }

        if (!Files.isDirectory(itemsDirectory)) {
            throw new IllegalStateException("Le chemin items n'est pas un dossier : " + itemsDirectory);
        }

        try (Stream<Path> stream = Files.walk(itemsDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isJsonFile)
                    .filter(path -> !shouldIgnore(itemsDirectory.relativize(path)))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier items : " + itemsDirectory, e);
        }
    }

    private boolean isJsonFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(".json");
    }

    private boolean shouldIgnore(Path relativePath) {
        String normalized = relativePath.toString().replace('\\', '/').toLowerCase(Locale.ROOT);

        return normalized.endsWith("/en_us.json")
                || normalized.endsWith("/fr_fr.json")
                || normalized.contains("/lang/")
                || normalized.contains("/loot_table/")
                || normalized.startsWith("cobblemon/seasonings/")
                || normalized.startsWith("cobblemon/fossils/");
    }
}