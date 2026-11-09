package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** APP-BE-17 : profil 360 sans données inventées (santé, notes, admission, connexions). */
class Profile360SansDonneesInventeesTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminStudentProfileController(
            Mockito.mock(ecole_221.sn.ecole.modules.ai.application.port.usecase.AIUseCase.class))).build();

    @Test
    void profilParDefautVideEtExplicite() throws Exception {
        mvc.perform(get("/admin/students/e42/profile360"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donneesStatus").value("non_disponible"))
                .andExpect(jsonPath("$.ueList").isEmpty())
                .andExpect(jsonPath("$.connectionLogs").isEmpty())
                .andExpect(content().string(not(containsString("O+"))))
                .andExpect(content().string(not(containsString("Seydou Nourou"))))
                .andExpect(content().string(not(containsString("MAT-2023"))));
    }

    @Test
    void groupeSanguinSaisiEstEnregistre() throws Exception {
        mvc.perform(post("/admin/students/e43/profile360").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bloodGroup\":\"A+\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extra.sante").value("A+"));
    }

    @Test
    void donneesDUneEcoleInvisiblesDepuisUneAutre() throws Exception {
        var ecoleA = ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory.create("A",
                ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType.ECOLE, "a@exemple.sn");
        var ecoleB = ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory.create("B",
                ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType.ECOLE, "b@exemple.sn");
        try {
            ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.setCurrentOrganization(ecoleA);
            mvc.perform(post("/admin/students/e44/profile360").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"bloodGroup\":\"B-\"}")).andExpect(status().isOk());
            ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.setCurrentOrganization(ecoleB);
            mvc.perform(get("/admin/students/e44/profile360"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("B-"))));
        } finally {
            ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.clear();
        }
    }
}
