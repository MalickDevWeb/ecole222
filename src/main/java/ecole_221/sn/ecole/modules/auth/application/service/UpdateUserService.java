package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.application.port.command.UpdateUserCommand;
import ecole_221.sn.ecole.modules.auth.application.port.usecase.UpdateUserUseCase;
import ecole_221.sn.ecole.modules.auth.domain.exception.UserNotFoundException;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service implémentant le UseCase de mise à jour d'utilisateur.
 */
@Service
public class UpdateUserService implements UpdateUserUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateUserService.class);
    
    private final UserRepositoryPort userRepositoryPort;
    
    public UpdateUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }
    
    @Override
    public User update(UpdateUserCommand command) {
        logger.info("Mise à jour de l'utilisateur: {}", command.id());
        
        // Récupération de l'utilisateur existant
        User user = userRepositoryPort.findById(command.id())
            .orElseThrow(() -> new UserNotFoundException(command.id()));
        
        // Mise à jour des champs
        user.updateProfile(command.nom(), command.prenom(), command.photoUrl());
        
        // Sauvegarde
        User updatedUser = userRepositoryPort.save(user);
        
        logger.info("Utilisateur mis à jour avec succès: {}", updatedUser.getId());
        return updatedUser;
    }
    
    @Override
    public User changePassword(String userId, String newPassword) {
        logger.info("Changement de mot de passe pour l'utilisateur: {}", userId);
        
        // Récupération de l'utilisateur existant
        User user = userRepositoryPort.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Changement du mot de passe
        user.changePassword(newPassword);
        
        // Sauvegarde
        User updatedUser = userRepositoryPort.save(user);
        
        logger.info("Mot de passe changé avec succès pour: {}", updatedUser.getId());
        return updatedUser;
    }
}