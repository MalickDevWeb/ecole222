package ecole_221.sn.ecole.modules.auth.application.port.usecase;

import ecole_221.sn.ecole.modules.auth.application.port.command.UpdateUserCommand;
import ecole_221.sn.ecole.modules.auth.domain.model.User;

/**
 * Input Port pour la mise à jour d'un utilisateur.
 */
public interface UpdateUserUseCase {
    /**
     * Met à jour les informations d'un utilisateur.
     */
    User update(UpdateUserCommand command);
    
    /**
     * Change le mot de passe d'un utilisateur.
     */
    User changePassword(String userId, String newPassword);
}