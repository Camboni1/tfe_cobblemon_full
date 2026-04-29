package be.loic.tfe_cobblemon;

import be.loic.tfe_cobblemon.common.config.AppProperties;
import be.loic.tfe_cobblemon.config.DatasetImportProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DatasetImportProperties.class, AppProperties.class})
public class CobblemonImporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(CobblemonImporterApplication.class, args);
    }
}
