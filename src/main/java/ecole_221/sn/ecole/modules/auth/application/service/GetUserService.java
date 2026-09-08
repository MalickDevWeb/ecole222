package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.application.port.usecase.GetUserUseCase;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service implémentant le UseCase de consultation des utilisateurs.
 */
@Service
public class GetUserService implements GetUserUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetUserService.class);

    private final UserRepositoryPort userRepositoryPort;

    public GetUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public Optional<User> getById(String id) {
        logger.debug("Récupération de l'utilisateur par ID: {}", id);
        return userRepositoryPort.findById(id);
    }

    @Override
    public Optional<User> getByEmail(String email) {
        logger.debug("Récupération de l'utilisateur par email: {}", email);
        return userRepositoryPort.findByEmail(email);
    }

    @Override
    public List<User> getAll() {
        logger.debug("Récupération de tous les utilisateurs");
        return userRepositoryPort.findAll();
    }

    @Override
    public List<User> getByRole(String role) {
        logger.debug("Récupération des utilisateurs par rôle: {}", role);
        RoleUtilisateur roleUtilisateur = RoleUtilisateur.fromString(role);
        return userRepositoryPort.findByRole(roleUtilisateur);
    }
}