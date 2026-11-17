package ecole_221.sn.ecole.modules.parent.infrastructure.web;

import ecole_221.sn.ecole.modules.parent.application.port.dto.ChildDto;
import ecole_221.sn.ecole.modules.parent.application.port.usecase.ParentUseCase;
import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour ParentController.
 * APP-SEC-11 : l'identité vient de l'utilisateur authentifié (cookie ou Bearer), plus du redécodage de l'en-tête.
 */
class ParentControllerTest {

    private ParentUseCase parentUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        parentUseCase = Mockito.mock(ParentUseCase.class);
        ParentController controller = new ParentController(parentUseCase,
                Mockito.mock(JwtService.class), Mockito.mock(UserRepositoryPort.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void connecter(RoleUtilisateur role) {
        User user = UserFactory.reconstructSimple("usr-parent-01", "Diallo", "Mamadou", "parent221@gmail.com",
                "ecole221", "PARENT-001", role, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private static ChildDto enfant() {
        return new ChildDto("child-1", "Diallo", "Aminata", "E221-2024-001",
                "Informatique", "L2", "L2-INFO-A", null);
    }

    @Test
    void enfantsDuParentConnecteSansEnTete() throws Exception {
        // authentification par cookie : aucun en-tête Authorization
        connecter(RoleUtilisateur.PARENT);
        Mockito.when(parentUseCase.getEnfantsByParentId("usr-parent-01")).thenReturn(List.of(enfant()));

        mockMvc.perform(get("/parent/enfants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("child-1"))
                .andExpect(jsonPath("$[0].nom").value("Diallo"));
        Mockito.verify(parentUseCase).getEnfantsByParentId("usr-parent-01");
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/parent/enfants")).andExpect(status().isUnauthorized());
        Mockito.verify(parentUseCase, Mockito.never()).getEnfantsByParentId(anyString());
    }

    @Test
    void autreRoleRefuse403() throws Exception {
        connecter(RoleUtilisateur.ETUDIANT);
        mockMvc.perform(get("/parent/enfants")).andExpect(status().isForbidden());
        Mockito.verify(parentUseCase, Mockito.never()).getEnfantsByParentId(anyString());
    }

    @Test
    void statsEnfantDuParent() throws Exception {
        connecter(RoleUtilisateur.PARENT);
        Mockito.when(parentUseCase.getEnfantsByParentId("usr-parent-01")).thenReturn(List.of(enfant()));

        mockMvc.perform(get("/parent/enfants/child-1/stats").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
        Mockito.verify(parentUseCase).getChildStats("child-1");
    }

    @Test
    void statsEnfantDunAutreFamilleRefusees() throws Exception {
        connecter(RoleUtilisateur.PARENT);
        Mockito.when(parentUseCase.getEnfantsByParentId("usr-parent-01")).thenReturn(List.of(enfant()));

        mockMvc.perform(get("/parent/enfants/child-autre/stats")).andExpect(status().is4xxClientError());
        Mockito.verify(parentUseCase, Mockito.never()).getChildStats("child-autre");
    }

    // APP-SEC-17 : professeurs proposés aux parents = professeurs actifs de l'école courante uniquement
    @Test
    void professeursDeLEcoleCouranteSeulement() throws Exception {
        var comptes = Mockito.mock(UserRepositoryPort.class);
        var appart = Mockito.mock(ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort.class);
        var ecole = ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory.create("A",
                ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType.ECOLE, "a@exemple.sn");
        User prof = UserFactory.reconstructSimple("prof-a", "Ba", "Fatou", "fatou@a.sn", "a", "P-1", RoleUtilisateur.PROFESSEUR, null);
        Mockito.when(appart.findByOrganizationIdAndRole(ecole.getId(), "PROFESSEUR")).thenReturn(List.of(
                ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory.create("prof-a", ecole.getId(), "PROFESSEUR")));
        Mockito.when(comptes.findById("prof-a")).thenReturn(java.util.Optional.of(prof));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ParentController(parentUseCase, Mockito.mock(JwtService.class), comptes, appart)).build();
        try {
            ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.setCurrentOrganization(ecole);
            mvc.perform(get("/parent/professeurs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].displayName").value("Fatou Ba"));
            Mockito.verify(comptes, Mockito.never()).findByRole(Mockito.any());
        } finally {
            ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.clear();
        }
    }

    @Test
    void aucunProfesseurInventeSiListeVide() throws Exception {
        mockMvc.perform(get("/parent/professeurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
