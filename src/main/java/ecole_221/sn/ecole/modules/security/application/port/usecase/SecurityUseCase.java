package ecole_221.sn.ecole.modules.security.application.port.usecase;

import ecole_221.sn.ecole.modules.security.application.port.dto.VigilProfileDto;
import java.util.List;
import java.util.Map;

/**
 * Interface des cas d'utilisation pour le module Security.
 * Gère les profils de vigilance et les opérations de sécurité.
 */
public interface SecurityUseCase {
    
    // ==================== GESTION DES PROFILS DE VIGILANCE ====================
    
    /**
     * Récupère tous les profils de vigilance.
     */
    List<VigilProfileDto> getAllVigilProfiles();
    
    /**
     * Récupère un profil de vigilance par son identifiant.
     */
    VigilProfileDto getVigilProfileById(String id);
    
    /**
     * Récupère les profils de vigilance par utilisateur.
     */
    List<VigilProfileDto> getVigilProfilesByUserId(String userId);
    
    /**
     * Récupère les profils de vigilance par rôle.
     */
    List<VigilProfileDto> getVigilProfilesByRole(String role);
    
    /**
     * Crée un nouveau profil de vigilance.
     */
    VigilProfileDto createVigilProfile(VigilProfileDto vigilProfile);
    
    /**
     * Met à jour un profil de vigilance.
     */
    VigilProfileDto updateVigilProfile(String id, VigilProfileDto vigilProfile);
    
    /**
     * Supprime un profil de vigilance.
     */
    void deleteVigilProfile(String id);
    
    // ==================== OPÉRATIONS DE SÉCURITÉ ====================
    
    /**
     * Récupère le statut du point de contrôle.
     */
    Map<String, Object> getCheckpointStatus();
    
    /**
     * Traite un scan de badge.
     */
    Map<String, Object> processBadgeScan(String badgeId);
    
    /**
     * Récupère les points de contrôle de vigilance.
     */
    List<Map<String, Object>> getVigilCheckIns();
    
    /**
     * Récupère le profil de vigilance (méthode legacy).
     */
    Map<String, Object> getVigilProfile(String vigilId);
}
