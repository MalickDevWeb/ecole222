package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.partage.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfessorControllerTest {

    private ProfessorUseCase professorUseCase;
    private ActionLogger actionLogger;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        professorUseCase = Mockito.mock(ProfessorUseCase.class);
        actionLogger = Mockito.mock(ActionLogger.class);
        
        ProfessorController controller = new ProfessorController(professorUseCase, actionLogger);
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void testGetCourseStudents() throws Exception {
        mockMvc.perform(get("/professor/courses/course-001/students")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetCourseGrades() throws Exception {
        mockMvc.perform(get("/professor/courses/course-001/grades")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testSaveGrade() throws Exception {
        Map<String, Object> mockGrade = new HashMap<>();
        mockGrade.put("studentId", "usr-001");
        mockGrade.put("courseId", "course-001");
        mockGrade.put("cc", 15.0);
        mockGrade.put("examen", 16.0);
        
        Mockito.doNothing().when(professorUseCase).saveGrade(anyString(), anyString(), anyDouble(), anyDouble());
        Mockito.when(professorUseCase.getGrade(anyString(), anyString())).thenReturn(mockGrade);
        Mockito.doNothing().when(actionLogger).logProfessorAction(anyString(), anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/professor/courses/course-001/grades/usr-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cc\":15.0,\"examen\":16.0}"))
                .andExpect(status().isOk());

        Mockito.verify(professorUseCase, Mockito.times(1)).saveGrade(anyString(), anyString(), anyDouble(), anyDouble());
    }

    @Test
    void testCreateHomework() throws Exception {
        Mockito.doNothing().when(professorUseCase).saveHomework(anyString(), anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/professor/courses/course-001/homeworks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Devoir 1\",\"desc\":\"Description\",\"prio\":\"HIGH\",\"deadlineStr\":\"2024-12-31\"}"))
                .andExpect(status().isOk());

        Mockito.verify(professorUseCase, Mockito.times(1)).saveHomework(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}