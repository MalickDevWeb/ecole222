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
}
