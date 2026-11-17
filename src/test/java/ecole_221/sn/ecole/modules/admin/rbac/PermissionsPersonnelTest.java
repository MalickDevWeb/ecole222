package ecole_221.sn.ecole.modules.admin.rbac;

import ecole_221.sn.ecole.modules.admin.application.service.AdminStaffService;
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

/** Matrice des droits du personnel appliquée par le serveur. */
class PermissionsPersonnelTest {

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void routesReellesRattacheesAuxModules() {
        assertThat(CataloguePermissions.permissionRequise("GET", "/admin/students")).isEqualTo("eleves.lire");
        assertThat(CataloguePermissions.permissionRequise("POST", "/admin/students/import")).isEqualTo("eleves.ecrire");
        assertThat(CataloguePermissions.permissionRequise("PATCH", "/finance/fees/1")).isEqualTo("finances.ecrire");
        assertThat(CataloguePermissions.permissionRequise("GET", "/v1/admissions/campagnes")).isEqualTo("admissions.lire");
        assertThat(CataloguePermissions.permissionRequise("POST", "/admin/transferts/t1/accepter")).isEqualTo("transferts.ecrire");
        // préfixe le plus long : /admin/studentsX n'est pas /admin/students
        assertThat(CataloguePermissions.permissionRequise("GET", "/admin/studentsX")).isNull();
        // la matrice elle-même n'est rattachée à aucun module : jamais délégable
        assertThat(CataloguePermissions.permissionRequise("PUT", "/admin/rbac/SECRETAIRE")).isNull();
    }

    @Test
    void personnelLimiteASesDroitsAdministrationIllimitee() throws Exception {
        PermissionsEcoleService droits = Mockito.mock(PermissionsEcoleService.class);
        Mockito.when(droits.autorise(any(), eq("COMPTABLE"), any()))
                .thenAnswer(i -> Set.of("finances.lire", "finances.ecrire").contains(i.<String>getArgument(2)));
        PermissionsInterceptor intercepteur = new PermissionsInterceptor(droits);

        connecter("COMPTABLE");
        assertThat(intercepteur.preHandle(requete("GET", "/finance/transactions"), new MockHttpServletResponse(), null)).isTrue();
        assertThatThrownBy(() -> intercepteur.preHandle(requete("GET", "/admin/staff"), new MockHttpServletResponse(), null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("personnel.lire");
        assertThatThrownBy(() -> intercepteur.preHandle(requete("PUT", "/admin/rbac/COMPTABLE"), new MockHttpServletResponse(), null))
                .isInstanceOf(ResponseStatusException.class);
        // routes communes (affichage de l'espace) et son compte
        assertThat(intercepteur.preHandle(requete("GET", "/admin/rbac/moi"), new MockHttpServletResponse(), null)).isTrue();
        assertThat(intercepteur.preHandle(requete("GET", "/planning/salles"), new MockHttpServletResponse(), null)).isTrue();
        assertThatThrownBy(() -> intercepteur.preHandle(requete("POST", "/planning/salles"), new MockHttpServletResponse(), null))
                .isInstanceOf(ResponseStatusException.class);

        connecter("ADMIN");   // l'administration n'est pas concernée par la matrice
        assertThat(intercepteur.preHandle(requete("GET", "/admin/staff"), new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void droitsParDefautPuisConfiguresParLEcole() {
        PermissionRoleJpaRepository repo = Mockito.mock(PermissionRoleJpaRepository.class);
        PermissionsEcoleService droits = new PermissionsEcoleService(repo);
        Mockito.when(repo.findByOrganizationIdAndRole("A", "COMPTABLE")).thenReturn(List.of());
        assertThat(droits.droits("A", "COMPTABLE")).contains("finances.ecrire");
        Mockito.when(repo.findByOrganizationIdAndRole("A", "COMPTABLE")).thenReturn(List.of(
                new PermissionRoleJpaEntity("A", "COMPTABLE", "*", "adm"),
                new PermissionRoleJpaEntity("A", "COMPTABLE", "finances.lire", "adm")));
        assertThat(droits.droits("A", "COMPTABLE")).containsExactly("finances.lire");
        assertThat(droits.droits("A", "ADMIN")).contains("personnel.ecrire", "transferts.ecrire");
        assertThat(droits.droits("A", "ETUDIANT")).isEmpty();
        assertThatThrownBy(() -> droits.enregistrer("A", "COMPTABLE", List.of("finances.tout"), "adm"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("inconnue");
        assertThatThrownBy(() -> droits.enregistrer("A", "ADMIN", List.of(), "adm")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void personnelNeCreeNiNePromeutUnAdministrateur() {
        connecter("RH");
        assertThatThrownBy(() -> AdminStaffService.verifierGestionAutorisee("ADMIN"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("administration");
        assertThatThrownBy(() -> AdminStaffService.verifierGestionAutorisee("DIRECTEUR")).isInstanceOf(ResponseStatusException.class);
        AdminStaffService.verifierGestionAutorisee("SECRETAIRE");   // autorisé
        connecter("ADMIN");
        AdminStaffService.verifierGestionAutorisee("ADMIN");        // l'administration, oui
    }

    private static MockHttpServletRequest requete(String methode, String chemin) {
        MockHttpServletRequest r = new MockHttpServletRequest(methode, "/api" + chemin);
        r.setContextPath("/api");
        r.setServletPath(chemin);
        return r;
    }
}
