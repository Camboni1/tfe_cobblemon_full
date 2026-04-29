package be.loic.tfe_cobblemon.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // Config pour les routes API (avec credentials, méthodes d'écriture)
        CorsConfiguration apiConfig = new CorsConfiguration();
        apiConfig.setAllowedOrigins(List.of("http://localhost:3000"));
        apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        apiConfig.setAllowedHeaders(List.of("*"));
        apiConfig.setAllowCredentials(true);

        // Config pour les assets statiques (lecture seule, pas de credentials,
        // utilisable depuis fetch() / Three.js TextureLoader avec crossOrigin)
        CorsConfiguration assetsConfig = new CorsConfiguration();
        assetsConfig.setAllowedOrigins(List.of("http://localhost:3000"));
        assetsConfig.setAllowedMethods(List.of("GET", "HEAD", "OPTIONS"));
        assetsConfig.setAllowedHeaders(List.of("*"));
        assetsConfig.setAllowCredentials(false);
        assetsConfig.setMaxAge(3600L); // mise en cache du preflight

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", apiConfig);
        source.registerCorsConfiguration("/assets/**", assetsConfig);

        return new CorsFilter(source);
    }
}