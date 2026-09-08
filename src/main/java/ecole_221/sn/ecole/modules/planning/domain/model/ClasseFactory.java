package ecole_221.sn.ecole.modules.planning.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de Classe.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class ClasseFactory {
    
    /**
     * Crée une nouvelle classe avec un ID généré.
     */
    public static Classe create(String name, String code, String filiereId, String level, int capacity) {
        String id = UUID.randomUUID().toString();
        return new Classe(id, name, code, filiereId, level, capacity, 0);
    }
    
    /**
     * Crée une nouvelle classe avec un ID généré et une taille initiale.
     */
    public static Classe create(String name, String code, String filiereId, String level, int capacity, int initialSize) {
        String id = UUID.randomUUID().toString();
        return new Classe(id, name, code, filiereId, level, capacity, initialSize);
    }
    
    /**
     * Reconstruit une classe existante (pour la persistance).
     */
    public static Classe reconstruct(String id, String name, String code, String filiereId, String level, int capacity, int currentSize) {
        return new Classe(id, name, code, filiereId, level, capacity, currentSize);
    }
}