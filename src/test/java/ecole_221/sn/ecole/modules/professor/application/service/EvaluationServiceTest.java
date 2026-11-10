package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** Évaluations : saisie en lot (tout ou rien), journal, verrouillage, suppression protégée, moyennes. */
class EvaluationServiceTest {

    private final Map<String, EvaluationJpaEntity> evals = new LinkedHashMap<>();
    private final Map<String, NoteEvaluationJpaEntity> notes = new LinkedHashMap<>();
    private final List<JournalNoteJpaEntity> journal = new ArrayList<>();

    private final EvaluationJpaRepository evalRepo = Mockito.mock(EvaluationJpaRepository.class);
    private final NoteEvaluationJpaRepository noteRepo = Mockito.mock(NoteEvaluationJpaRepository.class);
    private final JournalNoteJpaRepository journalRepo = Mockito.mock(JournalNoteJpaRepository.class);
    private final CourseJpaRepository coursRepo = Mockito.mock(CourseJpaRepository.class);
    private final InscriptionClasseJpaRepository inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
    private final ComptesEcoleService comptes = Mockito.mock(ComptesEcoleService.class);
    private EvaluationService service;

    private static Map.Entry<UserJpaEntity, OrganizationUserJpaEntity> eleve(String id, String nom) {
        UserJpaEntity u = new UserJpaEntity(id, nom, "Prénom", id + "@ex.sn", null, "ETUDIANT", "MAT-" + id, null);
        OrganizationUserJpaEntity l = new OrganizationUserJpaEntity();
        l.setUserId(id);
        l.setStatus("ACTIVE");
        return Map.entry(u, l);
    }

    @BeforeEach
    void preparer() {
        User prof = UserFactory.reconstructSimple("prof-a", "Ba", "Mariama", "prof@ex.sn", null, "M", RoleUtilisateur.PROFESSEUR, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(prof, null, prof.getAuthorities()));

        Mockito.when(coursRepo.findById("c1")).thenReturn(Optional.of(new CourseJpaEntity("c1", "Algo", "classe-1", 40, "prof-a")));
        Mockito.when(inscriptions.findByClasseId("classe-1")).thenReturn(List.of(
                new InscriptionClasseJpaEntity("i1", "e1", "classe-1"), new InscriptionClasseJpaEntity("i2", "e2", "classe-1")));
        // e3 est un élève de l'école mais pas de la classe du cours
        Mockito.when(comptes.membres("ETUDIANT")).thenReturn(List.of(eleve("e1", "Diop"), eleve("e2", "Fall"), eleve("e3", "Sarr")));

        Mockito.when(evalRepo.save(any())).thenAnswer(i -> { EvaluationJpaEntity e = i.getArgument(0); evals.put(e.getId(), e); return e; });
        Mockito.when(evalRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(evals.get(i.<String>getArgument(0))));
        Mockito.when(evalRepo.findByCourseIdOrderByDateEvaluationAscCreeLeAsc(anyString())).thenAnswer(i ->
                evals.values().stream().filter(e -> e.getCourseId().equals(i.getArgument(0))).toList());
        Mockito.when(evalRepo.existsByEvaluationRattrapeeId(anyString())).thenAnswer(i ->
                evals.values().stream().anyMatch(e -> i.getArgument(0).equals(e.getEvaluationRattrapeeId())));
        Mockito.doAnswer(i -> { evals.remove(i.<EvaluationJpaEntity>getArgument(0).getId()); return null; }).when(evalRepo).delete(any());

        Mockito.when(noteRepo.save(any())).thenAnswer(i -> { NoteEvaluationJpaEntity n = i.getArgument(0); notes.put(n.getId(), n); return n; });
        Mockito.when(noteRepo.findByEvaluationId(anyString())).thenAnswer(i ->
                notes.values().stream().filter(n -> n.getEvaluationId().equals(i.getArgument(0))).toList());
        Mockito.when(noteRepo.findByCourseId(anyString())).thenAnswer(i ->
                notes.values().stream().filter(n -> n.getCourseId().equals(i.getArgument(0))).toList());
        Mockito.when(noteRepo.countByEvaluationId(anyString())).thenAnswer(i ->
                notes.values().stream().filter(n -> n.getEvaluationId().equals(i.getArgument(0))).count());
        Mockito.doAnswer(i -> { notes.remove(i.<NoteEvaluationJpaEntity>getArgument(0).getId()); return null; }).when(noteRepo).delete(any());

        Mockito.when(journalRepo.save(any())).thenAnswer(i -> { journal.add(i.getArgument(0)); return i.getArgument(0); });
        Mockito.when(journalRepo.findByEvaluationIdOrderByHorodatageDesc(anyString())).thenAnswer(i -> {
            List<JournalNoteJpaEntity> l = new ArrayList<>(journal.stream().filter(j -> j.getEvaluationId().equals(i.getArgument(0))).toList());
            Collections.reverse(l);
            return l;
        });

        service = new EvaluationService(evalRepo, noteRepo, journalRepo, coursRepo, inscriptions, comptes);
    }

