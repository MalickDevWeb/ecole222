package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.application.OrganizationApplicationService;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Défaut F7 (15/11) : fiche publique d'une école selon son statut. */
class FichePubliqueStatutTest {

    private MockMvc mvc(String slug, OrganizationStatus statut) {
        OrganizationApplicationService service = Mockito.mock(OrganizationApplicationService.class);
        Mockito.when(service.getOrganizationBySlug(anyString())).thenReturn(Optional.empty());
        Mockito.when(service.getOrganizationBySlug(slug)).thenReturn(Optional.of(OrganizationFactory.create("Lycée de Thiès",
                OrganizationType.ECOLE, statut, slug, "SN", "Thiès", null, "+221 77 000 00 00", "contact@lycee.sn", null,
                "https://img.exemple/logo.png", null, null, Map.of("primaryColor", "#123456"))));
        return MockMvcBuilders.standaloneSetup(new PublicOnboardingController(service)).build();
    }

    @Test
    void ecoleActiveOuEnAttenteServieAvecSonStatut() throws Exception {
        mvc("lycee-de-thies", OrganizationStatus.ACTIVE).perform(get("/public/tenants/lycee-de-thies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIVE"))
                .andExpect(jsonPath("$.name").value("Lycée de Thiès"))
                .andExpect(jsonPath("$.branding.primaryColor").value("#123456"))
                .andExpect(jsonPath("$.branding.logoUrl").value("https://img.exemple/logo.png"));
        mvc("lycee-de-thies", OrganizationStatus.PENDING).perform(get("/public/tenants/lycee-de-thies"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("PENDING"));
    }

    @Test
    void ecoleSuspendueArchiveeOuRejetee404() throws Exception {
        for (OrganizationStatus s : new OrganizationStatus[]{OrganizationStatus.SUSPENDED, OrganizationStatus.ARCHIVED,
                OrganizationStatus.REJECTED}) {
            mvc("lycee-de-thies", s).perform(get("/public/tenants/lycee-de-thies")).andExpect(status().isNotFound());
        }
        mvc("ecole-221", OrganizationStatus.SUSPENDED).perform(get("/public/tenants/ecole221")).andExpect(status().isNotFound());
    }
}
