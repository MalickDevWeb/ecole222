package ecole_221.sn.ecole.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * I12 et B6 (audits DevSecOps du 15/11) : Swagger UI et la spécification OpenAPI sont fermés par défaut et en prod,
 * ouverts seulement en QA (SPRINGDOC_ENABLED=true) et en dev ; la console H2 n'est plus une route ouverte sans compte.
 */
class ExpositionDocumentationApiTest {

    private static List<PropertySource<?>> charger(String fichier) throws Exception {
        return new YamlPropertySourceLoader().load(fichier, new ClassPathResource(fichier));
    }

    /**
     * Environnement tel que Spring le compose : variables simulées, puis fichier du profil, puis application.yaml
     * (profil null = profil par défaut). Les variables et propriétés système réelles sont retirées : le résultat ne
     * dépend pas du shell qui lance les tests.
     */
    private static StandardEnvironment environnement(String profil, Map<String, Object> variables) throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.addLast(new MapPropertySource("variables-simulees", variables));
        if (profil != null) {
            charger("application-" + profil + ".yaml").forEach(sources::addLast);
        }
        charger("application.yaml").forEach(sources::addLast);
        return env;
    }

    private static void assertSpringdoc(String attendu, StandardEnvironment env, String cas) {
        assertEquals(attendu, env.getProperty("springdoc.api-docs.enabled"), cas + " : api-docs");
        assertEquals(attendu, env.getProperty("springdoc.swagger-ui.enabled"), cas + " : swagger-ui");
    }

    /**
     * B6 : backend/render.yaml et le Dockerfile ne posent pas SPRING_PROFILES_ACTIVE ; la production tourne donc en
     * profil par défaut. Swagger UI et la spécification y sont fermés.
     */
    @Test
    void swaggerEtApiDocsFermesParDefaut() throws Exception {
        assertSpringdoc("false", environnement(null, Map.of()), "profil par défaut");
    }

    @Test
    void swaggerEtApiDocsFermesEnProdMemeAvecLaVariable() throws Exception {
        assertSpringdoc("false", environnement("prod", Map.of()), "prod");
        assertSpringdoc("false", environnement("prod", Map.of("SPRINGDOC_ENABLED", "true")), "prod + SPRINGDOC_ENABLED=true");
    }

    /** generer_contrat_routes.py lit /api/v3/api-docs des backends de QA (qa/start_backend.sh pose SPRINGDOC_ENABLED=true). */
    @Test
    void specificationServieEnQaEtEnDev() throws Exception {
        StandardEnvironment qa = environnement(null, Map.of("SPRINGDOC_ENABLED", "true"));
        assertSpringdoc("true", qa, "QA");
        assertEquals("/v3/api-docs", qa.getProperty("springdoc.api-docs.path"));
        assertSpringdoc("true", environnement("dev", Map.of()), "dev");
    }

    @Test
    void scriptDeQaOuvreLaSpecification() throws Exception {
        java.nio.file.Path script = java.nio.file.Path.of("../qa/start_backend.sh");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.exists(script),
                "qa/start_backend.sh absent (copie du seul backend) : vérification sautée");
        String contenu = java.nio.file.Files.readString(script);
        assertTrue(contenu.lines().anyMatch(l -> l.strip().equals("export SPRINGDOC_ENABLED=true")));
    }

    @Test
    void consoleH2NonOuverteSansCompte() {
        PathPatternParser parser = new PathPatternParser();
        List<String> ouvertes = Stream.of(SecurityConfig.PUBLIC_PATHS, SecurityConfig.PORTAIL_CANDIDAT_LECTURE,
                SecurityConfig.PORTAIL_CANDIDAT_ECRITURE).flatMap(Arrays::stream).toList();
        for (String chemin : List.of("/h2-console", "/h2-console/", "/h2-console/login.do")) {
            for (String motif : ouvertes) {
                assertFalse(parser.parse(motif).matches(PathContainer.parsePath(chemin)),
                        chemin + " ne doit pas être ouvert sans compte (motif " + motif + ")");
            }
        }
    }
}
