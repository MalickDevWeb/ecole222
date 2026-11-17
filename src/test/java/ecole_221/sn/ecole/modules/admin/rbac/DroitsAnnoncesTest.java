package ecole_221.sn.ecole.modules.admin.rbac;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/** Annonces (15/11) : envoi réservé au droit « communication.ecrire » ; ses propres notifications ouvertes à tous. */
class DroitsAnnoncesTest {

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest requete(String methode, String chemin) {
        MockHttpServletRequest r = new MockHttpServletRequest(methode, "/api" + chemin);
        r.setContextPath("/api");
        r.setServletPath(chemin);
        return r;
    }

    private static void connecter(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void routesRattacheesAuModuleCommunication() {
        assertThat(CataloguePermissions.permissionRequise("POST", "/v1/notifications/annonces")).isEqualTo("communication.ecrire");
        assertThat(CataloguePermissions.permissionRequise("GET", "/v1/notifications/annonces")).isEqualTo("communication.lire");
        assertThat(CataloguePermissions.permissionRequise("GET", "/v1/notifications/annonces/apercu")).isEqualTo("communication.lire");
        assertThat(CataloguePermissions.routeCommune("GET", "/v1/notifications/mes")).isTrue();
        assertThat(CataloguePermissions.routeCommune("GET", "/v1/notifications/mes/non-lues")).isTrue();
        assertThat(CataloguePermissions.routeCommune("PATCH", "/v1/notifications/abc/lue")).isTrue();
        assertThat(CataloguePermissions.routeCommune("PATCH", "/v1/notifications/abc/read")).isFalse();
        assertThat(CataloguePermissions.routeCommune("POST", "/v1/notifications/mes")).isFalse();
    }

    @Test
    void personnelSansDroitDEcriture403MaisSesNotificationsOuvertes() throws Exception {
        PermissionsEcoleService droits = Mockito.mock(PermissionsEcoleService.class);
        Mockito.when(droits.autorise(any(), eq("COMPTABLE"), any()))
                .thenAnswer(i -> Set.of("finances.lire", "finances.ecrire").contains(i.<String>getArgument(2)));
        Mockito.when(droits.autorise(any(), eq("SECRETAIRE"), any()))
                .thenAnswer(i -> Set.of("communication.lire", "communication.ecrire").contains(i.<String>getArgument(2)));
        PermissionsInterceptor intercepteur = new PermissionsInterceptor(droits);

        connecter("COMPTABLE");
        assertThatThrownBy(() -> intercepteur.preHandle(requete("POST", "/v1/notifications/annonces"), new MockHttpServletResponse(), null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));
        assertThatThrownBy(() -> intercepteur.preHandle(requete("GET", "/v1/notifications/annonces/apercu"), new MockHttpServletResponse(), null))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(intercepteur.preHandle(requete("GET", "/v1/notifications/mes"), new MockHttpServletResponse(), null)).isTrue();
        assertThat(intercepteur.preHandle(requete("PATCH", "/v1/notifications/n1/lue"), new MockHttpServletResponse(), null)).isTrue();

        connecter("SECRETAIRE");
        assertThat(intercepteur.preHandle(requete("POST", "/v1/notifications/annonces"), new MockHttpServletResponse(), null)).isTrue();
    }
}
