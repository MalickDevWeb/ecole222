package ecole_221.sn.ecole.modules.referential.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Une valeur d'une liste de référence.
 *
 * @param organizationId null = valeur plateforme (globale) ; sinon valeur ou surcharge propre à une école
 * @param system         true = valeur de départ fournie par la plateforme : jamais supprimée globalement
 *                       (on la masque avec active=false), et non désactivable dans une liste LOCKED
 */
public record ReferenceValue(String id, ReferenceList list, String code, String label, String color,
                             int sortOrder, boolean active, boolean system, String organizationId) {

    public static final int MAX_LABEL = 80;

    public ReferenceValue {
        Objects.requireNonNull(list, "La liste est obligatoire");
        if (code == null || !code.matches("[A-Z0-9_]{2,40}")) {
            throw new IllegalArgumentException("Code invalide : 2 à 40 caractères A-Z, 0-9 ou _ (reçu : " + code + ")");
        }
        if (label == null || label.isBlank() || label.length() > MAX_LABEL) {
            throw new IllegalArgumentException("Le libellé est obligatoire (" + MAX_LABEL + " caractères max)");
        }
        label = label.trim();
    }

    public boolean isGlobal() { return organizationId == null; }

    /** Copie avec les champs modifiables ; un argument null conserve la valeur actuelle. */
    public ReferenceValue withChanges(String newLabel, String newColor, Integer newOrder, Boolean newActive) {
        return new ReferenceValue(id, list, code,
                newLabel != null ? newLabel : label,
                newColor != null ? newColor : color,
                newOrder != null ? newOrder : sortOrder,
                newActive != null ? newActive : active,
                system, organizationId);
    }

    /** Copie rattachée à une école (surcharge d'une valeur globale). */
    public ReferenceValue asOverrideFor(String newId, String orgId) {
        return new ReferenceValue(newId, list, code, label, color, sortOrder, active, system, orgId);
    }

    /** "Matériel & Équipement" → "MATERIEL_EQUIPEMENT". */
    public static String toCode(String raw) {
        if (raw == null) return null;
        String ascii = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String code = ascii.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        return code.length() > 40 ? code.substring(0, 40) : code;
    }
}
