package ecole_221.sn.ecole.modules.student.domain.valueobject;

/**
 * Value Object représentant le statut de présence.
 * Immutabilité garantie par le mot-clé record.
 */
public record AttendanceStatus(String value) {
    
    public AttendanceStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut de présence ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = normalizeAlias(value.trim().toUpperCase());
        if (!isValidStatus(normalized)) {
            throw new IllegalArgumentException("Statut de présence invalide: " + value + 
                ". Statuts autorisés: PRESENT, ABSENT, LATE, EXCUSED, UNKNOWN");
        }
        value = normalized;
    }

    private static String normalizeAlias(String status) {
        return switch (status) {
            case "COURSE", "COURS" -> "PRESENT";
            default -> status;
        };
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("PRESENT") || 
               status.equals("ABSENT") || 
               status.equals("LATE") || 
               status.equals("EXCUSED") || 
               status.equals("UNKNOWN");
    }
    
    // Statuts prédéfinis
    public static final AttendanceStatus PRESENT = new AttendanceStatus("PRESENT");
    public static final AttendanceStatus ABSENT = new AttendanceStatus("ABSENT");
    public static final AttendanceStatus LATE = new AttendanceStatus("LATE");
    public static final AttendanceStatus EXCUSED = new AttendanceStatus("EXCUSED");
    public static final AttendanceStatus UNKNOWN = new AttendanceStatus("UNKNOWN");
    
    // Méthode de conversion String → AttendanceStatus
    public static AttendanceStatus valueOf(String status) {
        String normalized = normalizeAlias(status.trim().toUpperCase());
        return switch (normalized) {
            case "PRESENT" -> PRESENT;
            case "ABSENT" -> ABSENT;
            case "LATE" -> LATE;
            case "EXCUSED" -> EXCUSED;
            case "UNKNOWN" -> UNKNOWN;
            default -> throw new IllegalArgumentException("Statut de présence invalide: " + status);
        };
    }
    
    // Méthode de conversion AttendanceStatus → String
    public String name() {
        return value();
    }
}