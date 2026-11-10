package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.PresenceSeanceJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;

/** Règles de la feuille d'appel par séance (lot A, 2026-11-10). */
class FeuilleAppelServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 10);

    private ScheduleSessionJpaRepository seances;
    private InscriptionClasseJpaRepository inscriptions;
    private PresenceSeanceJpaRepository presences;
    private ComptesEcoleService comptes;
    private FeuilleAppelService service;
    private final CourseJpaEntity cours = new CourseJpaEntity("c-a", "Algo", "cl-1", 30, "prof-a");

    @BeforeEach
    void preparer() {
        seances = Mockito.mock(ScheduleSessionJpaRepository.class);
        inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
        presences = Mockito.mock(PresenceSeanceJpaRepository.class);
        comptes = Mockito.mock(ComptesEcoleService.class);
        Clock horloge = Clock.fixed(AUJOURDHUI.atTime(9, 30).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new FeuilleAppelService(seances, inscriptions, presences, comptes, horloge);

        Mockito.when(inscriptions.findByClasseId("cl-1")).thenReturn(List.of(
                new InscriptionClasseJpaEntity("i1", "e1", "cl-1"), new InscriptionClasseJpaEntity("i2", "e2", "cl-1")));
        Mockito.when(comptes.membres("ETUDIANT")).thenReturn(List.of(
                membre("e1", "Kane", "Fatou"), membre("e2", "Ba", "Awa"), membre("e3", "Hors", "Classe")));
        ScheduleSessionJpaEntity s = new ScheduleSessionJpaEntity();
        s.setId("ses-1");
        s.setCourseId("c-a");
        ScheduleSessionJpaEntity autre = new ScheduleSessionJpaEntity();
        autre.setId("ses-b");
        autre.setCourseId("c-b");
        Mockito.when(seances.findById("ses-1")).thenReturn(Optional.of(s));
        Mockito.when(seances.findById("ses-b")).thenReturn(Optional.of(autre));
        Mockito.when(seances.findById("ses-x")).thenReturn(Optional.empty());
        Mockito.when(presences.findByCourseIdAndDate(any(), any())).thenReturn(List.of());
    }

    private static Map.Entry<UserJpaEntity, OrganizationUserJpaEntity> membre(String id, String nom, String prenom) {
        OrganizationUserJpaEntity lien = new OrganizationUserJpaEntity();
        lien.setStatus("ACTIVE");
        return Map.entry(new UserJpaEntity(id, nom, prenom, id + "@ex.sn", null, "ETUDIANT", null, null), lien);
    }

    private static Map<String, Object> corps(String sessionId, String date, List<Map<String, Object>> lignes) {
        Map<String, Object> c = new HashMap<>();
        c.put("sessionId", sessionId);
        c.put("date", date);
        c.put("entries", lignes);
        return c;
    }

    private static Map<String, Object> ligne(String eleve, String statut) {
        Map<String, Object> l = new HashMap<>();
        l.put("studentId", eleve);
        l.put("status", statut);
        return l;
    }

    private int statutHttp(Runnable r) {
        return assertThrows(ResponseStatusException.class, r::run).getStatusCode().value();
    }

    @Test
    void dateFutureRefusee() {
        assertEquals(400, statutHttp(() -> service.enregistrer(cours, corps("ses-1", "2026-11-11", List.of(ligne("e1", "PRESENT"))), "prof-a")));
        Mockito.verify(presences, Mockito.never()).saveAll(anyIterable());
    }

    @Test
    void dateManquanteOuIllisibleRefusee() {
        assertEquals(400, statutHttp(() -> service.enregistrer(cours, corps("ses-1", null, List.of(ligne("e1", "PRESENT"))), "p")));
        assertEquals(400, statutHttp(() -> service.feuille(cours, "ses-1", "mardi")));
    }

    @Test
    void statutInconnuRefuseEtRienNEstEcrit() {
        assertEquals(400, statutHttp(() -> service.enregistrer(cours,
                corps("ses-1", "2026-11-10", List.of(ligne("e1", "PRESENT"), ligne("e2", "PEUT_ETRE"))), "p")));
        Mockito.verify(presences, Mockito.never()).saveAll(anyIterable());
        Mockito.verify(presences, Mockito.never()).deleteAll(anyIterable());
    }

    @Test
    void eleveHorsClasseRefuse() {
        assertEquals(400, statutHttp(() -> service.enregistrer(cours, corps("ses-1", "2026-11-10", List.of(ligne("e3", "ABSENT"))), "p")));
    }

    @Test
    void memeEleveDeuxFoisRefuse() {
        assertEquals(400, statutHttp(() -> service.enregistrer(cours,
                corps("ses-1", "2026-11-10", List.of(ligne("e1", "ABSENT"), ligne("e1", "PRESENT"))), "p")));
    }

    @Test
    void feuilleVideRefusee() {
        assertEquals(400, statutHttp(() -> service.enregistrer(cours, corps("ses-1", "2026-11-10", List.of()), "p")));
    }

    @Test
    void seanceDUnAutreCoursRefuseeEtSeanceInconnueIntrouvable() {
        assertEquals(400, statutHttp(() -> service.enregistrer(cours, corps("ses-b", "2026-11-10", List.of(ligne("e1", "PRESENT"))), "p")));
        assertEquals(404, statutHttp(() -> service.feuille(cours, "ses-x", "2026-11-10")));
    }

    @Test
    void nouvelleSaisieRemplaceLAncienneDeLaMemeSeance() {
        AttendanceJpaEntity ancienne = new AttendanceJpaEntity();
        ancienne.setId("old");
        ancienne.setCourseId("c-a");
        ancienne.setSessionId("ses-1");
        ancienne.setDate("2026-11-10");
        ancienne.setStudentId("e1");
        ancienne.setStatus("ABSENT");
        AttendanceJpaEntity autreSeance = new AttendanceJpaEntity();
        autreSeance.setId("keep");
        autreSeance.setCourseId("c-a");
        autreSeance.setSessionId("ses-2");
        autreSeance.setDate("2026-11-10");
        autreSeance.setStudentId("e1");
        autreSeance.setStatus("PRESENT");
        Mockito.when(presences.findByCourseIdAndDate("c-a", "2026-11-10")).thenReturn(List.of(ancienne, autreSeance));

        Map<String, Object> l = ligne("e1", "retard");
        l.put("note", "Bus en retard");
        Map<String, Object> res = service.enregistrer(cours, corps("ses-1", "2026-11-10", List.of(l, ligne("e2", "EXCUSE"))), "prof-a");

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Iterable<AttendanceJpaEntity>> supprimees = ArgumentCaptor.forClass((Class) Iterable.class);
        Mockito.verify(presences).deleteAll(supprimees.capture());
        List<String> ids = new ArrayList<>();
        supprimees.getValue().forEach(a -> ids.add(a.getId()));
        assertEquals(List.of("old"), ids, "seule la saisie de la même séance est remplacée");

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Iterable<AttendanceJpaEntity>> ecrites = ArgumentCaptor.forClass((Class) Iterable.class);
        Mockito.verify(presences).saveAll(ecrites.capture());
        List<AttendanceJpaEntity> nouvelles = new ArrayList<>();
        ecrites.getValue().forEach(nouvelles::add);
        assertEquals(2, nouvelles.size());
        assertEquals("LATE", nouvelles.get(0).getStatus());
        assertEquals("Bus en retard", nouvelles.get(0).getNote());
        assertEquals("ses-1", nouvelles.get(0).getSessionId());
        assertEquals("EXCUSED", nouvelles.get(1).getStatus());
        assertEquals(1, res.get("lignesRemplacees"));
        assertNotNull(res.get("enregistreA"));
    }

    @Test
    void feuilleSansSaisieAfficheNonNoteEtNeListeQueLaClasse() {
        Map<String, Object> f = service.feuille(cours, "ses-1", "2026-11-10");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eleves = (List<Map<String, Object>>) f.get("eleves");
        assertEquals(2, eleves.size());
        assertEquals("Awa Ba", eleves.get(0).get("nom"));
        assertTrue(eleves.stream().allMatch(e -> e.get("status") == null));
        assertNull(f.get("enregistreA"));
    }

    @Test
    void tauxDAssiduite() {
        assertNull(FeuilleAppelService.taux(0, 0));
        assertEquals(75.0, FeuilleAppelService.taux(3, 4));
        assertEquals(66.7, FeuilleAppelService.taux(2, 3));
    }

    @Test
    void statistiquesDedoublonneesEtNullSansSeance() {
        List<AttendanceJpaEntity> toutes = new ArrayList<>();
        String[][] donnees = {{"ses-1", "2026-11-03", "PRESENT"}, {"ses-1", "2026-11-10", "LATE"}, {"ses-1", "2026-11-10", "ABSENT"},
                {null, "2026-11-04", "RETARD"}, {"ses-1", "2026-11-05", "EXCUSED"}};
        int n = 0;
        for (String[] d : donnees) {
            AttendanceJpaEntity a = new AttendanceJpaEntity();
            a.setId("a" + n);
            a.setCourseId("c-a");
            a.setSessionId(d[0]);
            a.setDate(d[1]);
            a.setStudentId("e1");
            a.setStatus(d[2]);
            a.setRecordedAt(LocalDateTime.of(2026, 11, 10, 8, n++));
            toutes.add(a);
        }
        Mockito.when(presences.findByCourseId("c-a")).thenReturn(toutes);
        Map<String, Object> s = service.statistiques(cours);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eleves = (List<Map<String, Object>>) s.get("eleves");
        Map<String, Object> fatou = eleves.stream().filter(e -> "e1".equals(e.get("studentId"))).findFirst().orElseThrow();
        assertEquals(1, fatou.get("present"));
        assertEquals(1, fatou.get("retard"));
        assertEquals(1, fatou.get("absent"), "la saisie la plus récente remplace la précédente");
        assertEquals(1, fatou.get("excuse"));
        assertEquals(4, fatou.get("seancesNotees"));
        assertEquals(50.0, fatou.get("taux"));
        Map<String, Object> awa = eleves.stream().filter(e -> "e2".equals(e.get("studentId"))).findFirst().orElseThrow();
        assertNull(awa.get("taux"));
    }
}
