package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.infrastructure.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.application.port.dto.LoginResponseDto;
import ecole_221.sn.ecole.modules.auth.application.port.usecase.LoginUseCase;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;

@Service
public class LoginService implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UserRepositoryPort userRepositoryPort,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder) {
        this.userRepositoryPort = userRepositoryPort;
        this.jwtService         = jwtService;
        this.passwordEncoder    = passwordEncoder;
    }

    @Override
    public LoginResponseDto login(LoginCommand command) {
        log.info("Tentative de connexion pour : {}", command.getEmail());

        User user = userRepositoryPort.findByEmail(command.getEmail())
                .orElseThrow(() -> {
                    log.warn("Email non trouvé : {}", command.getEmail());
                    // On renvoie le même message que pour un mauvais MDP
                    // afin de ne pas révéler si un email existe ou pas (sécurité)
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Email ou mot de passe incorrect.");
                });

        if (!passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            log.warn("Mot de passe incorrect pour : {}", command.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Email ou mot de passe incorrect.");
        }

        log.info("Connexion réussie pour : {} (rôle={})", command.getEmail(), user.getRole());
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
