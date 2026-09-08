package ecole_221.sn.ecole.modules.admin.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de TimeSlot.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class TimeSlotFactory {
    
    /**
     * Crée un nouveau créneau horaire avec un ID généré.
     */
    public static TimeSlot create(String day, String heureDebut, String heureFin, String nom, 
                                 String type, String professeur, String salle, String description, 
                                 String classeId) {
        String id = UUID.randomUUID().toString();
        return new TimeSlot(id, day, heureDebut, heureFin, nom, type, professeur, salle, description, classeId);
    }
    
    /**
     * Reconstruit un créneau horaire existant (pour la persistance).
     */
    public static TimeSlot reconstruct(String id, String day, String heureDebut, String heureFin, String nom, 
                                      String type, String professeur, String salle, String description, 
                                      String classeId) {
        return new TimeSlot(id, day, heureDebut, heureFin, nom, type, professeur, salle, description, classeId);
    }
}