package ecole_221.sn.ecole.modules.security.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.security.application.port.usecase.SecurityUseCase;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /vigil/scan : un badge n'est plus accepté sur son seul préfixe (SN-, badge-), aucune assiduité « 95% » ni
 * statut « À Jour » inventés, et le dernier scan est propre à chaque école (révision du 2026-11-09).
 */
class VigilScanSansDonneesInventeesTest {

    private SecurityUseCase securite;
    private StudentUseCase etudiants;
    private MockMvc mockMvc;
    private Organization ecoleA;
    private Organization ecoleB;

    @BeforeEach
    void setUp() {
        securite = Mockito.mock(SecurityUseCase.class);
        etudiants = Mockito.mock(StudentUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SecurityController(securite, etudiants, Mockito.mock(ActionLogger.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        ecoleA = OrganizationFactory.create("École A", OrganizationType.ECOLE, "a@exemple.sn");
        ecoleB = OrganizationFactory.create("École B", OrganizationType.ECOLE, "b@exemple.sn");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static Map<String, Object> resultat(boolean accorde, String nom, String frais, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("accessGranted", accorde);
        m.put("studentName", nom);
        m.put("statutFrais", frais);
        m.put("message", message);
        return m;
    }

    @Test
    void matriculeInconnuEstRefuseMalgreLePrefixeSN() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        Mockito.when(securite.processBadgeScan("SN-INVENTE")).thenReturn(
                resultat(false, "Inconnu", null, "Accès refusé : badge non reconnu."));

        mockMvc.perform(post("/vigil/scan").contentType(MediaType.APPLICATION_JSON).content("{\"badgeId\":\"SN-INVENTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("Refusé"))
                .andExpect(jsonPath("$.badgeOwner").value("Inconnu"))
                .andExpect(jsonPath("$.assiduite").value(nullValue()))
                .andExpect(jsonPath("$.statutFrais").value(nullValue()))
                .andExpect(jsonPath("$.zone").value(nullValue()));
        verify(securite).processBadgeScan("SN-INVENTE");
    }

    @Test
    void eleveReelDeLEcoleAvecStatutNonRenseigne() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        Mockito.when(securite.processBadgeScan("badge-ETU-1")).thenReturn(
                resultat(true, "Awa Fall", "Non renseigné", "Accès autorisé. Statut des frais non renseigné par l'administration."));

        mockMvc.perform(post("/vigil/scan").contentType(MediaType.APPLICATION_JSON).content("{\"badgeId\":\"badge-ETU-1\"}"))
                .andExpect(jsonPath("$.statut").value("Autorisé"))
                .andExpect(jsonPath("$.badgeOwner").value("Awa Fall"))
                .andExpect(jsonPath("$.statutFrais").value("Non renseigné"))
                .andExpect(jsonPath("$.assiduite").value(nullValue()));
    }

    @Test
    void jetonQrInvalideRefuseSansConsulterLesComptes() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        Mockito.when(etudiants.validateQrToken(anyString(), Mockito.anyLong(), anyString())).thenReturn(false);

        mockMvc.perform(post("/vigil/scan").contentType(MediaType.APPLICATION_JSON).content("{\"badgeId\":\"STU_SECURE:ETU-1:123:faux\"}"))
                .andExpect(jsonPath("$.statut").value("Refusé"))
                .andExpect(jsonPath("$.message").value("Token QR expiré ou invalide."));
        verify(securite, never()).processBadgeScan(anyString());
    }

    @Test
    void jetonQrValideResoluVersLeCompteReel() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        Mockito.when(etudiants.validateQrToken("ETU-1", 123L, "ok")).thenReturn(true);
        Mockito.when(securite.processBadgeScan("ETU-1")).thenReturn(resultat(true, "Awa Fall", "En Règle", "Accès autorisé."));

        mockMvc.perform(post("/vigil/scan").contentType(MediaType.APPLICATION_JSON).content("{\"badgeId\":\"STU_SECURE:ETU-1:123:ok\"}"))
                .andExpect(jsonPath("$.statut").value("Autorisé"))
                .andExpect(jsonPath("$.statutFrais").value("En Règle"));
    }

    @Test
    void dernierScanCloisonneParEcole() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        Mockito.when(securite.processBadgeScan("ETU-1")).thenReturn(resultat(true, "Awa Fall", "En Règle", "Accès autorisé."));
        mockMvc.perform(post("/vigil/scan").contentType(MediaType.APPLICATION_JSON).content("{\"badgeId\":\"ETU-1\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/vigil/last-scan")).andExpect(jsonPath("$.badgeOwner").value("Awa Fall"));

        TenantContext.setCurrentOrganization(ecoleB);
        mockMvc.perform(get("/vigil/last-scan"))
                .andExpect(jsonPath("$.message").value("Aucun scan récent"))
                .andExpect(jsonPath("$.badgeOwner").doesNotExist());
    }
}
