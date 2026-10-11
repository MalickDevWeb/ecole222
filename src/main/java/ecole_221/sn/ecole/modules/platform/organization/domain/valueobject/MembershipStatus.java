package ecole_221.sn.ecole.modules.platform.organization.domain.valueobject;

import java.util.Locale;

/**
 * Value Object représentant le statut de membership.
 * Immutabilité garantie par le mot-clé record.
 */
public record MembershipStatus(String value) {
    
    public MembershipStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut de membership ne peut pas être vide");
        }
        
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!isValidStatus(normalized)) {
            throw new IllegalArgumentException("Statut de membership invalide: " + value + 
                ". Statuts autorisés: ACTIVE, INVITED, SUSPENDED, EXPIRED, TRANSFERRED");
        }
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("ACTIVE") || 
               status.equals("INVITED") || 
               status.equals("SUSPENDED") || 
               status.equals("EXPIRED") || status.equals("TRANSFERRED");
    }
    
    public static MembershipStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le statut de membership ne peut pas être vide");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        String finalStatus = switch (normalized) {
            case "ACTIVE", "ACTIF" -> "ACTIVE";
            case "INVITED", "INVITE" -> "INVITED";
            case "SUSPENDED", "SUSPENDU" -> "SUSPENDED";
            case "EXPIRED", "EXPIRE" -> "EXPIRED";
            case "TRANSFERRED", "TRANSFERE" -> "TRANSFERRED";
            default -> normalized;
        };

        return new MembershipStatus(finalStatus);
    }
    
    public static MembershipStatus valueOf(String value) {
        return fromString(value);
    }
    
    // Statuts prédéfinis
    public static final MembershipStatus ACTIVE = new MembershipStatus("ACTIVE");
    public static final MembershipStatus INVITED = new MembershipStatus("INVITED");
    public static final MembershipStatus SUSPENDED = new MembershipStatus("SUSPENDED");
    public static final MembershipStatus EXPIRED = new MembershipStatus("EXPIRED");
    public static final MembershipStatus TRANSFERRED = new MembershipStatus("TRANSFERRED");
    
    public String displayName() {
        return switch (value) {
            case "ACTIVE" -> "Actif";
            case "INVITED" -> "Invité";
            case "SUSPENDED" -> "Suspendu";
            case "EXPIRED" -> "Expiré";
            case "TRANSFERRED" -> "Transféré";
            default -> value;
        };
    }
}
