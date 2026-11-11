package ecole_221.sn.ecole.modules.superadmin.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Test des routes super-admin du 2026-11-10 : le super-admin a pu supprimer SON PROPRE compte. */
class SuppressionSuperAdminProtegeeTest {

    private final UserRepositoryPort comptes = Mockito.mock(UserRepositoryPort.class);
    private final DeleteGlobalUserService service = new DeleteGlobalUserService(comptes);

    private static User compte(String id, RoleUtilisateur role) {
        return UserFactory.reconstructSimple(id, "Nom", "Prenom", id + "@ex.sn", null, "M-" + id, role, null);
    }

    private static void connecter(User u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    @AfterEach
    void fin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onNePeutPasSupprimerSonPropreCompte() {
        User moi = compte("sa-1", RoleUtilisateur.SUPER_ADMIN);
        connecter(moi);
        Mockito.when(comptes.findById("sa-1")).thenReturn(Optional.of(moi));
        var e = assertThrows(ResponseStatusException.class, () -> service.execute("sa-1"));
        assertEquals(409, e.getStatusCode().value());
        Mockito.verify(comptes, Mockito.never()).deleteById(Mockito.anyString());
    }

    @Test
    void onNePeutPasSupprimerLeDernierSuperAdmin() {
        connecter(compte("pa-1", RoleUtilisateur.PLATFORM_ADMIN));
        User dernier = compte("sa-2", RoleUtilisateur.SUPER_ADMIN);
        Mockito.when(comptes.findById("sa-2")).thenReturn(Optional.of(dernier));
        Mockito.when(comptes.findByRole(RoleUtilisateur.SUPER_ADMIN)).thenReturn(List.of(dernier));
        var e = assertThrows(ResponseStatusException.class, () -> service.execute("sa-2"));
        assertEquals(409, e.getStatusCode().value());
    }

    @Test
    void unAutreAdministrateurPlateformePeutEtreSupprime() {
        connecter(compte("sa-1", RoleUtilisateur.SUPER_ADMIN));
        Mockito.when(comptes.findById("pa-9")).thenReturn(Optional.of(compte("pa-9", RoleUtilisateur.PLATFORM_ADMIN)));
        service.execute("pa-9");
        Mockito.verify(comptes).deleteById("pa-9");
    }
}
