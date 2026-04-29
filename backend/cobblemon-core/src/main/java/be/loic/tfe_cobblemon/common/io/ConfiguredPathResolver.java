package be.loic.tfe_cobblemon.common.io;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfiguredPathResolver {

    private ConfiguredPathResolver() {
    }

    public static Path resolve(String configuredPath) {
        Path original = Path.of(configuredPath);
        Path directPath = original.toAbsolutePath().normalize();

        if (original.isAbsolute() || Files.exists(directPath)) {
            return directPath;
        }

        Path parentRelativePath = Path.of("..").resolve(original).toAbsolutePath().normalize();
        if (Files.exists(parentRelativePath)) {
            return parentRelativePath;
        }

        return directPath;
    }
}
