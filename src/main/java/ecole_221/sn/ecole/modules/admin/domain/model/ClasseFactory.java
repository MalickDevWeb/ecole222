package ecole_221.sn.ecole.modules.admin.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de Classe.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class ClasseFactory {
    
    /**
     * Crée une nouvelle classe avec un ID généré.
     */
    public static Classe create(String name, String filiereId) {
        String id = UUID.randomUUID().toString();
        return new Classe(id, name, filiereId);
    }
    
    /**
     * Reconstruit une classe existante (pour la persistance).
     */
    public static Classe reconstruct(String id, String name, String filiereId) {
        return new Classe(id, name, filiereId);
    }
}