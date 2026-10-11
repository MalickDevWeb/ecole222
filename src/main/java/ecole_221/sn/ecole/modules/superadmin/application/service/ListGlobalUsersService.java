package ecole_221.sn.ecole.modules.superadmin.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.SuperAdminUserDto;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.ListGlobalUsersUseCase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ListGlobalUsersService implements ListGlobalUsersUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public ListGlobalUsersService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public List<SuperAdminUserDto> execute() {
        // On récupère uniquement les SUPER_ADMIN, PLATFORM_ADMIN et ADMIN
        List<User> superAdmins = userRepositoryPort.findByRole(RoleUtilisateur.SUPER_ADMIN);
        List<User> platformAdmins = userRepositoryPort.findByRole(RoleUtilisateur.PLATFORM_ADMIN);
        List<User> admins = userRepositoryPort.findByRole(RoleUtilisateur.ADMIN);

        List<User> globalUsers = new ArrayList<>();
        globalUsers.addAll(superAdmins);
        globalUsers.addAll(platformAdmins);
        globalUsers.addAll(admins);

        return globalUsers.stream()
                .map(user -> new SuperAdminUserDto(
                        user.getId(),
                        user.getNom(),
                        user.getPrenom(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getMatricule()
                ))
                .toList();
    }
}
