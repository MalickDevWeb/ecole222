package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.EvaluationJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.EvaluationJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.NoteEvaluationJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.NoteEvaluationJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Courbe de performance : calculée sur les vraies évaluations et présences, jamais inventée. */
class PerformanceEleveServiceTest {

    private final List<NoteEvaluationJpaEntity> toutesNotes = new ArrayList<>();

    private static EvaluationJpaEntity eval(String id, String date, double coef, double sur, String rattrape) {
        EvaluationJpaEntity e = mock(EvaluationJpaEntity.class);
        when(e.getId()).thenReturn(id);
        when(e.getCourseId()).thenReturn("cours-1");
        when(e.getDateEvaluation()).thenReturn(LocalDate.parse(date));
        when(e.getCoefficient()).thenReturn(coef);
        when(e.getNoteSur()).thenReturn(sur);
        when(e.getSession()).thenReturn(rattrape == null ? "NORMALE" : "RATTRAPAGE");
        when(e.getEvaluationRattrapeeId()).thenReturn(rattrape);
        return e;
    }

    private void note(String eleve, String eval, Double valeur, String absence) {
        NoteEvaluationJpaEntity n = mock(NoteEvaluationJpaEntity.class);
        when(n.getStudentId()).thenReturn(eleve);
        when(n.getEvaluationId()).thenReturn(eval);
        when(n.getCourseId()).thenReturn("cours-1");
        when(n.getValeur()).thenReturn(valeur);
        when(n.getAbsence()).thenReturn(absence);
        toutesNotes.add(n);
    }

    private static AttendanceJpaEntity appel(String date, String statut) {
        AttendanceJpaEntity a = mock(AttendanceJpaEntity.class);
        when(a.getDate()).thenReturn(date);
        when(a.getStatus()).thenReturn(statut);
        return a;
    }

    @Test
    void moisParMoisAvecLaRegleDuProfesseur() {
        List<EvaluationJpaEntity> evals = List.of(
                eval("e1", "2026-10-05", 1, 20, null), eval("e2", "2026-10-20", 2, 10, null),
                eval("e3", "2026-11-10", 1, 20, null), eval("r3", "2026-11-25", 1, 20, "e3"));
        note("moi", "e1", 15.0, null); note("moi", "e2", 8.0, null);     // (15×1 + 16×2) / 3 = 15,67
        note("autre", "e1", 11.0, null); note("autre", "e2", 6.0, null); // (11×1 + 12×2) / 3 = 11,67
        note("moi", "e3", null, "ABS"); note("moi", "r3", 12.0, null);  // rattrapage meilleur que ABS (0) → 12

        NoteEvaluationJpaRepository notes = mock(NoteEvaluationJpaRepository.class);
        when(notes.findByStudentId(anyString())).thenAnswer(i -> toutesNotes.stream().filter(n -> n.getStudentId().equals(i.getArgument(0))).toList());
        when(notes.findByEvaluationId(anyString())).thenAnswer(i -> toutesNotes.stream().filter(n -> n.getEvaluationId().equals(i.getArgument(0))).toList());
        EvaluationJpaRepository evaluations = mock(EvaluationJpaRepository.class);
        when(evaluations.findByCourseIdOrderByDateEvaluationAscCreeLeAsc("cours-1")).thenReturn(evals);
        AttendanceJpaRepository presences = mock(AttendanceJpaRepository.class);
        List<AttendanceJpaEntity> appels = List.of(appel("2026-10-01", "PRESENT"), appel("2026-10-08", "LATE"),
                appel("2026-10-15", "ABSENT"), appel("2026-12-03", "EXCUSED"), appel("pas-une-date", "PRESENT"));
        when(presences.findByStudentId("moi")).thenReturn(appels);

        List<Map<String, Object>> r = new PerformanceEleveService(notes, evaluations, presences).performance("moi");

        assertThat(r).extracting(m -> m.get("mois")).containsExactly("2026-10", "2026-11", "2026-12");
        assertThat(r.get(0)).containsEntry("note", 15.67).containsEntry("moyenneClasse", 13.67)
                .containsEntry("presence", 67).containsEntry("seances", 3).containsEntry("evaluations", 2);
        assertThat(r.get(1)).containsEntry("note", 12.0).containsEntry("presence", null);
        assertThat(r.get(2)).containsEntry("note", null).containsEntry("moyenneClasse", null).containsEntry("presence", 0);
    }

    @Test
    void aucuneDonneeAucunPoint() {
        NoteEvaluationJpaRepository notes = mock(NoteEvaluationJpaRepository.class);
        AttendanceJpaRepository presences = mock(AttendanceJpaRepository.class);
        assertThat(new PerformanceEleveService(notes, mock(EvaluationJpaRepository.class), presences).performance("x")).isEmpty();
    }
}
