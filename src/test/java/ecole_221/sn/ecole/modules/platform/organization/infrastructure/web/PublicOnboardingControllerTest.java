package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.application.OrganizationApplicationService;
import ecole_221.sn.ecole.modules.platform.organization.application.dto.SchoolRegistrationRequest;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-02 [FEAT-001-SC-03] : l'inscription d'un établissement retourne 201 Created (OBS-01, Agent 05). */
class PublicOnboardingControllerTest {

    @Test
    void inscriptionEcoleRetourne201() throws Exception {
        OrganizationApplicationService service = Mockito.mock(OrganizationApplicationService.class);
        Mockito.when(service.registerSchool(any(SchoolRegistrationRequest.class))).thenReturn(Mockito.mock(Organization.class));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PublicOnboardingController(service)).build();

        mvc.perform(post("/register-school").contentType(MediaType.APPLICATION_JSON).content("{\"schoolName\":\"École test\"}"))
                .andExpect(status().isCreated());
    }
}
