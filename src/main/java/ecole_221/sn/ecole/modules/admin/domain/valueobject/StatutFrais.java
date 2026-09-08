package ecole_221.sn.ecole.modules.admin.domain.valueobject;

/**
 * Statut des frais d'un étudiant selon le contrat de données BACKEND_DATA_CONTRACT.md
 * Valeurs autorisées: "En Règle" | "En Retard"
 */
public enum StatutFrais {
    EN_REGLE("En Règle"),
    EN_RETARD("En Retard");

    private final String displayName;

    StatutFrais(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static StatutFrais fromString(String value) {
        if (value == null) return EN_RETARD;
        String normalizedValue = value.trim().toLowerCase();
        return switch (normalizedValue) {
            case "en règle", "en regle", "regle", "paye" -> EN_REGLE;
            case "en retard", "retard", "impaye", "suspendu" -> EN_RETARD;
            default -> EN_RETARD; // Default fallback
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
