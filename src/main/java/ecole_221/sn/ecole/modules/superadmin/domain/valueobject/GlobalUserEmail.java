package ecole_221.sn.ecole.modules.superadmin.domain.valueobject;

import java.util.regex.Pattern;

/**
 * Value Object représentant l'email d'un utilisateur global (Super Admin / Admin).
 * Encapsule la logique de validation stricte au niveau du domaine.
 */
public record GlobalUserEmail(String value) {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    public GlobalUserEmail {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email ne peut pas être vide");
        }
        
        String normalized = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Format d'email invalide: " + value);
        }
        
        value = normalized;
    }
}
