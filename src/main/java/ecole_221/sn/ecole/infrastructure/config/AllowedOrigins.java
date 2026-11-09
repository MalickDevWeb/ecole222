package ecole_221.sn.ecole.infrastructure.config;

import java.util.List;

/**
 * Source UNIQUE des origines autorisées pour les appels navigateur (APP-SEC-01).
 * Utilisée à la fois par le CORS REST ({@link SecurityConfig#corsConfigurationSource()})
 * et par l'endpoint STOMP/SockJS ({@link WebSocketConfig}). Ne jamais dupliquer cette liste.
 */
public final class AllowedOrigins {

    /** Motifs d'origine autorisés (pas de joker global "*" : allowCredentials=true). */
    public static final List<String> PATTERNS = List.of(
            "http://localhost:*", "http://127.0.0.1:*",
            "http://127.0.0.1:5173",
            "http://192.168.1.239:5173",
            "https://papa-221.vercel.app"
    );

    private AllowedOrigins() {
    }
}
