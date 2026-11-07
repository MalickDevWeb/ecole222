package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Non-régression SEC-002 : aucune porte dérobée dans l'authentification.
 * Vrai encodeur BCrypt (pas de mock) : seul le vrai mot de passe ouvre une session.
 */
class LoginServiceSecurityTest {

    private static final String REAL_PASSWORD = java.util.UUID.randomUUID().toString();   // aucun littéral

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UserRepositoryPort users;
    private JwtService jwt;
    private LoginService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepositoryPort.class);
        jwt = mock(JwtService.class);
        OrganizationUserRepositoryPort memberships = mock(OrganizationUserRepositoryPort.class);
        when(memberships.findByUserId(any())).thenReturn(List.of());
        service = new LoginService(users, jwt, encoder, memberships, mock(OrganizationRepositoryPort.class));

        User superAdmin = UserFactory.create("Super", "Admin", "superadmin@ecole221.sn",
                encoder.encode(REAL_PASSWORD), RoleUtilisateur.SUPER_ADMIN);
        when(users.findByEmail("superadmin@ecole221.sn")).thenReturn(Optional.of(superAdmin));
        when(users.findByEmail("admin@inventee-xyz.sn")).thenReturn(Optional.empty());
        when(jwt.generateToken(any(User.class))).thenReturn("jwt-de-test");
    }

    @Test
    void le_vrai_mot_de_passe_ouvre_une_session() {
        var response = service.login(new LoginCommand("superadmin@ecole221.sn", REAL_PASSWORD));
        assertThat(response.getToken()).isEqualTo("jwt-de-test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ecole221", "nimporte-quoi-221!", "SuperAdmin@221!", "", "mauvais"})
    void aucun_mot_de_passe_universel_ou_faux_n_est_accepte(String password) {
        assertUnauthorized(() -> service.login(new LoginCommand("superadmin@ecole221.sn", password)));
        verify(jwt, never()).generateToken(any(User.class));
    }

    @Test
    void un_mot_de_passe_null_est_refuse() {
        assertUnauthorized(() -> service.login(new LoginCommand("superadmin@ecole221.sn", null)));
    }

    @Test
    void un_email_inconnu_ne_cree_aucun_compte() {
        assertUnauthorized(() -> service.login(new LoginCommand("admin@inventee-xyz.sn", "ecole221")));
        verify(users, never()).save(any());
        verify(jwt, never()).generateToken(any(User.class));
    }

    private void assertUnauthorized(Runnable login) {
        assertThatThrownBy(login::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
