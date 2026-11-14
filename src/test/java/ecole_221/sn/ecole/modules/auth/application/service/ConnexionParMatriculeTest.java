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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * APP-TL-23 : un élève sans e-mail personnel se connecte avec son matricule ; seuls les comptes élèves le peuvent.
 */
class ConnexionParMatriculeTest {

    private static final String MOT_DE_PASSE = UUID.randomUUID().toString();
    private final PasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private UserRepositoryPort comptes;
    private LoginService service;

    @BeforeEach
    void preparer() {
        comptes = mock(UserRepositoryPort.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.generateToken(any(User.class))).thenReturn("jwt-de-test");
        OrganizationUserRepositoryPort appartenances = mock(OrganizationUserRepositoryPort.class);
        when(appartenances.findByUserId(any())).thenReturn(List.of());
        service = new LoginService(comptes, jwt, encodeur, appartenances, mock(OrganizationRepositoryPort.class));
        when(comptes.findByEmail(anyString())).thenReturn(Optional.empty());
        when(comptes.findByMatricule(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void eleveSansEmailSeConnecteAvecSonMatricule() {
        User eleve = UserFactory.create("Ndiaye", "Fatou", "etu-2026-abcdef@sans-email.invalid",
                encodeur.encode(MOT_DE_PASSE), RoleUtilisateur.fromString("ETUDIANT"));
        when(comptes.findByMatricule("ETU-2026-ABCDEF")).thenReturn(Optional.of(eleve));

        // saisi en minuscules avec des espaces (clavier de téléphone)
        var reponse = service.login(new LoginCommand(" etu-2026-abcdef ", MOT_DE_PASSE));

        assertThat(reponse.getToken()).isEqualTo("jwt-de-test");
    }

    @Test
    void unMatriculeNOuvrePasUnCompteQuiNEstPasEleve() {
        User admin = UserFactory.create("Sow", "Aliou", "admin@ecole.sn", encodeur.encode(MOT_DE_PASSE),
                RoleUtilisateur.fromString("ADMIN"));
        when(comptes.findByMatricule("EMP-2026-ABCDEF")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login(new LoginCommand("EMP-2026-ABCDEF", MOT_DE_PASSE)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void mauvaisMotDePasseRefuseAvecLeMemeMessage() {
        User eleve = UserFactory.create("Ndiaye", "Fatou", "etu-2026-abcdef@sans-email.invalid",
                encodeur.encode(MOT_DE_PASSE), RoleUtilisateur.fromString("ETUDIANT"));
        when(comptes.findByMatricule("ETU-2026-ABCDEF")).thenReturn(Optional.of(eleve));

        assertThatThrownBy(() -> service.login(new LoginCommand("ETU-2026-ABCDEF", "mauvais-mot-de-passe")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getReason()).isEqualTo("Email ou mot de passe incorrect.");
                });
    }

    @Test
    void uneAdresseEMailNEstJamaisCherchéeParmiLesMatricules() {
        assertThatThrownBy(() -> service.login(new LoginCommand("inconnu@ecole.sn", MOT_DE_PASSE)))
                .isInstanceOf(ResponseStatusException.class);
        verify(comptes, never()).findByMatricule(anyString());
    }
}