    @AfterEach
    void fin() {
        SecurityContextHolder.clearContext();
    }

    private String creer(String titre, double coefficient) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("titre", titre);
        corps.put("type", "DEVOIR_SURVEILLE");
        corps.put("date", LocalDate.now().minusDays(3).toString());
        corps.put("coefficient", coefficient);
        return (String) service.creer("c1", corps).get("id");
    }

    private static Map<String, Object> ligne(String studentId, Object valeur) {
        Map<String, Object> m = new HashMap<>();
        m.put("studentId", studentId);
        m.put("valeur", valeur);
        return m;
    }

    private static int statut(Runnable r) {
        return assertThrows(ResponseStatusException.class, r::run).getStatusCode().value();
    }

    @Test
    void creationValideEtErreursHumaines() {
        String id = creer("DS 1", 2);
        assertEquals(20.0, evals.get(id).getNoteSur());
        assertEquals("NORMALE", evals.get(id).getSession());
        assertEquals(400, statut(() -> service.creer("c1", new HashMap<>(Map.of("titre", "X", "type", "TP", "date", "2026-01-01", "coefficient", 0)))));
        assertEquals(400, statut(() -> service.creer("c1", new HashMap<>(Map.of("titre", "X", "type", "TP", "date", "2026-01-01", "coefficient", 11)))));
        assertEquals(400, statut(() -> service.creer("c1", new HashMap<>(Map.of("titre", "X", "type", "QUIZZ", "date", "2026-01-01", "coefficient", 1)))));
        assertEquals(400, statut(() -> service.creer("c1", new HashMap<>(Map.of("titre", "X", "type", "TP", "date", "demain", "coefficient", 1)))));
        assertEquals(400, statut(() -> service.creer("c1", new HashMap<>(Map.of("titre", "R", "type", "EXAMEN", "date", "2026-01-01", "coefficient", 1, "session", "RATTRAPAGE")))));
    }

    @Test
    void lotToutOuRien() {
        String id = creer("DS 1", 1);
        // e3 n'est pas dans la classe, e2 a 25/20 → RIEN n'est enregistré, pas même e1
        var e = assertThrows(ResponseStatusException.class, () -> service.enregistrerNotes("c1", id,
                List.of(ligne("e1", 12), ligne("e2", 25), ligne("e3", 10))));
        assertEquals(400, e.getStatusCode().value());
        assertTrue(e.getReason().contains("Ligne 2") && e.getReason().contains("Ligne 3"), e.getReason());
        assertTrue(notes.isEmpty());
        assertTrue(journal.isEmpty());
    }

    @Test
    void journalCreationModificationSuppression() {
        String id = creer("DS 1", 1);
        service.enregistrerNotes("c1", id, List.of(ligne("e1", 12), Map.of("studentId", "e2", "absence", "ABS")));
        service.enregistrerNotes("c1", id, List.of(ligne("e1", 14.5), Map.of("studentId", "e2", "absence", "ABS")));   // e2 inchangé
        service.enregistrerNotes("c1", id, List.of(ligne("e1", null)));
        List<Map<String, Object>> j = service.journal("c1", id);
        assertEquals(4, j.size());
        assertEquals("SUPPRESSION", j.get(0).get("action"));
        assertEquals("14.5", j.get(0).get("ancienneValeur"));
        assertNull(j.get(0).get("nouvelleValeur"));
        assertEquals("MODIFICATION", j.get(1).get("action"));
        assertEquals("12", j.get(1).get("ancienneValeur"));
        assertEquals("14.5", j.get(1).get("nouvelleValeur"));
        assertEquals("prof-a", j.get(1).get("auteurId"));
        assertEquals("Mariama Ba", j.get(1).get("auteur"));
        assertEquals("ABS", j.get(2).get("nouvelleValeur"));
        assertEquals("CREATION", j.get(3).get("action"));
    }

    @Test
    void verrouillageRefuseToutesLesModifications() {
        String id = creer("DS 1", 1);
        service.enregistrerNotes("c1", id, List.of(ligne("e1", 12)));
        service.verrouiller("c1", id, true);
        assertEquals(409, statut(() -> service.enregistrerNotes("c1", id, List.of(ligne("e1", 15)))));
        assertEquals(409, statut(() -> service.modifier("c1", id, new HashMap<>(Map.of("titre", "Autre")))));
        assertEquals(409, statut(() -> service.supprimer("c1", id)));
        assertEquals(12.0, notes.values().iterator().next().getValeur());
        assertEquals("VERROUILLAGE", service.journal("c1", id).get(0).get("action"));
        service.verrouiller("c1", id, false);
        service.enregistrerNotes("c1", id, List.of(ligne("e1", 15)));
        assertEquals(15.0, notes.values().iterator().next().getValeur());
    }

    @Test
    void suppressionRefuseeSiDesNotesExistent() {
        String id = creer("DS 1", 1);
        service.enregistrerNotes("c1", id, List.of(ligne("e1", 12)));
        assertEquals(409, statut(() -> service.supprimer("c1", id)));
        String vide = creer("DS 2", 1);
        service.supprimer("c1", vide);
        assertFalse(evals.containsKey(vide));
    }

    @Test
    void evaluationDUnAutreCoursEstIntrouvable() {
        String id = creer("DS 1", 1);
        assertEquals(404, statut(() -> service.notes("c2", id)));
    }

    @Test
    void notesAvantLaDateDeLEvaluationRefusees() {
        String id = (String) service.creer("c1", new HashMap<>(Map.of("titre", "Futur", "type", "EXAMEN",
                "date", LocalDate.now().plusDays(5).toString(), "coefficient", 1))).get("id");
        assertEquals(400, statut(() -> service.enregistrerNotes("c1", id, List.of(ligne("e1", 12)))));
    }

    @Test
    void moyennesPondereesMentionRangEtElevesSansNote() {
        String ds = creer("DS", 1);
        String exam = creer("Examen", 3);
        service.enregistrerNotes("c1", ds, List.of(ligne("e1", 8)));
        service.enregistrerNotes("c1", exam, List.of(ligne("e1", 16)));
        Map<String, Object> r = service.moyennes("c1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eleves = (List<Map<String, Object>>) r.get("eleves");
        assertEquals(2, eleves.size());   // e3 (hors classe) absent
        Map<String, Object> e1 = eleves.stream().filter(x -> "e1".equals(x.get("studentId"))).findFirst().orElseThrow();
        Map<String, Object> e2 = eleves.stream().filter(x -> "e2".equals(x.get("studentId"))).findFirst().orElseThrow();
        assertEquals(14.0, e1.get("moyenne"));   // (8×1 + 16×3) / 4
        assertEquals("Bien", e1.get("mention"));
        assertEquals(1, e1.get("rang"));
        assertNull(e2.get("moyenne"));
        assertNull(e2.get("mention"));
        assertNull(e2.get("rang"));
    }
}
