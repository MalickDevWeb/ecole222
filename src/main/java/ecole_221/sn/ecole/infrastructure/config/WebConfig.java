package ecole_221.sn.ecole.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Laisser Spring Security gérer le CORS via SecurityConfig.corsConfigurationSource()
        // pour éviter tout conflit d'en-têtes et garantir allowCredentials=true de manière sécurisée.
    }
}