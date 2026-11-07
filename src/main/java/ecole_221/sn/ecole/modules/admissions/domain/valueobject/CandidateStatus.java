package ecole_221.sn.ecole.modules.admissions.domain.valueobject;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Statut d'un candidat. Les CLÉS restent dans le code (le workflow en dépend) ;
 * libellés, couleurs et ordre d'affichage sont paramétrables dans le référentiel
 * {@code candidate-status} (table reference_values).
 *
 * <p>Toujours convertir une chaîne avec {@link #from(String)}, jamais {@code valueOf} :
 * la colonne candidates.admission_status a historiquement reçu plusieurs vocabulaires
 * (« pending », « ACCEPTE », « VALIDE »…) qui faisaient échouer toute la liste (BUG-QA-01).</p>
 */
public enum CandidateStatus {
    EN_ATTENTE,
    ACCEPTED,
    REJECTED,
    EN_COURS,
    FINALISE;

    /** Anciennes valeurs encore présentes en base ou envoyées par le frontend → clé canonique. */
    public static final Map<String, CandidateStatus> LEGACY_ALIASES = Map.ofEntries(
            Map.entry("PENDING", EN_ATTENTE), Map.entry("SOUMIS", EN_ATTENTE), Map.entry("WAITLIST", EN_ATTENTE),
            Map.entry("ACCEPTE", ACCEPTED), Map.entry("ACCEPTEE", ACCEPTED), Map.entry("VALIDE", ACCEPTED),
            Map.entry("REFUSE", REJECTED), Map.entry("REJETE", REJECTED),
            Map.entry("FINALISEE", FINALISE));

    /**
     * Conversion tolérante (casse, accents, anciennes variantes).
     *
     * @throws IllegalArgumentException (HTTP 400) si la valeur est inconnue
     */
    public static CandidateStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Statut de candidature obligatoire");
        }
        String key = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT).replace(' ', '_');
        return Arrays.stream(values()).filter(s -> s.name().equals(key)).findFirst()
                .or(() -> java.util.Optional.ofNullable(LEGACY_ALIASES.get(key)))
                .orElseThrow(() -> new IllegalArgumentException("Statut de candidature inconnu « " + raw
                        + " ». Valeurs autorisées : "
                        + Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "))));
    }
}
