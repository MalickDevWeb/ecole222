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

import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import java.util.List;

@Service
public class LoginService implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationUserRepositoryPort organizationUserRepositoryPort;
    private final OrganizationRepositoryPort organizationRepositoryPort;

    public LoginService(UserRepositoryPort userRepositoryPort,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder,
                        OrganizationUserRepositoryPort organizationUserRepositoryPort,
                        OrganizationRepositoryPort organizationRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.jwtService         = jwtService;
        this.passwordEncoder    = passwordEncoder;
        this.organizationUserRepositoryPort = organizationUserRepositoryPort;
        this.organizationRepositoryPort = organizationRepositoryPort;
    }

    @Override
    public LoginResponseDto login(LoginCommand command) {
        log.info("Tentative de connexion pour : {}", command.getEmail());
        // corps vide / champs absents : 400 clair (avant : NullPointerException → 500)
        if (command.getEmail() == null || command.getEmail().isBlank()
                || command.getPassword() == null || command.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "L'e-mail et le mot de passe sont obligatoires.");
        }

        // SEC-002 : aucun compte n'est créé à la connexion (les comptes de démo sont créés au
        // démarrage par SuperAdminInitializer, hors profil prod) et aucun mot de passe universel.
        User user = userRepositoryPort.findByEmail(command.getEmail())
                .orElseThrow(() -> {
                    log.warn("Email non trouvé : {}", command.getEmail());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Email ou mot de passe incorrect.");
                });

        boolean passwordValid = command.getPassword() != null
                && passwordEncoder.matches(command.getPassword(), user.getPassword());

        if (!passwordValid) {
            log.warn("Mot de passe incorrect pour : {}", command.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Email ou mot de passe incorrect.");
        }

        log.info("Connexion réussie pour : {} (rôle={})", command.getEmail(), user.getRole());
        String token = jwtService.generateToken(user);
        String tenantSlug = resolveTenantSlug(user, command.getEmail());

        return new LoginResponseDto(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRole(),
                token,
                tenantSlug
        );
    }

    private String resolveTenantSlug(User user, String inputEmail) {
        String email = (inputEmail != null ? inputEmail : user.getEmail()).trim().toLowerCase();

        // 1. Appartenance réelle (organization_users) — prioritaire (SEC-003)
        try {
            List<OrganizationUser> memberships = organizationUserRepositoryPort.findByUserId(user.getId());
            if (memberships != null && !memberships.isEmpty()) {
                String orgId = memberships.get(0).getOrganizationId();
                if (orgId != null) {
                    var orgOpt = organizationRepositoryPort.findById(orgId);
                    if (orgOpt.isPresent()) {
                        return orgOpt.get().getSlug();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de résoudre l'organisation pour l'utilisateur {} : {}", user.getId(), e.getMessage());
        }

        // 2. À défaut : déduction depuis l'email <role>@<slug>.sn (comptes historiques sans appartenance)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^.+@([a-zA-Z0-9_-]+)\\.sn$");
        java.util.regex.Matcher matcher = pattern.matcher(email);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        // 3. Fallback École 221
        return "ecole221";
    }
}
