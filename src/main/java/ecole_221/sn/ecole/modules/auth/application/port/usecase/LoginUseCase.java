package ecole_221.sn.ecole.modules.auth.application.port.usecase;

import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.application.port.dto.LoginResponseDto;

public interface LoginUseCase {
    LoginResponseDto login(LoginCommand command);
}
