package ecole_221.sn.ecole.modules.platform.organization.domain.valueobject;

import java.util.Locale;

/**
 * Value Object représentant le statut d'organization.
 * Immutabilité garantie par le mot-clé record.
 */
public record OrganizationStatus(String value) {
    
    public OrganizationStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut d'organization ne peut pas être vide");
        }
        
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!isValidStatus(normalized)) {
            throw new IllegalArgumentException("Statut d'organization invalide: " + value + 
                ". Statuts autorisés: PENDING, ACTIVE, SUSPENDED, REJECTED, EXPIRED, CANCELLED");
        }
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("PENDING") || 
               status.equals("ACTIVE") || 
               status.equals("SUSPENDED") || 
               status.equals("REJECTED") || 
               status.equals("EXPIRED") || 
               status.equals("CANCELLED");
    }
    
    public static OrganizationStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le statut d'organization ne peut pas être vide");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        String finalStatus = switch (normalized) {
            case "PENDING", "EN_ATTENTE" -> "PENDING";
            case "ACTIVE", "ACTIF" -> "ACTIVE";
            case "SUSPENDED", "SUSPENDU" -> "SUSPENDED";
            case "REJECTED", "REJETE" -> "REJECTED";
            case "EXPIRED", "EXPIRE" -> "EXPIRED";
            case "CANCELLED", "ANNULE" -> "CANCELLED";
            default -> normalized;
        };

        return new OrganizationStatus(finalStatus);
    }
    
    public static OrganizationStatus valueOf(String value) {
        return fromString(value);
    }
    
    // Statuts prédéfinis
    public static final OrganizationStatus PENDING = new OrganizationStatus("PENDING");
    public static final OrganizationStatus ACTIVE = new OrganizationStatus("ACTIVE");
    public static final OrganizationStatus SUSPENDED = new OrganizationStatus("SUSPENDED");
    public static final OrganizationStatus REJECTED = new OrganizationStatus("REJECTED");
    public static final OrganizationStatus EXPIRED = new OrganizationStatus("EXPIRED");
    public static final OrganizationStatus CANCELLED = new OrganizationStatus("CANCELLED");
    
    public String displayName() {
        return switch (value) {
            case "PENDING" -> "En attente";
            case "ACTIVE" -> "Actif";
            case "SUSPENDED" -> "Suspendu";
            case "REJECTED" -> "Rejeté";
            case "EXPIRED" -> "Expiré";
            case "CANCELLED" -> "Annulé";
            default -> value;
        };
    }
    
    public boolean canBeActivated() {
        return this.equals(PENDING);
    }
    
    public boolean canBeSuspended() {
        return this.equals(ACTIVE);
    }
    
    public boolean canBeReactivated() {
        return this.equals(SUSPENDED);
    }
}
