package ecole_221.sn.ecole.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI ecoleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🎓 Ecole 221 API")
                        .description("""
                                API REST complète de la plateforme **Ecole 221**.
                                
                                ## Modules disponibles
                                - **Auth** — Authentification (login/register)
                                - **Student** — Profil, tableau de bord, présences, sessions live
                                - **Professor** — Cours, notes, devoirs, leçons, modules, quiz
                                - **Admin** — Gestion des accès, statuts étudiants, logs
                                - **Planning** — Salles, filières, classes, créneaux horaires
                                - **Media** — Upload de fichiers (Cloudinary)
                                - **Collaboration** — Documents partagés, commentaires, sync
                                
                                > ## 🔐 Authentification Swagger
                                > Pour tester les endpoints Swagger :
                                > 1. Utilisez **POST /auth/login-token** pour obtenir votre JWT
                                > 2. Copiez le token reçu dans la réponse
                                > 3. Cliquez sur le bouton **Authorize** 🔒 en haut à droite
                                > 4. Entrez : `Bearer <votre_token>` (avec le préfixe Bearer)
                                > 5. Cliquez sur **Authorize** puis **Close**
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe Ecole 221")
                                .email("support@ecole221.sn")
                                .url("https://ecole221.sn"))
                        .license(new License()
                                .name("Propriétaire")
                                .url("https://ecole221.sn/license"))
                )
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Entrez votre token JWT obtenu via POST /auth/login-token. Format: Bearer <votre_token>")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .tags(List.of(
                        new Tag().name("Auth").description("Authentification — login, register"),
                        new Tag().name("Student").description("Espace étudiant — profil, dashboard, présences, sessions live"),
                        new Tag().name("Professor").description("Espace professeur — cours, notes, devoirs, leçons, modules, quiz"),
                        new Tag().name("Admin").description("Administration — portails, statuts étudiants, logs temps réel"),
                        new Tag().name("Planning").description("Planification — salles, filières, classes, créneaux"),
                        new Tag().name("Media").description("Gestion des médias — upload fichiers Cloudinary"),
                        new Tag().name("Collaboration").description("Collaboration — documents partagés, commentaires")
                ));
    }
}
