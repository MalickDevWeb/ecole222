package ecole_221.sn.ecole.modules.superadmin.application.port.usecase;

import ecole_221.sn.ecole.modules.superadmin.application.port.command.CreateGlobalUserCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.SuperAdminUserDto;

public interface CreateGlobalUserUseCase {
    SuperAdminUserDto execute(CreateGlobalUserCommand command);
}
