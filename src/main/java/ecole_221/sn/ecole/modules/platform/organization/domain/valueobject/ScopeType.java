package ecole_221.sn.ecole.modules.platform.organization.domain.valueobject;

import java.util.Locale;

/**
 * Value Object représentant le type de scope pour les permissions.
 * Immutabilité garantie par le mot-clé record.
 */
public record ScopeType(String value) {
    
    public ScopeType {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le type de scope ne peut pas être vide");
        }
        
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!isValidScope(normalized)) {
            throw new IllegalArgumentException("Type de scope invalide: " + value + 
                ". Types autorisés: ORGANIZATION, FACULTY, DEPARTMENT, FORMATION, LEVEL, CLASS, COURSE");
        }
    }
    
    private boolean isValidScope(String scope) {
        return scope.equals("ORGANIZATION") || 
               scope.equals("FACULTY") || 
               scope.equals("DEPARTMENT") || 
               scope.equals("FORMATION") || 
               scope.equals("LEVEL") || 
               scope.equals("CLASS") || 
               scope.equals("COURSE");
    }
    
    public static ScopeType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le type de scope ne peut pas être vide");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        String finalScope = switch (normalized) {
            case "ORGANIZATION", "ORG" -> "ORGANIZATION";
            case "FACULTY", "FACULTE" -> "FACULTY";
            case "DEPARTMENT", "DEPARTEMENT" -> "DEPARTMENT";
            case "FORMATION", "PROGRAM" -> "FORMATION";
            case "LEVEL", "NIVEAU" -> "LEVEL";
            case "CLASS", "CLASSE" -> "CLASS";
            case "COURSE" -> "COURSE";
            default -> normalized;
        };

        return new ScopeType(finalScope);
    }
    
    public static ScopeType valueOf(String value) {
        return fromString(value);
    }
    
    // Types prédéfinis
    public static final ScopeType ORGANIZATION = new ScopeType("ORGANIZATION");
    public static final ScopeType FACULTY = new ScopeType("FACULTY");
    public static final ScopeType DEPARTMENT = new ScopeType("DEPARTMENT");
    public static final ScopeType FORMATION = new ScopeType("FORMATION");
    public static final ScopeType LEVEL = new ScopeType("LEVEL");
    public static final ScopeType CLASS = new ScopeType("CLASS");
    public static final ScopeType COURSE = new ScopeType("COURSE");
    
    public String displayName() {
        return switch (value) {
            case "ORGANIZATION" -> "Organisation";
            case "FACULTY" -> "Faculté";
            case "DEPARTMENT" -> "Département";
            case "FORMATION" -> "Formation";
            case "LEVEL" -> "Niveau";
            case "CLASS" -> "Classe";
            case "COURSE" -> "Cours";
            default -> value;
        };
    }
}
