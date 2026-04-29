package be.loic.tfe_cobblemon;

import be.loic.tfe_cobblemon.common.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class CobblemonApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CobblemonApiApplication.class, args);
    }
}
