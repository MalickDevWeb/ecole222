package ecole_221.sn.ecole.modules.platform.organization.domain.valueobject;

import java.util.Locale;

/**
 * Value Object représentant le type d'organization.
 * Immutabilité garantie par le mot-clé record.
 */
public record OrganizationType(String value) {
    
    public OrganizationType {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le type d'organization ne peut pas être vide");
        }
        
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!isValidType(normalized)) {
            throw new IllegalArgumentException("Type d'organization invalide: " + value + 
                ". Types autorisés: ECOLE, UNIVERSITE, INSTITUT, CENTRE_FORMATION, LYCEE, COLLEGE");
        }
    }
    
    private boolean isValidType(String type) {
        return type.equals("ECOLE") || 
               type.equals("UNIVERSITE") || 
               type.equals("INSTITUT") || 
               type.equals("CENTRE_FORMATION") || 
               type.equals("LYCEE") || 
               type.equals("COLLEGE");
    }
    
    public static OrganizationType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le type d'organization ne peut pas être vide");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        String finalType = switch (normalized) {
            case "SCHOOL", "ECOLE" -> "ECOLE";
            case "UNIVERSITY", "UNIVERSITE" -> "UNIVERSITE";
            case "INSTITUTE", "INSTITUT" -> "INSTITUT";
            case "TRAINING_CENTER", "CENTRE_FORMATION" -> "CENTRE_FORMATION";
            case "HIGH_SCHOOL", "LYCEE" -> "LYCEE";
            case "MIDDLE_SCHOOL", "COLLEGE" -> "COLLEGE";
            default -> normalized;
        };

        return new OrganizationType(finalType);
    }
    
    public static OrganizationType valueOf(String value) {
        return fromString(value);
    }
    
    // Types prédéfinis
    public static final OrganizationType ECOLE = new OrganizationType("ECOLE");
    public static final OrganizationType UNIVERSITE = new OrganizationType("UNIVERSITE");
    public static final OrganizationType INSTITUT = new OrganizationType("INSTITUT");
    public static final OrganizationType CENTRE_FORMATION = new OrganizationType("CENTRE_FORMATION");
    public static final OrganizationType LYCEE = new OrganizationType("LYCEE");
    public static final OrganizationType COLLEGE = new OrganizationType("COLLEGE");
    
    public String displayName() {
        return switch (value) {
            case "ECOLE" -> "École";
            case "UNIVERSITE" -> "Université";
            case "INSTITUT" -> "Institut";
            case "CENTRE_FORMATION" -> "Centre de Formation";
            case "LYCEE" -> "Lycée";
            case "COLLEGE" -> "Collège";
            default -> value;
        };
    }
}
