package ecole_221.sn.ecole.modules.admin.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de Filiere.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class FiliereFactory {
    
    /**
     * Crée une nouvelle filière avec un ID généré.
     */
    public static Filiere create(String code, String name) {
        String id = UUID.randomUUID().toString();
        return new Filiere(id, code, name);
    }
    
    /**
     * Reconstruit une filière existante (pour la persistance).
     */
    /** Reconstruit une filière avec sa description réellement saisie (peut être null). */
    public static Filiere reconstruct(String id, String code, String name, String description) {
        Filiere f = new Filiere(id, code, name);
        f.setDescription(description);
        return f;
    }

    public static Filiere reconstruct(String id, String code, String name) {
        return new Filiere(id, code, name);
    }
}