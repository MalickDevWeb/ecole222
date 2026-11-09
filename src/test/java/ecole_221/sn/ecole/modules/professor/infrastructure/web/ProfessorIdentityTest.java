package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** APP-BE-16 : l'identité du professeur vient de l'authentification, jamais d'un identifiant codé en dur ni d'un paramètre. */
class ProfessorIdentityTest {

    private ProfessorUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = Mockito.mock(ProfessorUseCase.class);
        Mockito.when(useCase.getSchedule(anyString())).thenReturn(List.of());
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProfExtraController(useCase),
                new ProfessorController(useCase, Mockito.mock(ActionLogger.class))).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void connecter(String id) {
        User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void emploiDuTempsDuProfesseurConnecte() throws Exception {
        connecter("prof-42");
        mockMvc.perform(get("/professor/schedule")).andExpect(status().isOk());
        verify(useCase).getSchedule("prof-42");
        verify(useCase, never()).getSchedule("usr-002");
    }

    @Test
    void planningIgnoreLeParametreProfId() throws Exception {
        connecter("prof-42");
        mockMvc.perform(get("/professor/planning").param("profId", "prof-autre")).andExpect(status().isOk());
        verify(useCase).getSchedule("prof-42");
        verify(useCase, never()).getSchedule("prof-autre");
    }

    @Test
    void sansAuthentificationRefuse() throws Exception {
        mockMvc.perform(get("/professor/schedule")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/professor/planning")).andExpect(status().isUnauthorized());
        verify(useCase, never()).getSchedule(anyString());
    }
}
