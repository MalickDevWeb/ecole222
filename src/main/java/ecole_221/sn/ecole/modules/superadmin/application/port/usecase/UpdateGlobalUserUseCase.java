package ecole_221.sn.ecole.modules.superadmin.application.port.usecase;

import ecole_221.sn.ecole.modules.superadmin.application.port.command.UpdateGlobalUserCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.SuperAdminUserDto;

/**
 * Use Case : mise à jour partielle d'un utilisateur global.
 */
public interface UpdateGlobalUserUseCase {
    SuperAdminUserDto execute(String userId, UpdateGlobalUserCommand command);
}
