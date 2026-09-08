package ecole_221.sn.ecole.modules.auth.domain.valueobject;

import java.util.Locale;

/**
 * Value Object représentant le rôle d'un utilisateur.
 * Immutabilité garantie par le mot-clé record.
 */
public record RoleUtilisateur(String value) {
    
    public RoleUtilisateur {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le rôle ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!isValidRole(normalized)) {
            throw new IllegalArgumentException("Rôle invalide: " + value + 
                ". Rôles autorisés: ADMIN, SUPER_ADMIN, ETUDIANT, PROFESSEUR, SECRETAIRE, COMPTABLE, VIGIL, VISITEUR, PARENT");
        }
    }
    
    private boolean isValidRole(String role) {
        return role.equals("ADMIN") || 
               role.equals("ADMINISTRATOR") ||
               role.equals("SUPER_ADMIN") || 
               role.equals("ETUDIANT") || 
               role.equals("STUDENT") ||
               role.equals("PROFESSEUR") || 
               role.equals("TEACHER") ||
               role.equals("SECRETAIRE") || 
               role.equals("COMPTABLE") || 
               role.equals("VIGIL") || 
               role.equals("VISITEUR") ||
               role.equals("PARENT");
    }
    
    /**
     * Convertit une chaîne en RoleUtilisateur avec des alias supportés.
     */
    public static RoleUtilisateur fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le rôle ne peut pas être vide");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        String finalRole = switch (normalized) {
            case "ETUDIANT", "STUDENT" -> "ETUDIANT";
            case "PROFESSEUR", "PROFESSOR", "TEACHER" -> "PROFESSEUR";
            case "SECRETAIRE", "SECRETARY" -> "SECRETAIRE";
            case "COMPTABLE", "ACCOUNTANT" -> "COMPTABLE";
            case "VIGIL", "VIGILE", "GUARD" -> "VIGIL";
            case "VISITEUR", "VISITOR" -> "VISITEUR";
            case "SUPER_ADMIN", "SUPERADMIN", "SUPER-ADMIN" -> "SUPER_ADMIN";
            case "PARENT" -> "PARENT";
            default -> normalized;
        };

        return new RoleUtilisateur(finalRole);
    }

    /**
     * Méthode valueOf pour compatibilité avec les conventions Java (alias de fromString).
     */
    public static RoleUtilisateur valueOf(String value) {
        return fromString(value);
    }
    
    // Rôles prédéfinis
    public static final RoleUtilisateur ADMIN = new RoleUtilisateur("ADMIN");
    public static final RoleUtilisateur ADMINISTRATOR = new RoleUtilisateur("ADMINISTRATOR");
    public static final RoleUtilisateur SUPER_ADMIN = new RoleUtilisateur("SUPER_ADMIN");
    public static final RoleUtilisateur ETUDIANT = new RoleUtilisateur("ETUDIANT");
    public static final RoleUtilisateur STUDENT = new RoleUtilisateur("STUDENT");
    public static final RoleUtilisateur PROFESSEUR = new RoleUtilisateur("PROFESSEUR");
    public static final RoleUtilisateur TEACHER = new RoleUtilisateur("TEACHER");
    public static final RoleUtilisateur SECRETAIRE = new RoleUtilisateur("SECRETAIRE");
    public static final RoleUtilisateur COMPTABLE = new RoleUtilisateur("COMPTABLE");
    public static final RoleUtilisateur VIGIL = new RoleUtilisateur("VIGIL");
    public static final RoleUtilisateur VISITEUR = new RoleUtilisateur("VISITEUR");
    public static final RoleUtilisateur PARENT = new RoleUtilisateur("PARENT");
    
    /**
     * Pour la compatibilité avec Spring Security (retourne le nom du rôle).
     */
    public String name() {
        return value;
    }
}