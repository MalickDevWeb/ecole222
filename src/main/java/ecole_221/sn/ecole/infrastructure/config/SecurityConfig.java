package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.infrastructure.security.JwtAuthenticationFilter;
import ecole_221.sn.ecole.infrastructure.security.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // active @PreAuthorize : seconde barrière derrière les règles d'URL ci-dessous
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final TokenAuthenticationFilter tokenAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, TokenAuthenticationFilter tokenAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.tokenAuthFilter = tokenAuthFilter;
    }

    /** Rôles disposant d'un espace dans l'application (les seuls admis sur les routes métier). */
    static final String[] ROLES_AVEC_ESPACE = {
        "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN", "PLATFORM_ADMIN", "PROFESSEUR", "TEACHER", "ETUDIANT", "STUDENT",
        "PARENT", "VIGIL",
        // personnel de l'école : chaque route est ensuite filtrée par la matrice RBAC (PermissionsInterceptor)
        "DIRECTEUR", "SECRETAIRE", "COMPTABLE", "SURVEILLANT", "RH"
    };

    /** Administration + personnel : l'espace administration, filtré pour le personnel par la matrice RBAC. */
    static final String[] ROLES_ESPACE_ADMIN = {
        "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN", "PLATFORM_ADMIN", "DIRECTEUR", "SECRETAIRE", "COMPTABLE", "SURVEILLANT", "RH"
    };

    static final String[] PUBLIC_PATHS = {
        "/auth/login",
        "/auth/login-token",
        "/auth/logout",
        "/auth/mot-de-passe-oublie",          // APP-FEAT-02 : réponse identique que le compte existe ou non
        "/auth/reinitialiser-mot-de-passe",   // APP-FEAT-02 : jeton à usage unique
        "/auth/regles-mot-de-passe",          // Règles d'un rôle (indicateur de force), aucune donnée de compte
        "/auth/register",
        "/register-school",
        "/public/**",
        "/v1/public/**",
        "/health",
        "/error",
        // Swagger / OpenAPI
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/webjars/**",
        // (en profil prod, Swagger UI et /v3/api-docs sont désactivés : application-prod.yaml, I12)
        // I12 (audit DevSecOps) : /h2-console/** retiré des routes ouvertes. H2 n'est qu'une dépendance de
        // test et aucune console n'est servie ; si elle était un jour activée, elle ne serait pas ouverte sans compte.
        // WebSocket
        "/ws/**",
        // Admissions : seules les routes du portail candidat sont publiques, voir PORTAIL_CANDIDAT_* (APP-SEC-12)
        // Calendrier de l'école affiché sur la page de connexion (onglet « Calendrier »)
        // (/meet/active retiré : aucun écran ne l'appelle, il répondait sans compte ni jeton valide)
        "/schedule",
        // APP-SEC-15 : /collaboration, /wiki, /notifications, /sync, /media ne sont plus publics
        // (seules exceptions, par méthode : GET /sync/check et POST /media/upload, voir PORTAIL_CANDIDAT_*)
    };

    /** Lectures publiques du portail candidat (APP-SEC-12). */
    static final String[] PORTAIL_CANDIDAT_LECTURE = {
        "/candidatures/settings",
        // campagnes publiées (liste blanche de champs, aucune donnée de candidat)
        "/candidatures/campagnes",
        "/admissions/settings", "/v1/admissions/settings",
        // Espace du candidat (jeton de suivi exigé par chaque route : SON dossier seulement)
        "/candidatures/suivi/dossier", "/candidatures/suivi/messages", "/candidatures/suivi/piece",
        // APP-SEC-15 : indicateur hors ligne appelé sans compte (OfflineSyncToast)
        "/sync/check"
    };

    /** Écritures publiques du portail candidat (APP-SEC-12). */
    static final String[] PORTAIL_CANDIDAT_ECRITURE = {
        "/candidatures/submit",
        // APP-BE-12 : suivi de SON dossier (email + mot de passe de suivi ; échecs freinés)
        "/candidatures/suivi",
        // Pièces exigées par les règles d'admission de l'école, demandées avant le dépôt (aucune donnée de candidat)
        "/candidatures/regles/evaluer",
        // Espace du candidat — écrire à l'école, ajouter une pièce (jeton de suivi exigé)
        "/candidatures/suivi/messages", "/candidatures/suivi/pieces",
        // APP-SEC-15 : pièces du dossier de candidature envoyées sans compte (cloudinaryUpload.ts)
        "/media/upload"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                // APP-SEC-12 : portail candidat (sans compte) limité aux routes qu'il appelle réellement
                .requestMatchers(HttpMethod.GET, PORTAIL_CANDIDAT_LECTURE).permitAll()
                .requestMatchers(HttpMethod.POST, PORTAIL_CANDIDAT_ECRITURE).permitAll()
                // le reste des admissions (liste, CRUD, statuts, réglages) : administration de l'école
                .requestMatchers("/candidatures/**", "/admissions/**", "/v1/admissions/**")
                    .hasAnyRole(ROLES_ESPACE_ADMIN)
                // Routes exclusivement réservées au SUPER_ADMIN / PLATFORM_ADMIN
                .requestMatchers("/super-admin/**", "/platform/**", "/v1/super-admin/**", "/v1/platform/**",
                        "/api/v1/super-admin/**", "/api/v1/platform/**")  // contrôleurs mappés sous /api/v1 (défense en profondeur)
                    .hasAnyRole("SUPER_ADMIN", "PLATFORM_ADMIN")
                // Envoi d'e-mails libres (destinataire, sujet, contenu au choix) : réservé à l'administration.
                .requestMatchers("/email/**", "/v1/email/**").hasAnyRole(ROLES_ESPACE_ADMIN)
                // Centre de notifications de l'école (liste complète, envoi à n'importe quel destinataire, boîte d'un
                // autre compte) : administration uniquement — seul l'espace administration l'utilise.
                // Annonces : chaque compte, quel que soit son rôle, lit SES notifications et les marque lues
                // (le service ne sert que celles du compte connecté, dans l'école courante). Placé AVANT la règle
                // d'administration ci-dessous, qui garde tout le reste du centre de notifications.
                .requestMatchers(HttpMethod.GET, "/v1/notifications/mes", "/v1/notifications/mes/non-lues",
                        "/api/v1/notifications/mes", "/api/v1/notifications/mes/non-lues").hasAnyRole(ROLES_AVEC_ESPACE)
                .requestMatchers(HttpMethod.PATCH, "/v1/notifications/*/lue", "/api/v1/notifications/*/lue").hasAnyRole(ROLES_AVEC_ESPACE)
                .requestMatchers("/v1/notifications/**", "/api/v1/notifications/**", "/v1/notifications", "/api/v1/notifications")
                    .hasAnyRole(ROLES_ESPACE_ADMIN)
                .requestMatchers("/admin/**", "/finance/**", "/dashboard/**", "/audit/**")
                    .hasAnyRole(ROLES_ESPACE_ADMIN)
                // Planning : lecture pour tout compte connecté ; écriture (salles, classes, filières, créneaux) réservée à
                // l'administration.
                .requestMatchers(HttpMethod.GET, "/planning/**").hasAnyRole(ROLES_AVEC_ESPACE)
                .requestMatchers("/planning/**").hasAnyRole(ROLES_ESPACE_ADMIN)
                .requestMatchers("/vigil/check-ins").hasAnyRole("ADMIN", "ADMINISTRATOR", "VIGIL", "PROFESSEUR", "TEACHER", "SUPER_ADMIN")
                .requestMatchers("/security/**", "/vigil/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "VIGIL", "SUPER_ADMIN")
                // Validation d'un badge QR réservée aux vigiles et à l'administration (évite un
                // oracle permettant de tester des jetons sans compte)
                .requestMatchers("/student/qr-validate").hasAnyRole("ADMIN", "ADMINISTRATOR", "VIGIL", "SUPER_ADMIN")
                .requestMatchers("/professor/**", "/prof/**").hasAnyRole("PROFESSEUR", "TEACHER", "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN")
                // APP-SEC-11 : espace parent réservé aux parents (le contrôleur vérifie aussi la filiation)
                .requestMatchers("/parent/**").hasRole("PARENT")
                .requestMatchers("/student/**").hasAnyRole("ETUDIANT", "STUDENT", "PROFESSEUR", "TEACHER", "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN")
                // Pointage d'arrivée / départ par le QR d'entrée du vigile — élèves et professeurs seulement
                .requestMatchers("/pointages", "/pointages/**").hasAnyRole("ETUDIANT", "STUDENT", "PROFESSEUR", "TEACHER")
                // FEAT-007 : groupes WhatsApp à rejoindre, pour les élèves et les parents
                .requestMatchers("/groupes-whatsapp/**").hasAnyRole("ETUDIANT", "STUDENT", "PARENT")
                // APP-SEC-20 (refus par défaut) : un rôle sans espace (SECRETAIRE, COMPTABLE, VISITEUR,
                // SURVEILLANT, DIRECTEUR, RH) n'atteint plus aucune route métier ; seul son compte (/auth/**) reste
                // accessible.
                .requestMatchers("/auth/**").authenticated()
                .anyRequest().hasAnyRole(ROLES_AVEC_ESPACE)
            )
            .exceptionHandling(exception -> exception
                // réponses JSON en français
                .authenticationEntryPoint((request, response, authException) ->
                        reponseJson(response, 401, "AUTH_INVALID_CREDENTIALS", "Veuillez vous connecter pour accéder à cette page."))
                .accessDeniedHandler((request, response, deniedException) ->
                        reponseJson(response, 403, "AUTH_ACCESS_DENIED", "Votre rôle ne permet pas d'accéder à cette fonctionnalité."))
            )
            // Filtre Cookie HTTP-only pour l'application web
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // Filtre Bearer Token pour Swagger/OpenAPI
            .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        return http.build();
    }

    private static void reponseJson(jakarta.servlet.http.HttpServletResponse response, int statut, String code, String message)
            throws java.io.IOException {
        response.setStatus(statut);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"status\":" + statut + ",\"code\":\"" + code
                + "\",\"message\":\"" + message + "\",\"error\":\"" + message + "\"}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Domaines des écoles : AUCUNE origine dynamique. L'API est servie en MÊME ORIGINE sur chaque hôte
        // d'école (relais /api, comme frontend/nginx.conf) : ni sous-domaine ni domaine propre n'a besoin du CORS.
        CorsConfiguration config = new CorsConfiguration();
        // Origines explicites requises quand allowCredentials=true
        // Source unique partagée avec le WebSocket (APP-SEC-01) : voir AllowedOrigins
        config.setAllowedOriginPatterns(AllowedOrigins.PATTERNS);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // requis pour que les cookies soient envoyés
        config.setExposedHeaders(List.of("Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Coût BCrypt exigé par BR-03 (décision APP-TL-18 : sécurité maximale).
     * Les empreintes existantes (coût 10) restent vérifiables et sont recalculées au coût 12 à la connexion suivante
     * (LoginService).
     */
    public static final int COUT_BCRYPT = 12;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(COUT_BCRYPT);
    }
}
