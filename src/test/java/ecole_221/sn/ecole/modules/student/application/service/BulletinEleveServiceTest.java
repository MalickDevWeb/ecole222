package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.professor.application.service.EvaluationService;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Bulletin : réutilise les moyennes du professeur, moyenne générale simple, rien d'inventé. */
class BulletinEleveServiceTest {

    private EvaluationService evaluations;
    private BulletinEleveService service;

    @BeforeEach
    void preparer() {
        InscriptionClasseJpaRepository inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
        CourseJpaRepository cours = Mockito.mock(CourseJpaRepository.class);
        evaluations = Mockito.mock(EvaluationService.class);
        service = new BulletinEleveService(inscriptions, cours, evaluations, Mockito.mock(UserJpaRepository.class));
        Mockito.when(inscriptions.findByUserId("e1")).thenReturn(List.of(new InscriptionClasseJpaEntity("i1", "e1", "cl-1")));
        Mockito.when(cours.findByClasse("cl-1")).thenReturn(List.of(
                new CourseJpaEntity("c-algo", "Algorithmique", "cl-1", 30, null),
                new CourseJpaEntity("c-bd", "Bases de données", "cl-1", 30, null),
                new CourseJpaEntity("c-ang", "Anglais", "cl-1", 20, null)));
    }

    private static Map<String, Object> eval(String id, boolean verrouillee) {
        return new LinkedHashMap<>(Map.of("id", id, "titre", "DS " + id, "type", "EXAMEN", "date", "2026-11-09",
                "coefficient", 2.0, "noteSur", 20.0, "session", "NORMALE", "verrouillee", verrouillee));
    }

    private static Map<String, Object> moyennes(List<Map<String, Object>> evals, Double moyenneE1, Map<String, Object> notesE1) {
        Map<String, Object> e1 = new LinkedHashMap<>();
        e1.put("studentId", "e1"); e1.put("moyenne", moyenneE1); e1.put("mention", moyenneE1 == null ? null : "Bien");
        e1.put("rang", moyenneE1 == null ? null : 1); e1.put("nombreElevesClasses", 2L); e1.put("notes", notesE1);
        Map<String, Object> e2 = new LinkedHashMap<>(Map.of("studentId", "e2", "moyenne", 9.0, "notes", Map.of()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("evaluations", evals); m.put("eleves", List.of(e1, e2)); m.put("moyenneClasse", 12.2);
        return m;
    }

    @Test
    @SuppressWarnings("unchecked")
    void moyenne_generale_simple_des_matieres_notees_et_statut_provisoire() {
        Mockito.when(evaluations.moyennes("c-algo")).thenReturn(moyennes(List.of(eval("a1", false)), 15.4,
                Map.of("a1", Map.of("valeur", 15.4))));
        Mockito.when(evaluations.moyennes("c-bd")).thenReturn(moyennes(List.of(eval("b1", true)), 11.0,
                Map.of("b1", new HashMap<>(Map.of("valeur", 11.0)))));
        Mockito.when(evaluations.moyennes("c-ang")).thenReturn(moyennes(List.of(), null, Map.of()));

        Map<String, Object> b = service.bulletin("e1");
        assertEquals(13.2, b.get("moyenneGenerale"), "(15,4 + 11) / 2 ; l'anglais sans note n'est pas compté");
        assertEquals(2, b.get("matieresNotees"));
        assertEquals(true, b.get("provisoire"), "Algorithmique n'est pas encore verrouillée");
        List<Map<String, Object>> m = (List<Map<String, Object>>) b.get("matieres");
        assertEquals(List.of("Algorithmique", "Anglais", "Bases de données"), m.stream().map(x -> x.get("cours")).toList());
        assertEquals(true, m.get(0).get("provisoire"));
        assertEquals(false, m.get(2).get("provisoire"), "BD : évaluation verrouillée → définitive");
        assertNull(m.get(1).get("moyenne"), "pas de note : « — », jamais 0");
        assertEquals(15.4, ((List<Map<String, Object>>) m.get(0).get("evaluations")).get(0).get("valeur"));
        assertEquals(1, m.get(0).get("rang"));
    }

    @Test
    void sans_aucune_note_rien_n_est_invente() {
        Mockito.when(evaluations.moyennes(Mockito.anyString())).thenReturn(moyennes(List.of(), null, Map.of()));
        Map<String, Object> b = service.bulletin("e1");
        assertNull(b.get("moyenneGenerale"));
        assertNull(b.get("mention"));
        assertEquals(0, b.get("matieresNotees"));
        assertEquals(false, b.get("provisoire"));
    }

    @Test
    void eleve_sans_classe_bulletin_vide() {
        Map<String, Object> b = service.bulletin("inconnu");
        assertTrue(((List<?>) b.get("matieres")).isEmpty());
        assertNull(b.get("moyenneGenerale"));
    }
}
