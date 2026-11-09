package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * FEAT-003 Q2 (Agent 01) : saisir une seule note ne doit pas effacer l'autre, ni inventer de zéro.
 * Testé sur l'adaptateur RÉEL (ProfessorRepositoryAdapter, table grades simulée en mémoire) — l'ancien
 * InMemoryProfessorRepository, pré-rempli de notes inventées, a été supprimé.
 */
class SaisiePartielleNoteTest {

    private ProfessorRepositoryAdapter repo;

    @BeforeEach
    void setUp() {
        Map<String, GradeJpaEntity> table = new HashMap<>();
        GradeJpaRepository grades = Mockito.mock(GradeJpaRepository.class);
        Mockito.when(grades.findByStudentIdAndCourseId(anyString(), anyString())).thenAnswer(inv ->
                Optional.ofNullable(table.get(inv.getArgument(0) + "|" + inv.getArgument(1))));
        Mockito.when(grades.save(any(GradeJpaEntity.class))).thenAnswer(inv -> {
            GradeJpaEntity g = inv.getArgument(0);
            table.put(g.getStudentId() + "|" + g.getCourseId(), g);
            return g;
        });
        repo = new ProfessorRepositoryAdapter(
                Mockito.mock(CourseJpaRepository.class), Mockito.mock(HomeworkJpaRepository.class),
                Mockito.mock(LessonJpaRepository.class), Mockito.mock(ModuleJpaRepository.class),
                Mockito.mock(QuizJpaRepository.class), grades, Mockito.mock(AttendanceJpaRepository.class),
                Mockito.mock(UserJpaRepository.class), Mockito.mock(ComptesEcoleService.class));
    }

    @Test
    void examenSaisiApresLeCcConserveLeCc() {
        repo.saveGrade("e1", "c1", 14.0, null);
        repo.saveGrade("e1", "c1", null, 16.0);
        var note = repo.getGrade("e1", "c1");
        assertEquals(14.0, note.get("cc"));
        assertEquals(16.0, note.get("examen"));
        assertEquals(15.2, note.get("average"));
    }

    @Test
    void premiereSaisiePartielleNInventePasDeZero() {
        repo.saveGrade("e2", "c1", null, 12.0);
        assertNull(repo.getGrade("e2", "c1").get("cc"));
    }

    @Test
    void aucuneNoteSaisieDonneUneReponseVide() {
        assertTrue(repo.getGrade("inconnu", "c1").isEmpty());
    }
}
