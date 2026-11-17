package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.CourseDetailRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.CourseDetail;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentDataRepository;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase;
import org.junit.jupiter.api.AfterEach;
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
    private EmailUseCase emailUseCase;
    private ecole_221.sn.ecole.modules.professor.application.service.QuizTentativeService quizEleve;
    private MockMvc mockMvc;

    /** APP-SEC-19 : le portail étudiant sert l'étudiant CONNECTÉ (ici usr-001). */
    private static void connecter(String id) {
        var u = ecole_221.sn.ecole.modules.auth.domain.model.UserFactory.reconstructSimple(id, "Ba", "Fatou", id + "@ex.sn",
                "ecole221", "ETU-1", ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur.ETUDIANT, null);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    @AfterEach
    void deconnecter() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        connecter("usr-001");
        studentUseCase = Mockito.mock(StudentUseCase.class);
        courseDetailRepository = Mockito.mock(CourseDetailRepository.class);
        studentDataRepository = Mockito.mock(StudentDataRepository.class);
        actionLogger = Mockito.mock(ActionLogger.class);
        emailUseCase = Mockito.mock(EmailUseCase.class);
        
        StudentController controller = new StudentController(
            studentUseCase, courseDetailRepository, studentDataRepository, 
            null, actionLogger, emailUseCase
        );
        // Appartenance du cours à la classe de l'élève (détails de cours)
        quizEleve = Mockito.mock(ecole_221.sn.ecole.modules.professor.application.service.QuizTentativeService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "quizEleve", quizEleve);
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void profilConstruitDepuisLeCompteReel() throws Exception {
        Mockito.when(studentDataRepository.findById(anyString())).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/student/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("usr-001"))
                .andExpect(jsonPath("$.nom").value("Ba"))
                .andExpect(jsonPath("$.prenom").value("Fatou"))
                .andExpect(jsonPath("$.matricule").value("ETU-1"));
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
    void badgeAvecLeMatriculeReel() throws Exception {
        Mockito.when(studentDataRepository.findById(anyString())).thenReturn(java.util.Optional.empty());
        Map<String, Object> badge = new HashMap<>();
        badge.put("matricule", "ETU-1");
        Mockito.when(studentUseCase.getDigitalBadge(anyString(), anyString(), anyString(), Mockito.any())).thenReturn(badge);

        mockMvc.perform(get("/student/digital-badge")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricule").value("ETU-1"));

        Mockito.verify(studentUseCase).getDigitalBadge(Mockito.eq("usr-001"), Mockito.eq("ETU-1"), Mockito.eq("Fatou Ba"), Mockito.any());
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
        Mockito.when(quizEleve.estCoursDeSaClasse("course-1", "usr-001")).thenReturn(true);
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
        Mockito.when(quizEleve.estCoursDeSaClasse("course-404", "usr-001")).thenReturn(true);
        Mockito.when(courseDetailRepository.findById("course-404")).thenReturn(java.util.Optional.empty());
        Mockito.when(courseDetailRepository.findById("c-404")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/student/courses/course-404/details")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cours non trouvé avec l'identifiant: course-404"));
    }

    @Test
    void detailsDuCoursSansLesBonnesReponsesDesQuiz() throws Exception {
        Mockito.when(quizEleve.estCoursDeSaClasse("course-7", "usr-001")).thenReturn(true);
        Map<String, Object> q1 = new HashMap<>();
        q1.put("question", "Combien de tours fait la boucle ?");
        q1.put("options", java.util.List.of("1", "2", "3"));
        q1.put("reponseCorrecte", 2);
        q1.put("explication", "La condition est vérifiée trois fois.");
        Map<String, Object> data = new HashMap<>();
        data.put("id", "course-7");
        data.put("title", "Algorithmique");
        data.put("quizzes", java.util.List.of(q1));
        Mockito.when(courseDetailRepository.findById("course-7")).thenReturn(java.util.Optional.of(new CourseDetail("course-7", data)));

        String corps = mockMvc.perform(get("/student/courses/course-7/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Algorithmique"))
                .andExpect(jsonPath("$.quizzes[0].question").value("Combien de tours fait la boucle ?"))
                .andExpect(jsonPath("$.quizzes[0].options.length()").value(3))
                .andExpect(jsonPath("$.quizzes[0].reponseCorrecte").doesNotExist())
                .andExpect(jsonPath("$.quizzes[0].explication").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(corps).doesNotContain("reponseCorrecte");
        // le contenu enregistré n'est pas modifié
        org.assertj.core.api.Assertions.assertThat(q1).containsKey("reponseCorrecte");
    }

    @Test
    void detailsDUnCoursHorsDeSaClasse404SansLireLeContenu() throws Exception {
        Mockito.when(quizEleve.estCoursDeSaClasse(anyString(), anyString())).thenReturn(false);
        mockMvc.perform(get("/student/courses/course-9/details"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cours non trouvé avec l'identifiant: course-9"));
        Mockito.verify(courseDetailRepository, Mockito.never()).findById(anyString());
    }

    @Test
    void chaqueEtudiantVoitSesPropresNotes() throws Exception {
        connecter("etu-autre");
        Mockito.when(studentUseCase.getStudentGrades(anyString())).thenReturn(java.util.List.of());
        mockMvc.perform(get("/student/grades")).andExpect(status().isOk());
        Mockito.verify(studentUseCase).getStudentGrades("etu-autre");
        Mockito.verify(studentUseCase, Mockito.never()).getStudentGrades("usr-001");
    }

    @Test
    void sansConnexionRefuse() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        mockMvc.perform(get("/student/grades")).andExpect(status().isUnauthorized());
    }
}
