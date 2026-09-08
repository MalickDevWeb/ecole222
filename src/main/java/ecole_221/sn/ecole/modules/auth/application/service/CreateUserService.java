package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.application.port.command.CreateUserCommand;
import ecole_221.sn.ecole.modules.auth.application.port.usecase.CreateUserUseCase;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service implémentant le UseCase de création d'utilisateur.
 */
@Service
public class CreateUserService implements CreateUserUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateUserService.class);
    
    private final UserRepositoryPort userRepositoryPort;
    
    public CreateUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }
    
    @Override
    public User create(CreateUserCommand command) {
        logger.info("Création d'un nouvel utilisateur: {}", command.email());
        
        // Vérification de l'unicité de l'email
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }
        
        // Conversion des types simples vers Value Objects
        RoleUtilisateur role = RoleUtilisateur.fromString(command.role());
        
        // Création via Factory
        User user = UserFactory.create(
            command.nom(),
            command.prenom(),
            command.email(),
            command.password(),
            command.matricule(),
            role,
            command.photoUrl()
        );
        
        // Sauvegarde
        User savedUser = userRepositoryPort.save(user);
        
        logger.info("Utilisateur créé avec succès: {}", savedUser.getId());
        return savedUser;
    }
}