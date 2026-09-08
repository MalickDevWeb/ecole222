package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.infrastructure.security.JwtService;
import org.springframework.stereotype.Service;
import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.application.port.dto.LoginResponseDto;
import ecole_221.sn.ecole.modules.auth.application.port.usecase.LoginUseCase;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final JwtService jwtService;

    public LoginService(UserRepositoryPort userRepositoryPort, JwtService jwtService) {
        this.userRepositoryPort = userRepositoryPort;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponseDto login(LoginCommand command) {
        User user = userRepositoryPort.findByEmail(command.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.getPassword().equals(command.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        String token = jwtService.generateToken(user);

        return new LoginResponseDto(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }
}
