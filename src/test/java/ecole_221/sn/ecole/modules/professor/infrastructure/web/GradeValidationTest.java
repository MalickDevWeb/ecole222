package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** OBS-02 (Agent 05) : une note mal nommée ou hors 0–20 est refusée au lieu d'enregistrer 0. */
class GradeValidationTest {

    private ProfessorUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = Mockito.mock(ProfessorUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProfessorController(useCase, Mockito.mock(ActionLogger.class))).build();
    }

    private void poster(String json, int attendu) throws Exception {
        mvc.perform(post("/professor/courses/c1/grades/e1").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(attendu));
    }

    @Test
    void clesInconnuesRefusees() throws Exception {
        poster("{\"noteCc\":16.5,\"noteExam\":15.0}", 400);
        Mockito.verify(useCase, Mockito.never()).saveGrade(anyString(), anyString(), any(), any());
    }

    @Test
    void horsIntervalleRefuse() throws Exception {
        poster("{\"cc\":21}", 400);
        poster("{\"examen\":-1}", 400);
        Mockito.verify(useCase, Mockito.never()).saveGrade(anyString(), anyString(), any(), any());
    }

    @Test
    void noteValideEnregistree() throws Exception {
        poster("{\"cc\":16.5,\"examen\":15.0}", 200);
        Mockito.verify(useCase).saveGrade("e1", "c1", 16.5, 15.0);
    }
}
