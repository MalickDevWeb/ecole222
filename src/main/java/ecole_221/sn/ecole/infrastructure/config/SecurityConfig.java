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
        // H2 console (dev)
        "/h2-console/**",
        // WebSocket
        "/ws/**",
        // Admissions : seules les routes du portail candidat sont publiques, voir PORTAIL_CANDIDAT_* (APP-SEC-12)
        // Calendrier de l'école affiché sur la page de connexion (onglet « Calendrier »)
        // (/meet/active retiré le 2026-11-13 : aucun écran ne l'appelle, il répondait sans compte ni jeton valide)
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
        // APP-SEC-15 : indicateur hors ligne appelé sans compte (OfflineSyncToast)
        "/sync/check"
    };

    /** Écritures publiques du portail candidat (APP-SEC-12). */
    static final String[] PORTAIL_CANDIDAT_ECRITURE = {
        "/candidatures/submit",
        // APP-BE-12 : suivi de SON dossier (email + mot de passe de suivi ; échecs freinés)
        "/candidatures/suivi",
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
                // Avant : tout compte connecté, élève compris, pouvait écrire à n'importe qui au nom de l'école.
                .requestMatchers("/email/**", "/v1/email/**").hasAnyRole(ROLES_ESPACE_ADMIN)
                // Centre de notifications de l'école (liste complète, envoi à n'importe quel destinataire, boîte d'un
                // autre compte) : administration uniquement — seul l'espace administration l'utilise. Avant : un élève
                // lisait les notifications de l'administrateur et envoyait des notifications « prioritaires » à qui il voulait.
                .requestMatchers("/v1/notifications/**", "/api/v1/notifications/**", "/v1/notifications", "/api/v1/notifications")
                    .hasAnyRole(ROLES_ESPACE_ADMIN)
                .requestMatchers("/admin/**", "/finance/**", "/dashboard/**", "/audit/**")
                    .hasAnyRole(ROLES_ESPACE_ADMIN)
                // Planning : lecture pour tout compte connecté ; écriture (salles, classes, filières, créneaux) réservée à
                // l'administration. Avant (balayage du 2026-11-13) : un élève créait, modifiait ou supprimait le planning.
                .requestMatchers(HttpMethod.GET, "/planning/**").hasAnyRole(ROLES_AVEC_ESPACE)
                .requestMatchers("/planning/**").hasAnyRole(ROLES_ESPACE_ADMIN)
                .requestMatchers("/vigil/check-ins").hasAnyRole("ADMIN", "ADMINISTRATOR", "VIGIL", "PROFESSEUR", "TEACHER", "SUPER_ADMIN")
                .requestMatchers("/security/**", "/vigil/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "VIGIL", "SUPER_ADMIN")
                // 2026-11-13 : validation d'un badge QR réservée aux vigiles et à l'administration (avant : publique →
                // oracle permettant de tester des jetons sans compte)
                .requestMatchers("/student/qr-validate").hasAnyRole("ADMIN", "ADMINISTRATOR", "VIGIL", "SUPER_ADMIN")
                .requestMatchers("/professor/**", "/prof/**").hasAnyRole("PROFESSEUR", "TEACHER", "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN")
                // APP-SEC-11 : espace parent réservé aux parents (le contrôleur vérifie aussi la filiation)
                .requestMatchers("/parent/**").hasRole("PARENT")
                .requestMatchers("/student/**").hasAnyRole("ETUDIANT", "STUDENT", "PROFESSEUR", "TEACHER", "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN")
                // 16/11 : pointage d'arrivée / départ par le QR d'entrée du vigile — élèves et professeurs seulement
                .requestMatchers("/pointages", "/pointages/**").hasAnyRole("ETUDIANT", "STUDENT", "PROFESSEUR", "TEACHER")
                // APP-SEC-20 (2026-11-13, refus par défaut) : un rôle sans espace (SECRETAIRE, COMPTABLE, VISITEUR,
                // SURVEILLANT, DIRECTEUR, RH) n'atteint plus aucune route métier ; seul son compte (/auth/**) reste
                // accessible. Avant : ces rôles atteignaient toutes les routes simplement « authentifiées ».
                .requestMatchers("/auth/**").authenticated()
                .anyRequest().hasAnyRole(ROLES_AVEC_ESPACE)
            )
            .exceptionHandling(exception -> exception
                // réponses JSON en français (avant : « Unauthorized » / « Forbidden » bruts de Spring)
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
     * Coût BCrypt exigé par BR-03 (2026-11-13, décision APP-TL-18 : sécurité maximale). Avant : coût par défaut 10.
     * Les empreintes existantes (coût 10) restent vérifiables et sont recalculées au coût 12 à la connexion suivante
     * (LoginService).
     */
    public static final int COUT_BCRYPT = 12;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(COUT_BCRYPT);
    }
}
