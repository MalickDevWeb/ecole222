package ecole_221.sn.ecole.modules.superadmin.application.port.usecase;

import ecole_221.sn.ecole.modules.superadmin.application.port.dto.SuperAdminUserDto;

import java.util.List;

public interface ListGlobalUsersUseCase {
    List<SuperAdminUserDto> execute();
}
