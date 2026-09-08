package ecole_221.sn.ecole.modules.auth.domain.port.repository;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;

import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des utilisateurs.
 * Définit les opérations de persistance pour l'agrégat User.
 */
public interface UserRepositoryPort {
    
    /**
     * Sauvegarde un utilisateur (création ou modification).
     */
    User save(User user);
    
    /**
     * Récupère un utilisateur par son email.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Récupère un utilisateur par son ID.
     */
    Optional<User> findById(String id);
    
    /**
     * Récupère tous les utilisateurs.
     */
    List<User> findAll();
    
    /**
     * Récupère les utilisateurs par rôle.
     */
    List<User> findByRole(RoleUtilisateur role);
    
    /**
     * Supprime un utilisateur par son ID.
     */
    void deleteById(String id);
    
    /**
     * Vérifie si un utilisateur existe par email.
     */
    boolean existsByEmail(String email);
}
