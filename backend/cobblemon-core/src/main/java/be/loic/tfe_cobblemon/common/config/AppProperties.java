package be.loic.tfe_cobblemon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Assets assets) {

    public record Assets(String path, String baseUrl) {}
}