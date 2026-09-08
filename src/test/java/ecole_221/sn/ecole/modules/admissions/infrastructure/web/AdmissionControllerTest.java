package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.application.port.usecase.AdmissionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdmissionControllerTest {

    private AdmissionUseCase admissionUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        admissionUseCase = Mockito.mock(AdmissionUseCase.class);
        
        AdmissionController controller = new AdmissionController(admissionUseCase);
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void testGetAllCandidates() throws Exception {
        Mockito.when(admissionUseCase.getAllCandidates()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/admissions/candidates")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Mockito.verify(admissionUseCase, Mockito.times(1)).getAllCandidates();
    }

    @Test
    void testGetAdmissionSettings() throws Exception {
        Mockito.when(admissionUseCase.getAdmissionSettings()).thenReturn(null);

        mockMvc.perform(get("/admissions/settings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        Mockito.verify(admissionUseCase, Mockito.times(1)).getAdmissionSettings();
    }

    @Test
    void testGetKanbanData() throws Exception {
        Mockito.when(admissionUseCase.getKanbanData()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/admissions/kanban")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Mockito.verify(admissionUseCase, Mockito.times(1)).getKanbanData();
    }

    @Test
    void testGetCandidateStatistics() throws Exception {
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("total", 100);
        mockStats.put("accepted", 50);
        
        Mockito.when(admissionUseCase.getCandidateStatistics()).thenReturn(mockStats);

        mockMvc.perform(get("/admissions/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(admissionUseCase, Mockito.times(1)).getCandidateStatistics();
    }

    @Test
    void testProcessAdmissions() throws Exception {
        Mockito.doNothing().when(admissionUseCase).processAdmissions();

        mockMvc.perform(post("/admissions/automation/process")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Mockito.verify(admissionUseCase, Mockito.times(1)).processAdmissions();
    }
}