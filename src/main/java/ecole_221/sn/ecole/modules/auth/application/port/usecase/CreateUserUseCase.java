package ecole_221.sn.ecole.modules.auth.application.port.usecase;

import ecole_221.sn.ecole.modules.auth.application.port.command.CreateUserCommand;
import ecole_221.sn.ecole.modules.auth.domain.model.User;

/**
 * Input Port pour la création d'un utilisateur.
 */
public interface CreateUserUseCase {
    /**
     * Crée un nouvel utilisateur.
     */
    User create(CreateUserCommand command);
}