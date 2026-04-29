package be.loic.tfe_cobblemon.common.config;

import be.loic.tfe_cobblemon.common.io.ConfiguredPathResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.assets.path}")
    private String assetsPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path resolved = ConfiguredPathResolver.resolve(assetsPath);

        registry.addResourceHandler("/assets/**")
                .addResourceLocations("file:" + resolved + "/");
    }
}
