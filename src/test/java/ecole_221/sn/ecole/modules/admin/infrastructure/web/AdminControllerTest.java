package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.port.usecase.AdminUseCase;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerTest {

    private AdminUseCase adminUseCase;
    private ActionLogger actionLogger;
    private ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository userJpaRepository;
    private ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository classeJpaRepository;
    private ecole_221.sn.ecole.modules.planning.infrastructure.persistence.RoomJpaRepository roomJpaRepository;
    private ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository scheduleSessionJpaRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminUseCase = Mockito.mock(AdminUseCase.class);
        actionLogger = Mockito.mock(ActionLogger.class);
        userJpaRepository = Mockito.mock(ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository.class);
        classeJpaRepository = Mockito.mock(ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository.class);
        roomJpaRepository = Mockito.mock(ecole_221.sn.ecole.modules.planning.infrastructure.persistence.RoomJpaRepository.class);
        scheduleSessionJpaRepository = Mockito.mock(ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository.class);
        
        AdminController controller = new AdminController(
            adminUseCase, 
            actionLogger,
            userJpaRepository,
            classeJpaRepository,
            roomJpaRepository,
            scheduleSessionJpaRepository
        );
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void testAssignGate() throws Exception {
        Mockito.doNothing().when(adminUseCase).assignGate(anyString(), anyString());
        Mockito.doNothing().when(actionLogger).logAdminAction(anyString(), anyString(), anyString(), anyString());

        Map<String, String> body = new HashMap<>();
        body.put("gate", "Gate A");
        body.put("guard", "Guard 1");

        mockMvc.perform(post("/admin/gates/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gate\":\"Gate A\",\"guard\":\"Guard 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.gate").value("Gate A"))
                .andExpect(jsonPath("$.assignedTo").value("Guard 1"));

        Mockito.verify(adminUseCase, Mockito.times(1)).assignGate("Gate A", "Guard 1");
    }

    @Test
    void testUpdateStudentStatus() throws Exception {
        Mockito.doNothing().when(adminUseCase).updateStudentStatus(anyString(), any());
        Mockito.doNothing().when(actionLogger).logAdminAction(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch("/admin/students/usr-001/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statutFrais\":\"En Règle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").exists());

        Mockito.verify(adminUseCase, Mockito.times(1)).updateStudentStatus(eq("usr-001"), any());
    }

    @Test
    void testGetRealtimeLogs() throws Exception {
        List<String> mockLogs = Arrays.asList("Log 1", "Log 2", "Log 3");
        Mockito.when(adminUseCase.getRealtimeLogs()).thenReturn(mockLogs);

        mockMvc.perform(get("/admin/realtime-logs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("Log 1"));

        Mockito.verify(adminUseCase, Mockito.times(1)).getRealtimeLogs();
    }

    @Test
    void testUpdateCandidaturesSettings() throws Exception {
        Mockito.doNothing().when(adminUseCase).updateCandidaturesSettings(anyBoolean());

        mockMvc.perform(post("/admin/candidatures/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.candidaturesEnabled").value(true));

        Mockito.verify(adminUseCase, Mockito.times(1)).updateCandidaturesSettings(true);
    }

    @Test
    void testGetStats() throws Exception {
        mockMvc.perform(get("/admin/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentsCount").value(150))
                .andExpect(jsonPath("$.professorsCount").value(25))
                .andExpect(jsonPath("$.coursesCount").value(30))
                .andExpect(jsonPath("$.classesCount").value(8));
    }

    @Test
    void testGetUsers() throws Exception {
        mockMvc.perform(get("/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").isArray())
                .andExpect(jsonPath("$.professors").isArray())
                .andExpect(jsonPath("$.promotions").isArray());
    }

    @Test
    void testAddStudent() throws Exception {
        Mockito.doNothing().when(actionLogger).logAdminAction(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/admin/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Student\",\"matricule\":\"2026-TEST-001\",\"promotion_id\":\"L1-GL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Student"))
                .andExpect(jsonPath("$.matricule").value("2026-TEST-001"))
                .andExpect(jsonPath("$.promotion_id").value("L1-GL"));
    }

    @Test
    void testDeleteStudent() throws Exception {
        Mockito.doNothing().when(actionLogger).logAdminAction(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(delete("/admin/students/usr-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value("usr-001"));
    }

    @Test
    void testAddProfessor() throws Exception {
        Mockito.doNothing().when(actionLogger).logAdminAction(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/admin/professors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Professor\",\"email\":\"test@ecole221.sn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Professor"))
                .andExpect(jsonPath("$.email").value("test@ecole221.sn"));
    }

    @Test
    void testDeleteProfessor() throws Exception {
        Mockito.doNothing().when(actionLogger).logAdminAction(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(delete("/admin/professors/usr-002")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value("usr-002"));
    }
}