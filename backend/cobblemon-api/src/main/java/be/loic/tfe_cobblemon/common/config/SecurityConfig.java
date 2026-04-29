package be.loic.tfe_cobblemon.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/biomes/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/v1/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.GET,    "/api/v1/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/v1/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/v1/spawns/**").permitAll()
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/spawns/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/spawns/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/v1/drops/**").permitAll()
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/drops/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/drops/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/items/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/translations/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/translations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/assets/**").permitAll()
                        .anyRequest().authenticated()
                )
//                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}