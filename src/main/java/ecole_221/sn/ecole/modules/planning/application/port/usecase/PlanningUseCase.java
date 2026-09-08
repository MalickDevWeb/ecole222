package ecole_221.sn.ecole.modules.planning.application.port.usecase;

import ecole_221.sn.ecole.modules.planning.application.port.dto.*;
import ecole_221.sn.ecole.partage.PageResult;

import java.util.List;

/**
 * Interface des cas d'utilisation pour le module Planning.
 * Gère la gestion des salles, créneaux horaires, filières et classes.
 */
public interface PlanningUseCase {
    
    // ==================== GESTION DES SALLES ====================
    
    /**
     * Récupère toutes les salles disponibles.
     */
    List<RoomDto> getAllRooms();
    
    /**
     * Récupère une salle par son identifiant.
     */
    RoomDto getRoomById(String roomId);
    
    /**
     * Crée ou met à jour une salle.
     */
    RoomDto saveRoom(RoomDto roomDto);
    
    /**
     * Supprime une salle.
     */
    void deleteRoom(String roomId);
    
    // ==================== GESTION DES CRÉNEAUX HORAIRES ====================
    
    /**
     * Récupère tous les créneaux horaires.
     */
    List<SlotDto> getAllSlots();
    
    /**
     * Récupère un créneau horaire par son identifiant.
     */
    SlotDto getSlotById(String slotId);
    
    /**
     * Crée ou met à jour un créneau horaire.
     */
    SlotDto saveSlot(SlotDto slotDto);
    
    /**
     * Supprime un créneau horaire.
     */
    void deleteSlot(String slotId);
    
    // ==================== GESTION DES FILIÈRES ====================
    
    /**
     * Récupère toutes les filières.
     */
    List<FiliereDto> getAllFilieres();
    
    /**
     * Récupère une filière par son identifiant.
     */
    FiliereDto getFiliereById(String filiereId);
    
    /**
     * Crée ou met à jour une filière.
     */
    FiliereDto saveFiliere(FiliereDto filiereDto);
    
    /**
     * Supprime une filière.
     */
    void deleteFiliere(String filiereId);
    
    // ==================== GESTION DES CLASSES ====================
    
    /**
     * Récupère toutes les classes.
     */
    PageResult<ClasseDto> getAllClasses(int page, int size);
    
    /**
     * Récupère une classe par son identifiant.
     */
    ClasseDto getClasseById(String classeId);
    
    /**
     * Crée ou met à jour une classe.
     */
    ClasseDto saveClasse(ClasseDto classeDto);
    
    /**
     * Supprime une classe.
     */
    void deleteClasse(String classeId);
    
    /**
     * Récupère les classes d'une filière.
     */
    List<ClasseDto> getClassesByFiliere(String filiereId);
}