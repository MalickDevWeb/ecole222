package ecole_221.sn.ecole.modules.admin.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de Room.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class RoomFactory {
    
    /**
     * Crée une nouvelle salle avec un ID généré.
     */
    public static Room create(String name, int capacity) {
        String id = UUID.randomUUID().toString();
        return new Room(id, name, capacity);
    }
    
    /**
     * Reconstruit une salle existante (pour la persistance).
     */
    public static Room reconstruct(String id, String name, int capacity) {
        return new Room(id, name, capacity);
    }
}