package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.shared.error.AppException;
import ecole_221.sn.ecole.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** Défauts F1 et F10 (15/11) à la connexion : école fermée refusée, meilleure appartenance retenue. */
class ConnexionEcoleFermeeTest {

    private static final String MDP = java.util.UUID.randomUUID().toString();
    private final BCryptPasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private final UserRepositoryPort comptes = Mockito.mock(UserRepositoryPort.class);
    private final OrganizationUserRepositoryPort appartenances = Mockito.mock(OrganizationUserRepositoryPort.class);
    private final OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
    private final JwtService jwt = Mockito.mock(JwtService.class);
    private final LoginService service = new LoginService(comptes, jwt, encodeur, appartenances, ecoles);

    private Organization ecole(String slug, OrganizationStatus statut) {
        Organization o = OrganizationFactory.create("École " + slug, OrganizationType.ECOLE, statut, slug,
                "SN", "Dakar", null, null, slug + "@exemple.sn", null, null, null, null, null);
        Mockito.when(ecoles.findById(o.getId())).thenReturn(Optional.of(o));
        return o;
    }

    private User compte(String email, RoleUtilisateur role, Organization... membreDe) {
        User u = UserFactory.create("Diop", "Awa", email, encodeur.encode(MDP), role);
        Mockito.when(comptes.findByEmail(email)).thenReturn(Optional.of(u));
        Mockito.when(appartenances.findByUserId(u.getId())).thenReturn(java.util.Arrays.stream(membreDe)
                .map(o -> OrganizationUserFactory.create(u.getId(), o.getId(), role.value())).toList());
        return u;
    }

    ConnexionEcoleFermeeTest() {
        Mockito.when(jwt.generateToken(any(User.class))).thenReturn("jwt");
        Mockito.when(ecoles.findById(anyString())).thenReturn(Optional.empty());
        Mockito.when(appartenances.findByUserId(anyString())).thenReturn(List.of());
    }

    @Test
    void ecoleSuspendueRefuseLaConnexionSansJeton() {
        compte("awa@suspendue.sn", RoleUtilisateur.ETUDIANT, ecole("ecole-suspendue", OrganizationStatus.SUSPENDED));
        assertThatThrownBy(() -> service.login(new LoginCommand("awa@suspendue.sn", MDP)))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.TENANT_SUSPENDED))
                .hasMessageContaining("suspendu");
        Mockito.verify(jwt, Mockito.never()).generateToken(any(User.class));
    }

    @Test
    void ecoleArchiveeRefuseeAvecMessageClair() {
        compte("awa@archivee.sn", RoleUtilisateur.ADMIN, ecole("ecole-archivee", OrganizationStatus.ARCHIVED));
        assertThatThrownBy(() -> service.login(new LoginCommand("awa@archivee.sn", MDP)))
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.TENANT_NOT_ACTIVE))
                .hasMessageContaining("Archivé");
    }

    @Test
    void appartenanceDUneEcoleActivePrefereeAUneEcoleFermee() {
        compte("awa@deux.sn", RoleUtilisateur.PROFESSEUR, ecole("ecole-fermee", OrganizationStatus.SUSPENDED),
                ecole("ecole-ouverte", OrganizationStatus.ACTIVE));
        assertThat(service.login(new LoginCommand("awa@deux.sn", MDP)).getTenantSlug()).isEqualTo("ecole-ouverte");
        compte("awa@trois.sn", RoleUtilisateur.PROFESSEUR, ecole("ecole-attente", OrganizationStatus.PENDING),
                ecole("ecole-active", OrganizationStatus.ACTIVE));
        assertThat(service.login(new LoginCommand("awa@trois.sn", MDP)).getTenantSlug()).isEqualTo("ecole-active");
    }

    @Test
    void superAdministrateurJamaisBloque() {
        compte("superadmin@exemple.sn", RoleUtilisateur.SUPER_ADMIN, ecole("ecole-suspendue-2", OrganizationStatus.SUSPENDED));
        assertThat(service.login(new LoginCommand("superadmin@exemple.sn", MDP)).getToken()).isEqualTo("jwt");
    }
}
