package ecole_221.sn.ecole.modules.dashboard.infrastructure.web;

import ecole_221.sn.ecole.modules.dashboard.application.port.usecase.DashboardUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardControllerTest {

    private DashboardUseCase dashboardUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        dashboardUseCase = Mockito.mock(DashboardUseCase.class);
        
        DashboardController controller = new DashboardController(dashboardUseCase);
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void testGetAllKpis() throws Exception {
        mockMvc.perform(get("/dashboard/kpis")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Mockito.verify(dashboardUseCase, Mockito.times(1)).getAllKpis();
    }

    @Test
    void testGetAllAlerts() throws Exception {
        mockMvc.perform(get("/dashboard/alerts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Mockito.verify(dashboardUseCase, Mockito.times(1)).getAllAlerts();
    }

    @Test
    void testGetRecentActivities() throws Exception {
        mockMvc.perform(get("/dashboard/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Mockito.verify(dashboardUseCase, Mockito.times(1)).getRecentActivities(anyInt());
    }

    @Test
    void testGetCompleteDashboard() throws Exception {
        Map<String, Object> mockDashboard = new HashMap<>();
        mockDashboard.put("kpis", Arrays.asList());
        mockDashboard.put("alerts", Arrays.asList());
        mockDashboard.put("activities", Arrays.asList());
        
        Mockito.when(dashboardUseCase.getCompleteDashboard()).thenReturn(mockDashboard);

        mockMvc.perform(get("/dashboard/complete")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(dashboardUseCase, Mockito.times(1)).getCompleteDashboard();
    }
}