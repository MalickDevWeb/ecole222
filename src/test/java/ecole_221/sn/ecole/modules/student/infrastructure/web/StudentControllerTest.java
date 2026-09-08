package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.CourseDetailRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.CourseDetail;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentDataRepository;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.ai.application.port.usecase.AIUseCase;
import ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentControllerTest {

    private StudentUseCase studentUseCase;
    private CourseDetailRepository courseDetailRepository;
    private StudentDataRepository studentDataRepository;
    private ActionLogger actionLogger;
    private AIUseCase aiUseCase;
    private EmailUseCase emailUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        studentUseCase = Mockito.mock(StudentUseCase.class);
        courseDetailRepository = Mockito.mock(CourseDetailRepository.class);
        studentDataRepository = Mockito.mock(StudentDataRepository.class);
        actionLogger = Mockito.mock(ActionLogger.class);
        aiUseCase = Mockito.mock(AIUseCase.class);
        emailUseCase = Mockito.mock(EmailUseCase.class);
        
        StudentController controller = new StudentController(
            studentUseCase, courseDetailRepository, studentDataRepository, 
            null, actionLogger, aiUseCase, emailUseCase
        );
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetProfile() throws Exception {
        Map<String, Object> mockProfile = new HashMap<>();
        mockProfile.put("id", "usr-001");
        mockProfile.put("name", "Test Student");
        mockProfile.put("email", "test@ecole221.sn");
        
        Mockito.when(studentDataRepository.findById(anyString())).thenReturn(java.util.Optional.empty());
        Mockito.when(studentUseCase.getStudentProfile(anyString())).thenReturn(mockProfile);

        mockMvc.perform(get("/student/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("usr-001"));

        Mockito.verify(studentUseCase, Mockito.times(1)).getStudentProfile("usr-001");
    }

    @Test
    void testGetDashboard() throws Exception {
        Map<String, Object> mockDashboard = new HashMap<>();
        mockDashboard.put("coursesCount", 5);
        mockDashboard.put("average", 15.5);
        
        Mockito.when(studentDataRepository.findById(anyString())).thenReturn(java.util.Optional.empty());
        Mockito.when(studentUseCase.getStudentDashboard(anyString())).thenReturn(mockDashboard);

        mockMvc.perform(get("/student/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(studentUseCase, Mockito.times(1)).getStudentDashboard("usr-001");
    }

    @Test
    void testGetDigitalBadge() throws Exception {
        Map<String, Object> mockBadge = new HashMap<>();
        mockBadge.put("matricule", "2026-EC-001");
        mockBadge.put("qrCode", "test-qr-code");
        
        Mockito.when(studentUseCase.getDigitalBadge(anyString())).thenReturn(mockBadge);

        mockMvc.perform(get("/student/digital-badge")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricule").value("2026-EC-001"));

        Mockito.verify(studentUseCase, Mockito.times(1)).getDigitalBadge("usr-001");
    }

    @Test
    void testValidateQr() throws Exception {
        Mockito.when(studentUseCase.validateQrToken(anyString(), anyLong(), anyString())).thenReturn(true);

        mockMvc.perform(post("/student/qr-validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricule\":\"2026-EC-001\",\"minuteEpoch\":1234567890,\"token\":\"test-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        Mockito.verify(studentUseCase, Mockito.times(1)).validateQrToken(anyString(), anyLong(), anyString());
    }

    @Test
    void testSelfCheckInAcceptsCoursAlias() throws Exception {
        mockMvc.perform(post("/student/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"COURS\",\"method\":\"MANUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Mockito.verify(studentUseCase, Mockito.times(1))
                .submitSelfAttendance(eq("usr-001"), eq("PRESENT"), eq("MANUAL"));
    }

    @Test
    void testGetCourseDetailsFallbackToLegacyId() throws Exception {
        Map<String, Object> detailData = new HashMap<>();
        detailData.put("id", "course-1");
        detailData.put("title", "Architecture des Microservices");

        Mockito.when(courseDetailRepository.findById("course-1")).thenReturn(java.util.Optional.empty());
        Mockito.when(courseDetailRepository.findById("c-1")).thenReturn(java.util.Optional.of(new CourseDetail("c-1", detailData)));

        mockMvc.perform(get("/student/courses/course-1/details")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("course-1"));
    }

    @Test
    void testGetCourseDetailsNotFoundReturns404() throws Exception {
        Mockito.when(courseDetailRepository.findById("course-404")).thenReturn(java.util.Optional.empty());
        Mockito.when(courseDetailRepository.findById("c-404")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/student/courses/course-404/details")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cours non trouvé avec l'identifiant: course-404"));
    }
}