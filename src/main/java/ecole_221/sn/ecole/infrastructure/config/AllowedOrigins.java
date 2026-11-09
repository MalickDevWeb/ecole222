package ecole_221.sn.ecole.infrastructure.config;

import java.util.Arrays;
import java.util.List;

/**
 * Source UNIQUE des origines autorisées pour les appels navigateur (APP-SEC-01).
 * Utilisée à la fois par le CORS REST ({@link SecurityConfig#corsConfigurationSource()})
 * et par l'endpoint STOMP/SockJS ({@link WebSocketConfig}). Ne jamais dupliquer cette liste.
 *
 * <p>APP-SEC-04 : la liste se règle par la variable d'environnement {@code CORS_ALLOWED_ORIGINS}
 * (motifs séparés par des virgules). Sans elle : postes de développement + frontend de production.
 * Le joker global « * » est toujours refusé (allowCredentials=true).
 */
public final class AllowedOrigins {

    /** Valeur utilisée quand CORS_ALLOWED_ORIGINS n'est pas définie. */
    static final List<String> PAR_DEFAUT = List.of(
            "http://localhost:*", "http://127.0.0.1:*",
            "https://papa-221.vercel.app"
    );

    /** Motifs d'origine autorisés. */
    public static final List<String> PATTERNS = depuis(System.getenv("CORS_ALLOWED_ORIGINS"));

    private AllowedOrigins() {
    }

    /** Liste à partir de la valeur de la variable ; vide ou absente → {@link #PAR_DEFAUT}. */
    static List<String> depuis(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return PAR_DEFAUT;
        }
        List<String> motifs = Arrays.stream(valeur.split(","))
                .map(String::trim)
                .filter(m -> !m.isEmpty())
                .toList();
        if (motifs.stream().anyMatch(m -> m.equals("*") || m.matches("https?://\\*/?"))) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS : le joker global « * » est interdit");
        }
        return motifs.isEmpty() ? PAR_DEFAUT : motifs;
    }
}
