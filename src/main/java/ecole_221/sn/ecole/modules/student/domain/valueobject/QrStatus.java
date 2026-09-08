package ecole_221.sn.ecole.modules.student.domain.valueobject;

/**
 * Statut QR d'un étudiant selon le contrat de données BACKEND_DATA_CONTRACT.md
 * Valeurs autorisées: "AUTORISÉ" | "SUSPENDU"
 */
public enum QrStatus {
    AUTORISE("AUTORISÉ"),
    SUSPENDU("SUSPENDU");

    private final String displayName;

    QrStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static QrStatus fromString(String value) {
        if (value == null) return SUSPENDU;
        String normalizedValue = value.trim().toUpperCase();
        return switch (normalizedValue) {
            case "AUTORISÉ", "AUTORISE" -> AUTORISE;
            case "SUSPENDU" -> SUSPENDU;
            default -> SUSPENDU; // Default fallback
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}