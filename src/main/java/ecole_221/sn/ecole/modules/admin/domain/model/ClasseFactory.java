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
    /** Reconstruit une classe avec son niveau et sa capacité réellement saisis (peuvent être null). */
    public static Classe reconstruct(String id, String name, String filiereId, String level, Integer capacity) {
        Classe c = new Classe(id, name, filiereId);
        c.setLevel(level);
        c.setCapacity(capacity);
        return c;
    }

    public static Classe reconstruct(String id, String name, String filiereId, String level, Integer capacity, Long price) {
        Classe c = reconstruct(id, name, filiereId, level, capacity);
        c.setPrice(price);
        return c;
    }

    public static Classe reconstruct(String id, String name, String filiereId) {
        return new Classe(id, name, filiereId);
    }
}