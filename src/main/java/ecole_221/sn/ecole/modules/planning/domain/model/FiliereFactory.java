package ecole_221.sn.ecole.modules.planning.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de Filiere.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class FiliereFactory {
    
    /**
     * Crée une nouvelle filière avec un ID généré.
     */
    public static Filiere create(String name, String code, String description, String level) {
        String id = UUID.randomUUID().toString();
        return new Filiere(id, name, code, description, level);
    }
    
    /**
     * Reconstruit une filière existante (pour la persistance).
     */
    public static Filiere reconstruct(String id, String name, String code, String description, String level) {
        return new Filiere(id, name, code, description, level);
    }
}