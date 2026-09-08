package ecole_221.sn.ecole.modules.auth.application.port.usecase;

import ecole_221.sn.ecole.modules.auth.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Input Port pour la consultation des utilisateurs.
 */
public interface GetUserUseCase {
    /**
     * Récupère un utilisateur par son ID.
     */
    Optional<User> getById(String id);
    
    /**
     * Récupère un utilisateur par son email.
     */
    Optional<User> getByEmail(String email);
    
    /**
     * Récupère tous les utilisateurs.
     */
    List<User> getAll();
    
    /**
     * Récupère les utilisateurs par rôle.
     */
    List<User> getByRole(String role);
}