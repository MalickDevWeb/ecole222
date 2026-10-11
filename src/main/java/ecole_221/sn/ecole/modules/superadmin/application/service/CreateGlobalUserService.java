package ecole_221.sn.ecole.modules.superadmin.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.superadmin.application.port.command.CreateGlobalUserCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.SuperAdminUserDto;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.CreateGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.EmailAlreadyExistsException;
import ecole_221.sn.ecole.modules.superadmin.domain.valueobject.GlobalUserEmail;
import ecole_221.sn.ecole.modules.superadmin.domain.valueobject.GlobalUserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CreateGlobalUserService implements CreateGlobalUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    public CreateGlobalUserService(UserRepositoryPort userRepositoryPort, PasswordEncoder passwordEncoder) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SuperAdminUserDto execute(CreateGlobalUserCommand command) {
        // Validation Domaine (Value Objects — fail-fast au niveau du domaine)
        GlobalUserEmail email = new GlobalUserEmail(command.email());
        GlobalUserRole role = new GlobalUserRole(command.role());

        // Validation Métier : Unicité de l'email
        if (userRepositoryPort.existsByEmail(email.value())) {
            throw new EmailAlreadyExistsException(email.value());
        }

        // Utilisation de UserFactory (pattern établi dans le projet)
        String matricule = "GA-" + System.currentTimeMillis();
        User user = UserFactory.create(
                command.nom(),
                command.prenom(),
                email.value(),
                passwordEncoder.encode(command.password()),
                matricule,
                RoleUtilisateur.fromString(role.value()),
                null
        );

        User savedUser = userRepositoryPort.save(user);

        return new SuperAdminUserDto(
                savedUser.getId(),
                savedUser.getNom(),
                savedUser.getPrenom(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getMatricule()
        );
    }
}
