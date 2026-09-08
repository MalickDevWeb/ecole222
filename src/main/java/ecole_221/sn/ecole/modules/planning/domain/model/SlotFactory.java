package ecole_221.sn.ecole.modules.planning.domain.model;

import ecole_221.sn.ecole.modules.planning.domain.valueobject.SlotType;

import java.util.UUID;

/**
 * Factory pour créer des instances de Slot.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class SlotFactory {
    
    /**
     * Crée un nouveau créneau horaire avec un ID généré.
     */
    public static Slot create(String startTime, String endTime, String dayOfWeek, SlotType type) {
        String id = UUID.randomUUID().toString();
        return new Slot(id, startTime, endTime, dayOfWeek, type);
    }
    
    /**
     * Reconstruit un créneau horaire existant (pour la persistance).
     */
    public static Slot reconstruct(String id, String startTime, String endTime, String dayOfWeek, SlotType type) {
        return new Slot(id, startTime, endTime, dayOfWeek, type);
    }
}