package ecole_221.sn.ecole.modules.planning.domain.port.repository;

import ecole_221.sn.ecole.modules.planning.domain.model.Room;
import ecole_221.sn.ecole.modules.planning.domain.model.Slot;
import ecole_221.sn.ecole.modules.planning.domain.model.Filiere;
import ecole_221.sn.ecole.modules.planning.domain.model.Classe;
import ecole_221.sn.ecole.modules.planning.domain.valueobject.SlotType;

import java.util.List;
import java.util.Optional;

/**
 * Output Ports pour la persistance des entités de planning.
 * Définit les opérations de persistance pour les agrégats Room, Slot, Filiere et Classe.
 */
public interface PlanningRepositoryPort {
    
    // ==================== REPOSITORY SALLES ====================
    
    /**
     * Sauvegarde une salle (création ou modification).
     */
    Room saveRoom(Room room);
    
    /**
     * Récupère une salle par son ID.
     */
    Optional<Room> findRoomById(String roomId);
    
    /**
     * Récupère toutes les salles.
     */
    List<Room> findAllRooms();
    
    /**
     * Supprime une salle par son ID.
     */
    void deleteRoom(String roomId);
    
    /**
     * Récupère les salles par capacité minimale.
     */
    List<Room> findRoomsByCapacityMin(int minCapacity);
    
    /**
     * Récupère les salles par emplacement.
     */
    List<Room> findRoomsByLocation(String location);
    
    // ==================== REPOSITORY CRÉNEAUX ====================
    
    /**
     * Sauvegarde un créneau horaire (création ou modification).
     */
    Slot saveSlot(Slot slot);
    
    /**
     * Récupère un créneau horaire par son ID.
     */
    Optional<Slot> findSlotById(String slotId);
    
    /**
     * Récupère tous les créneaux horaires.
     */
    List<Slot> findAllSlots();
    
    /**
     * Supprime un créneau horaire par son ID.
     */
    void deleteSlot(String slotId);
    
    /**
     * Récupère les créneaux par jour de la semaine.
     */
    List<Slot> findSlotsByDayOfWeek(String dayOfWeek);
    
    /**
     * Récupère les créneaux par type.
     */
    List<Slot> findSlotsByType(SlotType type);
    
    // ==================== REPOSITORY FILIÈRES ====================
    
    /**
     * Sauvegarde une filière (création ou modification).
     */
    Filiere saveFiliere(Filiere filiere);
    
    /**
     * Récupère une filière par son ID.
     */
    Optional<Filiere> findFiliereById(String filiereId);
    
    /**
     * Récupère toutes les filières.
     */
    List<Filiere> findAllFilieres();
    
    /**
     * Supprime une filière par son ID.
     */
    void deleteFiliere(String filiereId);
    
    /**
     * Récupère les filières par niveau.
     */
    List<Filiere> findFilieresByLevel(String level);
    
    // ==================== REPOSITORY CLASSES ====================
    
    /**
     * Sauvegarde une classe (création ou modification).
     */
    Classe saveClasse(Classe classe);
    
    /**
     * Récupère une classe par son ID.
     */
    Optional<Classe> findClasseById(String classeId);
    
    /**
     * Récupère toutes les classes.
     */
    List<Classe> findAllClasses();
    
    /**
     * Supprime une classe par son ID.
     */
    void deleteClasse(String classeId);
    
    /**
     * Récupère les classes par filière.
     */
    List<Classe> findClassesByFiliere(String filiereId);
    
    /**
     * Récupère les classes par niveau.
     */
    List<Classe> findClassesByLevel(String level);
}