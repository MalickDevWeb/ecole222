package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.infrastructure.security.JwtAuthenticationFilter;
import ecole_221.sn.ecole.infrastructure.security.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final TokenAuthenticationFilter tokenAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, TokenAuthenticationFilter tokenAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.tokenAuthFilter = tokenAuthFilter;
    }

    private static final String[] PUBLIC_PATHS = {
        "/auth/login",
        "/auth/login-token",
        "/auth/logout",
        "/auth/register",
        "/register-school",
        "/public/**",
        "/v1/public/**",
        "/candidatures/settings",
        "/candidatures/submit",
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
        // Nouvelles APIs publiques pour développement
        "/finance/**",
        "/dashboard/**",
        "/admissions/**",
        "/v1/admissions/**",
        "/notifications/**",
        "/audit/**",
        // Dashboard étudiant — polling routes
        "/meet/active",
        "/schedule",
        // Finance endpoints pour tests
        "/finance/**",
        // Pointages & Émargements personnel
        "/admin/attendance/**",
        "/admin/attendance",
        // Gestion du personnel & comptes
        "/admin/staff/**",
        "/admin/staff",
        // Planning, Salles, Filières, Classes
        "/planning/**",
        // Étudiants, Utilisateurs admin et statistiques
        "/admin/users",
        "/admin/students/**",
        "/admin/students",
        "/admin/stats",
        // Collaboration & Wiki partagé
        "/collaboration/**",
        "/wiki/**",
        // Synchronisation offline / online
        "/sync/**",
        // Upload multimédia Cloudinary public pour admissions & documents
        "/media/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                // Routes exclusivement réservées au SUPER_ADMIN / PLATFORM_ADMIN
                .requestMatchers("/super-admin/**", "/platform/**", "/v1/super-admin/**", "/v1/platform/**")
                    .hasAnyRole("SUPER_ADMIN", "PLATFORM_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPER_ADMIN", "PLATFORM_ADMIN")
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
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*", "http://127.0.0.1:*",
            "http://127.0.0.1:5173",
            "http://192.168.1.239:5173",
            "https://papa-221.vercel.app"
        ));
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
