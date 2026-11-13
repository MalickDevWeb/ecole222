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
    void empreinte_d_un_cout_inferieur_recalculee_au_cout_actuel() {
        // BR-03 (2026-11-13) : coût de l'application = 12 ; une empreinte au coût 4 est recalculée à la connexion
        PasswordEncoder actuel = new BCryptPasswordEncoder(5);
        OrganizationUserRepositoryPort memberships = mock(OrganizationUserRepositoryPort.class);
        when(memberships.findByUserId(any())).thenReturn(List.of());
        LoginService avecCoutSuperieur = new LoginService(users, jwt, actuel, memberships, mock(OrganizationRepositoryPort.class));
        avecCoutSuperieur.login(new LoginCommand("superadmin@ecole221.sn", REAL_PASSWORD));
        var capture = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).save(capture.capture());
        assertThat(capture.getValue().getPassword()).startsWith("$2a$05$");
        assertThat(actuel.matches(REAL_PASSWORD, capture.getValue().getPassword())).isTrue();
    }

    @Test
    void cout_de_l_application_est_12() {
        assertThat(ecole_221.sn.ecole.infrastructure.config.SecurityConfig.COUT_BCRYPT).isEqualTo(12);
        assertThat(new ecole_221.sn.ecole.infrastructure.config.SecurityConfig(null, null).passwordEncoder().encode("x")).startsWith("$2a$12$");
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
    void email_avec_majuscules_et_espaces_retrouve() {
        var response = service.login(new LoginCommand("  SuperAdmin@Ecole221.SN ", REAL_PASSWORD));
        assertThat(response.getToken()).isEqualTo("jwt-de-test");
    }

    @Test
    void cinq_echecs_freinent_le_compte_meme_avec_le_bon_mot_de_passe() {
        for (int i = 0; i < LoginService.ECHECS_MAX; i++) {
            assertUnauthorized(() -> service.login(new LoginCommand("superadmin@ecole221.sn", "faux")));
        }
        assertThatThrownBy(() -> service.login(new LoginCommand("SUPERADMIN@ecole221.sn", REAL_PASSWORD)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS))
                .hasMessageContaining("Réessayez dans 15 minutes");
        // fin de la fenêtre : de nouveau possible
        service.horloge = java.time.Clock.offset(java.time.Clock.systemUTC(), java.time.Duration.ofMinutes(16));
        assertThat(service.login(new LoginCommand("superadmin@ecole221.sn", REAL_PASSWORD)).getToken()).isEqualTo("jwt-de-test");
    }

    @Test
    void une_connexion_reussie_remet_le_compteur_a_zero() {
        for (int i = 0; i < LoginService.ECHECS_MAX - 1; i++) {
            assertUnauthorized(() -> service.login(new LoginCommand("superadmin@ecole221.sn", "faux")));
        }
        service.login(new LoginCommand("superadmin@ecole221.sn", REAL_PASSWORD));
        assertUnauthorized(() -> service.login(new LoginCommand("superadmin@ecole221.sn", "faux")));
        assertThat(service.login(new LoginCommand("superadmin@ecole221.sn", REAL_PASSWORD)).getToken()).isEqualTo("jwt-de-test");
    }

    @Test
    void un_email_inconnu_est_freine_comme_un_vrai_compte() {
        for (int i = 0; i < LoginService.ECHECS_MAX; i++) {
            assertUnauthorized(() -> service.login(new LoginCommand("admin@inventee-xyz.sn", "x")));
        }
        assertThatThrownBy(() -> service.login(new LoginCommand("admin@inventee-xyz.sn", "x")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
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
