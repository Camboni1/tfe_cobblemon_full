package be.loic.tfe_cobblemon.runner;

import be.loic.tfe_cobblemon.config.DatasetImportProperties;
import be.loic.tfe_cobblemon.dataset.importer.service.DatasetRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatasetImportRunner implements CommandLineRunner {

    private final DatasetImportProperties properties;
    private final DatasetRefreshService datasetRefreshService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        datasetRefreshService.refresh(
                properties.getCode(),
                properties.getLabel(),
                properties.getInputPath(),
                properties.isCleanBeforeImport()
        );

        System.exit(org.springframework.boot.SpringApplication.exit(applicationContext, () -> 0));
    }
}
