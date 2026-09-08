package ecole_221.sn.ecole.modules.security.domain.port.repository;

import ecole_221.sn.ecole.modules.security.domain.model.VigilProfile;

import java.util.List;
import java.util.Optional;

/**
 * Interface du repository pour le module Security.
 * Définit les opérations de persistance pour les profils de vigilance.
 */
public interface SecurityRepositoryPort {
    
    /**
     * Récupère tous les profils de vigilance.
     */
    List<VigilProfile> findAllVigilProfiles();
    
    /**
     * Récupère un profil de vigilance par son identifiant.
     */
    Optional<VigilProfile> findVigilProfileById(String id);
    
    /**
     * Sauvegarde un profil de vigilance.
     */
    VigilProfile saveVigilProfile(VigilProfile vigilProfile);
    
    /**
     * Supprime un profil de vigilance.
     */
    void deleteVigilProfile(String id);
    
    /**
     * Récupère les profils de vigilance par utilisateur.
     * Note: Non implémenté car VigilProfileJpaEntity n'a pas de champ userId
     */
    List<VigilProfile> findVigilProfilesByUserId(String userId);
    
    /**
     * Récupère les profils de vigilance par rôle.
     */
    List<VigilProfile> findVigilProfilesByRole(String role);
}