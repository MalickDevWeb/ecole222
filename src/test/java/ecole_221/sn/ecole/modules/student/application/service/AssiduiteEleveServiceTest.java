package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.parent.application.service.JustificatifAbsenceService;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** « Mes absences » : mêmes règles que la feuille d'appel du professeur (lot A). */
class AssiduiteEleveServiceTest {

    private AttendanceJpaRepository presences;
    private CourseJpaRepository cours;
    private AssiduiteEleveService service;
    private JustificatifAbsenceService justificatifs;

    @BeforeEach
    void preparer() {
        presences = Mockito.mock(AttendanceJpaRepository.class);
        InscriptionClasseJpaRepository inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
        cours = Mockito.mock(CourseJpaRepository.class);
        ScheduleSessionJpaRepository seances = Mockito.mock(ScheduleSessionJpaRepository.class);
        UserJpaRepository comptes = Mockito.mock(UserJpaRepository.class);
        justificatifs = Mockito.mock(JustificatifAbsenceService.class);
        Mockito.when(justificatifs.parPresence("e1")).thenReturn(Map.of());
        service = new AssiduiteEleveService(presences, inscriptions, cours, seances, comptes, justificatifs);

        Mockito.when(inscriptions.findByUserId("e1")).thenReturn(List.of(new InscriptionClasseJpaEntity("i1", "e1", "cl-1")));
        Mockito.when(cours.findByClasse("cl-1")).thenReturn(List.of(
                new CourseJpaEntity("c-algo", "Algorithmique", "cl-1", 30, "p1"),
                new CourseJpaEntity("c-bd", "Bases de données", "cl-1", 30, "p1")));
        Mockito.when(cours.findById(Mockito.anyString())).thenReturn(Optional.empty());
        UserJpaEntity prof = new UserJpaEntity();
        prof.setId("p1"); prof.setPrenom("Awa"); prof.setNom("Diop");
        Mockito.when(comptes.findById("p1")).thenReturn(Optional.of(prof));
        ScheduleSessionJpaEntity s = new ScheduleSessionJpaEntity();
        s.setId("ses-1"); s.setCourseId("c-algo"); s.setTime("08:00-10:00"); s.setRoom("Salle 12");
        Mockito.when(seances.findById("ses-1")).thenReturn(Optional.of(s));
    }

    private static AttendanceJpaEntity saisie(String id, String cours, String seance, String date, String statut, String note, int minute) {
        AttendanceJpaEntity a = new AttendanceJpaEntity();
        a.setId(id); a.setStudentId("e1"); a.setCourseId(cours); a.setSessionId(seance);
        a.setDate(date); a.setStatus(statut); a.setNote(note);
        a.setRecordedAt(LocalDateTime.of(2026, 11, 10, 9, minute));
        return a;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> ligne(Map<String, Object> res, String courseId) {
        return ((List<Map<String, Object>>) res.get("cours")).stream()
                .filter(l -> courseId.equals(l.get("courseId"))).findFirst().orElseThrow();
    }

    @Test
    void sans_aucun_appel_le_taux_est_vide_et_non_invente() {
        Mockito.when(presences.findByStudentId("e1")).thenReturn(List.of());
        Map<String, Object> res = service.assiduite("e1");
        assertNull(((Map<?, ?>) res.get("global")).get("taux"));
        assertEquals(2, ((List<?>) res.get("cours")).size(), "ses cours apparaissent même sans appel");
        assertNull(ligne(res, "c-bd").get("taux"));
        assertEquals("Awa Diop", ligne(res, "c-algo").get("professeur"));
        assertTrue(((List<?>) res.get("seances")).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void taux_par_cours_et_global_avec_la_regle_du_professeur() {
        Mockito.when(presences.findByStudentId("e1")).thenReturn(List.of(
                saisie("a1", "c-algo", "ses-1", "2026-11-02", "PRESENT", null, 1),
                saisie("a2", "c-algo", "ses-1", "2026-11-09", "ABSENT", "Pas venu", 2),
                saisie("a3", "c-algo", "ses-1", "2026-11-03", "LATE", null, 3),      // ancien statut anglais
                saisie("a4", "c-bd", null, "2026-11-04T10:00:00", "EXCUSED", "Certificat", 4)));
        Map<String, Object> res = service.assiduite("e1");

        Map<String, Object> algo = ligne(res, "c-algo");
        assertEquals(1, algo.get("present")); assertEquals(1, algo.get("retard")); assertEquals(1, algo.get("absent"));
        assertEquals(66.7, algo.get("taux"), "taux = (présences + retards) / séances notées");
        assertEquals(0.0, ligne(res, "c-bd").get("taux"), "excusé ne compte pas comme présent");
        assertEquals(50.0, ((Map<?, ?>) res.get("global")).get("taux"));

        List<Map<String, Object>> seances = (List<Map<String, Object>>) res.get("seances");
        assertEquals(3, seances.size(), "seules les séances manquées sont détaillées");
        assertEquals("2026-11-09", seances.get(0).get("date"), "les plus récentes d'abord");
        assertEquals("ABSENT", seances.get(0).get("statut"));
        assertEquals("Pas venu", seances.get(0).get("observation"));
        assertEquals("Salle 12", seances.get(0).get("salle"));
        assertEquals("2026-11-04", seances.get(1).get("date"), "date-heure ramenée au jour");
        assertEquals("EXCUSE", seances.get(1).get("statut"));
        assertEquals("RETARD", seances.get(2).get("statut"), "ancien statut LATE affiché comme retard");
    }

    @Test
    void une_seance_corrigee_n_est_comptee_qu_une_fois_avec_la_derniere_saisie() {
        Mockito.when(presences.findByStudentId("e1")).thenReturn(List.of(
                saisie("a1", "c-algo", "ses-1", "2026-11-09", "ABSENT", null, 1),
                saisie("a2", "c-algo", "ses-1", "2026-11-09", "PRESENT", null, 5)));
        Map<String, Object> res = service.assiduite("e1");
        assertEquals(1, ligne(res, "c-algo").get("seancesNotees"));
        assertEquals(100.0, ligne(res, "c-algo").get("taux"));
        assertTrue(((List<?>) res.get("seances")).isEmpty());
    }

    @Test
    void auto_pointages_et_statuts_inconnus_sont_ignores() {
        Mockito.when(presences.findByStudentId("e1")).thenReturn(List.of(
                saisie("q1", "self-check", null, "2026-11-09", "ENTRY", null, 1),
                saisie("q2", "QR", null, "2026-11-09", "PRESENT", null, 2),
                saisie("q3", "c-algo", null, "2026-11-09", "INCONNU", null, 3)));
        Map<String, Object> res = service.assiduite("e1");
        assertEquals(0, ((Map<?, ?>) res.get("global")).get("seancesNotees"));
        assertEquals(2, ((List<?>) res.get("cours")).size(), "aucun faux cours « QR » ou « self-check »");
    }
}
