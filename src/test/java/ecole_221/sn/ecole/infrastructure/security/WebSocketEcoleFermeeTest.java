package ecole_221.sn.ecole.infrastructure.security;

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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;

/** Revue DevSecOps du 15/11 (F1) : CONNECT WebSocket — école fermée refusée, appartenance expirée ignorée. */
class WebSocketEcoleFermeeTest {

    private final OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
    private final OrganizationUserRepositoryPort appartenances = Mockito.mock(OrganizationUserRepositoryPort.class);
    private final WebSocketAuthInterceptor intercepteur = new WebSocketAuthInterceptor(null, Mockito.mock(UserRepositoryPort.class));

    WebSocketEcoleFermeeTest() throws Exception {
        for (String champ : new String[]{"ecoles", "appartenances"}) {
            Field f = WebSocketAuthInterceptor.class.getDeclaredField(champ);
            f.setAccessible(true);
            f.set(intercepteur, "ecoles".equals(champ) ? ecoles : appartenances);
        }
        Mockito.when(ecoles.findById(anyString())).thenReturn(Optional.empty());
        Mockito.when(ecoles.findBySlug(anyString())).thenReturn(Optional.empty());
    }

    private Organization ecole(String slug, OrganizationStatus statut) {
        Organization o = OrganizationFactory.create("École " + slug, OrganizationType.ECOLE, statut, slug,
                "SN", "Dakar", null, null, slug + "@exemple.sn", null, null, null, null, null);
        Mockito.when(ecoles.findById(o.getId())).thenReturn(Optional.of(o));
        Mockito.when(ecoles.findBySlug(slug)).thenReturn(Optional.of(o));
        return o;
    }

    private UsernamePasswordAuthenticationToken jeton(RoleUtilisateur role, List<ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser> liens, User u) {
        Mockito.when(appartenances.findByUserId(u.getId())).thenReturn(liens);
        return new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
    }

    @Test
    void ecoleSuspendueRefuseeAuConnect() {
        User u = UserFactory.create("Diop", "Awa", "awa@ex.sn", "x", RoleUtilisateur.ETUDIANT);
        Organization s = ecole("ecole-suspendue", OrganizationStatus.SUSPENDED);
        var j = jeton(RoleUtilisateur.ETUDIANT, List.of(OrganizationUserFactory.create(u.getId(), s.getId(), "ETUDIANT")), u);
        assertThatThrownBy(() -> intercepteur.ecoleDeLaConnexion(j, "ecole-suspendue"))
                .isInstanceOf(MessageDeliveryException.class).hasMessageContaining("suspendu");
        assertThatThrownBy(() -> intercepteur.ecoleDeLaConnexion(j, null)).isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void appartenanceExpireeIgnoreeEtEcoleActiveServie() {
        User u = UserFactory.create("Diop", "Awa", "awa2@ex.sn", "x", RoleUtilisateur.ETUDIANT);
        Organization a = ecole("ecole-active", OrganizationStatus.ACTIVE);
        Organization b = ecole("ecole-ancienne", OrganizationStatus.ACTIVE);
        var expiree = OrganizationUserFactory.create(u.getId(), b.getId(), "ETUDIANT", null, null, null,
                ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.MembershipStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        var j = jeton(RoleUtilisateur.ETUDIANT, List.of(OrganizationUserFactory.create(u.getId(), a.getId(), "ETUDIANT"), expiree), u);
        assertThat(intercepteur.ecoleDeLaConnexion(j, "ecole-ancienne")).as("appartenance expirée").isNull();
        assertThat(intercepteur.ecoleDeLaConnexion(j, null)).isEqualTo(a.getId());
        assertThat(intercepteur.ecoleDeLaConnexion(j, "ecole-active")).isEqualTo(a.getId());
    }
}
