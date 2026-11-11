package ecole_221.sn.ecole.modules.referential.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Listes de référence connues du code (les VALEURS, elles, vivent en base : table reference_values).
 *
 * <ul>
 *   <li>{@link Mode#OPEN}   : la plateforme et chaque école peuvent ajouter, renommer ou masquer des valeurs.</li>
 *   <li>{@link Mode#LOCKED} : les clés sont imposées par le code (workflow) ; seuls libellé, couleur
 *       et ordre sont modifiables. Ajouter une clé inconnue du code la ferait ignorer ou planter.</li>
 * </ul>
 * Pour rendre une nouvelle liste paramétrable : ajouter une constante ici + ses valeurs de départ
 * dans {@code ReferenceDataSeeder}.
 */
public enum ReferenceList {

    EXPENSE_CATEGORY("expense-category", "Catégories de dépenses", Mode.OPEN),
    CANDIDATE_STATUS("candidate-status", "Statuts de candidature", Mode.LOCKED);

    public enum Mode { OPEN, LOCKED }

    private final String slug;
    private final String label;
    private final Mode mode;

    ReferenceList(String slug, String label, Mode mode) {
        this.slug = slug;
        this.label = label;
        this.mode = mode;
    }

    public String slug() { return slug; }
    public String label() { return label; }
    public Mode mode() { return mode; }
    public boolean isLocked() { return mode == Mode.LOCKED; }

    /** Accepte le slug d'URL ("expense-category") ou le nom ("EXPENSE_CATEGORY"). */
    public static Optional<ReferenceList> fromSlug(String raw) {
        if (raw == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(l -> l.slug.equalsIgnoreCase(raw) || l.name().equalsIgnoreCase(raw))
                .findFirst();
    }
}
