package ecole_221.sn.ecole.modules.planning.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de Room.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class RoomFactory {
    
    /**
     * Crée une nouvelle salle avec un ID généré.
     */
    public static Room create(String name, int capacity, String equipment, String location) {
        String id = UUID.randomUUID().toString();
        return new Room(id, name, capacity, equipment, location);
    }
    
    /**
     * Reconstruit une salle existante (pour la persistance).
     */
    public static Room reconstruct(String id, String name, int capacity, String equipment, String location) {
        return new Room(id, name, capacity, equipment, location);
    }
}