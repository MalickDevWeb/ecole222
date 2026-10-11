package ecole_221.sn.ecole.modules.superadmin.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.SuperAdminUserDto;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.GetGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.GlobalUserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetGlobalUserService implements GetGlobalUserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public GetGlobalUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public SuperAdminUserDto execute(String userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new GlobalUserNotFoundException(userId));

        return new SuperAdminUserDto(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRole().name(),
                user.getMatricule()
        );
    }
}
