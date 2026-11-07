package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** SEC-003 : un utilisateur ne franchit jamais la frontière de son école (hors rôles plateforme). */
class TenantMembershipInterceptorTest {

    private static final String SCHOOL_A = "org-a";
    private final OrganizationUserRepositoryPort memberships = mock(OrganizationUserRepositoryPort.class);
    private final TenantMembershipInterceptor interceptor = new TenantMembershipInterceptor(memberships, false);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/finance/expenses");

    @BeforeEach
    void schoolA() {
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(SCHOOL_A);
        TenantContext.setCurrentOrganization(org);
        TenantContext.setPlatformMode(false);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void membre_de_l_ecole_autorise() {
        User u = login(RoleUtilisateur.ADMIN);
        OrganizationUser m = membership(SCHOOL_A);
        when(memberships.findByUserId(u.getId())).thenReturn(List.of(m));
        assertThat(pass()).isTrue();
    }

    @Test
    void membre_d_une_autre_ecole_refuse_403() {
        User u = login(RoleUtilisateur.ADMIN);
        OrganizationUser m = membership("org-b");
        when(memberships.findByUserId(u.getId())).thenReturn(List.of(m));
        assertThatThrownBy(this::pass).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void super_admin_autorise_sans_appartenance() {
        login(RoleUtilisateur.SUPER_ADMIN);
        assertThat(pass()).isTrue();
        verifyNoInteractions(memberships);
    }

    @Test
    void transition_utilisateur_sans_aucune_appartenance_autorise() {
        User u = login(RoleUtilisateur.ETUDIANT);
        when(memberships.findByUserId(u.getId())).thenReturn(List.of());
        assertThat(pass()).isTrue();
    }

    @Test
    void mode_strict_utilisateur_sans_appartenance_refuse_403() {
        User u = login(RoleUtilisateur.ETUDIANT);
        when(memberships.findByUserId(u.getId())).thenReturn(List.of());
        TenantMembershipInterceptor strict = new TenantMembershipInterceptor(memberships, true);
        assertThatThrownBy(() -> strict.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void requete_anonyme_laissee_a_spring_security() {
        assertThat(pass()).isTrue();
        verifyNoInteractions(memberships);
    }

    private boolean pass() {
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    private User login(RoleUtilisateur role) {
        User u = UserFactory.create("Test", "User", "u@test.sn", "x", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
        return u;
    }

    private OrganizationUser membership(String orgId) {
        OrganizationUser m = mock(OrganizationUser.class);
        when(m.getOrganizationId()).thenReturn(orgId);
        when(m.isActive()).thenReturn(true);
        when(m.isExpired()).thenReturn(false);
        return m;
    }
}
