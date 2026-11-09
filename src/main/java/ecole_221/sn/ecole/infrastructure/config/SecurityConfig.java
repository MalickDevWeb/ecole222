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

    static final String[] PUBLIC_PATHS = {
        "/auth/login",
        "/auth/login-token",
        "/auth/logout",
        "/auth/register",
        "/register-school",
        "/public/**",
        "/v1/public/**",
        "/health",
        "/error",
        "/student/qr-validate", // Ajouté pour permettre aux vigiles de scanner les QR
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
        "/notifications/**",
        // Dashboard étudiant & planning consultation
        "/meet/active",
        "/schedule",
        // Collaboration & Wiki partagé
        "/collaboration/**",
        "/wiki/**",
        // Synchronisation offline / online
        "/sync/**",
        // Upload multimédia Cloudinary public pour admissions & documents
        "/media/**"
    };

    /** Lectures publiques du portail candidat (APP-SEC-12). */
    static final String[] PORTAIL_CANDIDAT_LECTURE = {
        "/candidatures/settings",
        "/admissions/settings", "/v1/admissions/settings",
        // TEMPORAIRE (carte APP-TL-10) : le portail télécharge toute la base des admissions
        "/admissions/db", "/v1/admissions/db"
    };

    /** Écritures publiques du portail candidat (APP-SEC-12). */
    static final String[] PORTAIL_CANDIDAT_ECRITURE = {
        "/candidatures/submit",
        // TEMPORAIRE (carte APP-TL-10)
        "/admissions/db", "/v1/admissions/db"
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
                    .hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPER_ADMIN", "PLATFORM_ADMIN")
                // Routes exclusivement réservées au SUPER_ADMIN / PLATFORM_ADMIN
                .requestMatchers("/super-admin/**", "/platform/**", "/v1/super-admin/**", "/v1/platform/**")
                    .hasAnyRole("SUPER_ADMIN", "PLATFORM_ADMIN")
                .requestMatchers("/admin/**", "/finance/**", "/dashboard/**", "/audit/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPER_ADMIN", "PLATFORM_ADMIN")
                .requestMatchers("/planning/**").authenticated()
                .requestMatchers("/vigil/check-ins").hasAnyRole("ADMIN", "ADMINISTRATOR", "VIGIL", "PROFESSEUR", "TEACHER", "SUPER_ADMIN")
                .requestMatchers("/security/**", "/vigil/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "VIGIL", "SUPER_ADMIN")
                .requestMatchers("/professor/**", "/prof/**").hasAnyRole("PROFESSEUR", "TEACHER", "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN")
                .requestMatchers("/student/**").hasAnyRole("ETUDIANT", "STUDENT", "PROFESSEUR", "TEACHER", "ADMIN", "ADMINISTRATOR", "SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
            )
            // Filtre Cookie HTTP-only pour l'application web
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // Filtre Bearer Token pour Swagger/OpenAPI
            .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        return http.build();
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
