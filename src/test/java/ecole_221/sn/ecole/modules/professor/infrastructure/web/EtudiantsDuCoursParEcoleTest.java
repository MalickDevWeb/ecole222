package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaRepository;
import ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.GradeJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.GradeJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import ecole_221.sn.ecole.modules.sync.application.service.SyncService;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncAction;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncResult;
import ecole_221.sn.ecole.modules.sync.domain.valueobject.SyncActionType;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Étudiants d'un cours : ceux de l'école du professeur, avec leurs VRAIES notes / présences (avant : tous les
 * étudiants de toutes les écoles, « PRESENT » et 15/20 codés en dur). Présences : plus de date ni de cours inventés.
 */
class EtudiantsDuCoursParEcoleTest {

    private ProfessorUseCase useCase;
    private CourseJpaRepository courses;
    private GradeJpaRepository notes;
    private AttendanceJpaRepository presences;
    private UserJpaRepository comptes;
    private OrganizationUserJpaRepository appartenances;
    private Organization ecole;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = Mockito.mock(ProfessorUseCase.class);
        courses = Mockito.mock(CourseJpaRepository.class);
        notes = Mockito.mock(GradeJpaRepository.class);
        presences = Mockito.mock(AttendanceJpaRepository.class);
        comptes = Mockito.mock(UserJpaRepository.class);
        appartenances = Mockito.mock(OrganizationUserJpaRepository.class);
        ComptesEcoleService comptesEcole = new ComptesEcoleService(comptes, appartenances, new BCryptPasswordEncoder(4));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ProfessorController(useCase, Mockito.mock(ActionLogger.class), courses, notes, presences, comptesEcole),
                        new ProfExtraController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        ecole = OrganizationFactory.create("Lycée A", OrganizationType.ECOLE, "a@exemple.sn");
        TenantContext.setCurrentOrganization(ecole);
        connecter("prof-1");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void connecter(String id) {
        User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(id);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private CourseJpaEntity cours(String id, String profId) {
        CourseJpaEntity c = new CourseJpaEntity();
        c.setId(id);
        c.setProfId(profId);
        c.setOrganizationId(ecole.getId());
        return c;
    }

    private OrganizationUserJpaEntity lien(String userId) {
        OrganizationUserJpaEntity l = new OrganizationUserJpaEntity();
        l.setId("ou-" + userId);
        l.setUserId(userId);
        l.setOrganizationId(ecole.getId());
        l.setRole("ETUDIANT");
        l.setStatus("ACTIVE");
        return l;
    }

    private UserJpaEntity compte(String id, String prenom, String nom) {
        UserJpaEntity u = new UserJpaEntity();
        u.setId(id);
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setRole("ETUDIANT");
        return u;
    }

    @Test
    void etudiantsDeLEcoleAvecNotesEtPresencesReelles() throws Exception {
        Mockito.when(courses.findById("c1")).thenReturn(Optional.of(cours("c1", "prof-1")));
        Mockito.when(appartenances.findByOrganizationIdAndRole(ecole.getId(), "ETUDIANT"))
                .thenReturn(List.of(lien("e1"), lien("e2")));
        Mockito.when(comptes.findAllById(any())).thenReturn(List.of(compte("e1", "Awa", "Fall"), compte("e2", "Ibou", "Sarr")));
        GradeJpaEntity note = new GradeJpaEntity();
        note.setStudentId("e1");
        note.setCourseId("c1");
        note.setAverage(12.5);
        Mockito.when(notes.findByCourseId("c1")).thenReturn(List.of(note));
        AttendanceJpaEntity ancienne = new AttendanceJpaEntity();
        ancienne.setStudentId("e1");
        ancienne.setDate("2026-11-01");
        ancienne.setStatus("PRESENT");
        AttendanceJpaEntity recente = new AttendanceJpaEntity();
        recente.setStudentId("e1");
        recente.setDate("2026-11-08");
        recente.setStatus("ABSENT");
        Mockito.when(presences.findByCourseId("c1")).thenReturn(List.of(ancienne, recente));

        mockMvc.perform(get("/professor/courses/c1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("e1"))
                .andExpect(jsonPath("$[0].averageGrade").value(12.5))
                .andExpect(jsonPath("$[0].attendanceStatus").value("ABSENT"))
                .andExpect(jsonPath("$[1].id").value("e2"))
                .andExpect(jsonPath("$[1].averageGrade").value(nullValue()))
                .andExpect(jsonPath("$[1].attendanceStatus").value(nullValue()))
                .andExpect(jsonPath("$[1].matricule").value(nullValue()));
        verify(comptes, never()).findByRole(anyString());   // plus de lecture de toutes les écoles
    }

    @Test
    void coursDUnAutreProfesseurInterdit() throws Exception {
        Mockito.when(courses.findById("c2")).thenReturn(Optional.of(cours("c2", "prof-2")));
        mockMvc.perform(get("/professor/courses/c2/students")).andExpect(status().isForbidden());
    }

    @Test
    void coursInconnuOuDUneAutreEcole404() throws Exception {
        Mockito.when(courses.findById("absent")).thenReturn(Optional.empty());
        mockMvc.perform(get("/professor/courses/absent/students")).andExpect(status().isNotFound());
        CourseJpaEntity ailleurs = cours("c3", "prof-1");
        ailleurs.setOrganizationId("autre-ecole");
        Mockito.when(courses.findById("c3")).thenReturn(Optional.of(ailleurs));
        mockMvc.perform(get("/professor/courses/c3/students")).andExpect(status().isNotFound());
    }

    @Test
    void presenceSansDateOuSansCoursRefusee() throws Exception {
        mockMvc.perform(post("/prof/attendance/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"e1\",\"status\":\"PRESENT\",\"courseId\":\"c1\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/prof/attendance/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"studentId\":\"e1\",\"status\":\"PRESENT\",\"courseId\":\"c1\",\"date\":\"2026-11-09\"},"
                                + "{\"studentId\":\"e2\",\"status\":\"PRESENT\",\"date\":\"2026-11-09\"}]"))
                .andExpect(status().isBadRequest());
        verify(useCase, never()).submitAttendance(any(), any(), any(), any(), any());

        mockMvc.perform(post("/prof/attendance/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"e1\",\"status\":\"PRESENT\",\"courseId\":\"c1\",\"date\":\"2026-11-09\"}"))
                .andExpect(status().isOk());
        verify(useCase).submitAttendance("2026-11-09", "e1", null, "PRESENT", "c1");
    }

    /** SyncActionType n'accepte que CREATE/UPDATE/DELETE/SYNC : on simule le type métier envoyé par le client. */
    private static SyncAction action(String id, String type, Map<String, Object> payload) {
        SyncActionType t = Mockito.mock(SyncActionType.class);
        Mockito.when(t.name()).thenReturn(type);
        SyncAction a = Mockito.mock(SyncAction.class);
        Mockito.when(a.getId()).thenReturn(id);
        Mockito.when(a.getType()).thenReturn(t);
        Mockito.when(a.getPayload()).thenReturn(payload);
        return a;
    }

    @Test
    void synchronisationRefuseLesActionsIncompletesSansValeurParDefaut() {
        var etudiantUseCase = Mockito.mock(ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase.class);
        SyncService sync = new SyncService(etudiantUseCase, useCase,
                Mockito.mock(ecole_221.sn.ecole.modules.security.application.port.usecase.SecurityUseCase.class),
                Mockito.mock(ecole_221.sn.ecole.modules.admin.application.port.usecase.AdminUseCase.class));

        List<SyncResult> res = sync.synchronize(List.of(
                action("a1", "ATTENDANCE_SUBMIT", Map.of("studentId", "e1", "status", "PRESENT")),
                action("a2", "ATTENDANCE_SELF", Map.of("studentId", "usr-001", "type", "ENTREE")),
                action("a3", "ATTENDANCE_SELF", Map.of("type", "ENTREE"))));

        assertTrue(res.get(0).isFailed());
        assertTrue(res.get(0).getMessage().contains("date"), res.get(0).getMessage());
        assertTrue(res.get(1).isFailed());   // un autre élève que celui connecté
        assertTrue(res.get(2).isSuccess());
        verify(useCase, never()).submitAttendance(any(), any(), any(), any(), any());
        verify(etudiantUseCase).submitSelfAttendance("prof-1", "ENTREE", null);
        verify(etudiantUseCase, never()).submitSelfAttendance(Mockito.eq("usr-001"), any(), any());
    }
}
