package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.media.application.port.usecase.UploadMediaUseCase;
import ecole_221.sn.ecole.modules.media.domain.exception.InvalidFileException;
import ecole_221.sn.ecole.modules.media.domain.model.UploadedFileFactory;
import ecole_221.sn.ecole.modules.notifications.application.port.usecase.NotificationUseCase;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Lot B : rendus, correction (0–20, commentaire ≤ 2000), élève hors classe, devoir d'un autre cours, notifications. */
class RendusDevoirsServiceTest {

    private static final byte[] PDF = "%PDF-1.4\n%minimal\n".getBytes();

    private HomeworkJpaRepository devoirs;
    private CourseJpaRepository cours;
    private RenduDevoirJpaRepository rendus;
    private InscriptionClasseJpaRepository inscriptions;
    private ComptesEcoleService comptes;
    private UploadMediaUseCase stockage;
    private NotificationUseCase notifications;
    private RendusDevoirsService service;
    private final Map<String, RenduDevoirJpaEntity> base = new HashMap<>();

    @BeforeEach
    void preparer() throws Exception {
        devoirs = mock(HomeworkJpaRepository.class);
        cours = mock(CourseJpaRepository.class);
        rendus = mock(RenduDevoirJpaRepository.class);
        inscriptions = mock(InscriptionClasseJpaRepository.class);
        comptes = mock(ComptesEcoleService.class);
        stockage = mock(UploadMediaUseCase.class);
        notifications = mock(NotificationUseCase.class);
        service = new RendusDevoirsService(devoirs, cours, rendus, inscriptions, comptes, stockage,
                new NotificationsDevoirsService(notifications));

        CourseJpaEntity c = new CourseJpaEntity("cours-1", "Algorithmique", "classe-1", 40, "prof-1");
        when(cours.findById("cours-1")).thenReturn(Optional.of(c));
        when(cours.findByClasse("classe-1")).thenReturn(List.of(c));
        HomeworkJpaEntity d = devoir("hw-1", "cours-1", LocalDate.now().plusDays(3).toString());
        when(devoirs.findById("hw-1")).thenReturn(Optional.of(d));
        when(devoirs.findById("hw-autre")).thenReturn(Optional.of(devoir("hw-autre", "cours-2", "2099-01-01")));
        when(devoirs.findByCourseId("cours-1")).thenReturn(List.of(d));
        when(inscriptions.findByClasseId("classe-1")).thenReturn(List.of(
                new InscriptionClasseJpaEntity("i1", "eleve-1", "classe-1"), new InscriptionClasseJpaEntity("i2", "eleve-2", "classe-1")));
        when(inscriptions.findByUserId("eleve-1")).thenReturn(List.of(new InscriptionClasseJpaEntity("i1", "eleve-1", "classe-1")));
        when(comptes.membres("ETUDIANT")).thenReturn(List.of(membre("eleve-1", "Ba"), membre("eleve-2", "Sy"), membre("eleve-3", "Diop")));

        when(rendus.findByHomeworkId(anyString())).thenAnswer(i -> base.values().stream()
                .filter(r -> r.getHomeworkId().equals(i.getArgument(0))).toList());
        when(rendus.findByHomeworkIdAndStudentId(anyString(), anyString())).thenAnswer(i -> base.values().stream()
                .filter(r -> r.getHomeworkId().equals(i.getArgument(0)) && r.getStudentId().equals(i.getArgument(1))).findFirst());
        when(rendus.findByStudentId(anyString())).thenAnswer(i -> base.values().stream()
                .filter(r -> r.getStudentId().equals(i.getArgument(0))).toList());
        when(rendus.save(any())).thenAnswer(i -> { RenduDevoirJpaEntity r = i.getArgument(0); base.put(r.getId(), r); return r; });
        when(stockage.upload(any(), anyString(), anyString())).thenAnswer(i ->
                UploadedFileFactory.create(i.getArgument(1), "https://stockage.exemple/rendu.pdf", "pid", ((byte[]) i.getArgument(0)).length, i.getArgument(2)));
    }

    private static HomeworkJpaEntity devoir(String id, String courseId, String limite) {
        HomeworkJpaEntity d = new HomeworkJpaEntity();
        d.setId(id); d.setCourseId(courseId); d.setTitle("TP tri"); d.setDeadline(limite); d.setPriority("HIGH");
        return d;
    }

    private static Map.Entry<UserJpaEntity, OrganizationUserJpaEntity> membre(String id, String nom) {
        UserJpaEntity u = new UserJpaEntity();
        u.setId(id); u.setNom(nom); u.setPrenom("P");
        OrganizationUserJpaEntity o = new OrganizationUserJpaEntity();
        o.setStatus("ACTIVE");
        return Map.entry(u, o);
    }

    @Test
    void listeTousLesElevesDeLaClasseAvecRenduOuNonRendu() {
        service.deposer("eleve-1", "cours-1", "hw-1", "copie.pdf", "application/pdf", PDF);
        Map<String, Object> r = service.rendus("cours-1", "hw-1");
        @SuppressWarnings("unchecked") List<Map<String, Object>> eleves = (List<Map<String, Object>>) r.get("eleves");
        assertEquals(2, eleves.size(), "seuls les élèves AFFECTÉS à la classe (pas eleve-3)");
        assertEquals("RENDU", eleves.get(0).get("statut"));
        assertEquals("NON_RENDU", eleves.get(1).get("statut"));
        @SuppressWarnings("unchecked") Map<String, Object> rendu = (Map<String, Object>) eleves.get(0).get("rendu");
        assertEquals("https://stockage.exemple/rendu.pdf", rendu.get("fichierUrl"));
        @SuppressWarnings("unchecked") Map<String, Object> stats = (Map<String, Object>) r.get("statistiques");
        assertEquals(2, stats.get("attendus"));
        assertEquals(1, stats.get("rendus"));
    }

    @Test
    void corrigeEtNotifieLEleve_puisLEleveRelitSaNote() {
        Map<String, Object> r = service.corriger("cours-1", "hw-1", "eleve-1", Map.of("note", 15.5, "commentaire", "Bon travail"), "prof-1");
        assertEquals(15.5, r.get("note"));
        verify(notifications).sendNotification(eq("eleve-1"), eq("ETUDIANT"), contains("TP tri"), contains("15,5/20"), anyString(), anyString());
        List<Map<String, Object>> mes = service.devoirsEleve("eleve-1");
        @SuppressWarnings("unchecked") Map<String, Object> corr = (Map<String, Object>) mes.get(0).get("correction");
        assertEquals(15.5, corr.get("note"));
        assertEquals("Bon travail", corr.get("commentaire"));
    }

    @Test
    void echecDeNotificationNAnnulePasLaCorrection() {
        doThrow(new RuntimeException("panne")).when(notifications).sendNotification(any(), any(), any(), any(), any(), any());
        Map<String, Object> r = service.corriger("cours-1", "hw-1", "eleve-2", Map.of("note", 12), "prof-1");
        assertEquals(12.0, r.get("note"));
        @SuppressWarnings("unchecked") Map<String, Object> n = (Map<String, Object>) r.get("notification");
        assertEquals(0, n.get("envoyees"));
        assertTrue(String.valueOf(n.get("message")).contains("échoué"));
        assertEquals(1, base.size(), "correction bien enregistrée");
    }

    @Test
    void refuseLesNotesHorsBornesEtLesCommentairesTropLongs() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> RendusDevoirsService.noteValide(25)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> RendusDevoirsService.noteValide(-1));
        assertThrows(ResponseStatusException.class, () -> RendusDevoirsService.noteValide(null));
        assertThrows(ResponseStatusException.class, () -> RendusDevoirsService.noteValide("quinze"));
        assertEquals(12.5, RendusDevoirsService.noteValide("12,5"));
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> RendusDevoirsService.commentaireValide("x".repeat(2001)));
        assertTrue(e.getReason().contains("2000"));
        assertNull(RendusDevoirsService.commentaireValide("  "));
        verifyNoInteractions(rendus);
    }

    @Test
    void refuseLEleveHorsClasseEtLeDevoirDUnAutreCours() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.corriger("cours-1", "hw-1", "eleve-3", Map.of("note", 10), "prof-1")).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.corriger("cours-1", "hw-autre", "eleve-1", Map.of("note", 10), "prof-1")).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.rendus("cours-1", "hw-autre")).getStatusCode().value());
    }

    @Test
    void depot_refuseEleveHorsClasse_fichierNonConforme_etApresCorrection() {
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.deposer("eleve-3", "cours-1", "hw-1", "c.pdf", "application/pdf", PDF)).getStatusCode().value());
        assertThrows(InvalidFileException.class,
                () -> service.deposer("eleve-1", "cours-1", "hw-1", "c.pdf", "application/pdf", "<html>pas un pdf</html>".getBytes()));
        assertThrows(InvalidFileException.class,
                () -> service.deposer("eleve-1", "cours-1", "hw-1", "c.exe", "application/x-msdownload", PDF));
        service.corriger("cours-1", "hw-1", "eleve-1", Map.of("note", 10), "prof-1");
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.deposer("eleve-1", "cours-1", "hw-1", "c.pdf", "application/pdf", PDF)).getStatusCode().value());
    }

    @Test
    void renduApresLaDateLimiteEstEnRetard() {
        assertTrue(RendusDevoirsService.enRetard(LocalDateTime.now(), LocalDate.now().minusDays(1)));
        assertFalse(RendusDevoirsService.enRetard(LocalDateTime.now(), LocalDate.now()));
        assertFalse(RendusDevoirsService.enRetard(null, LocalDate.now()));
    }

    @Test
    void nouveauDevoirNotifieChaqueEleveDeLaClasse() {
        Map<String, Object> r = service.notifierNouveauDevoir("cours-1", "TP tri", "2099-01-01");
        assertEquals(2, r.get("envoyees"));
        verify(notifications, times(2)).sendNotification(anyString(), eq("ETUDIANT"), contains("TP tri"), anyString(), anyString(), anyString());
    }
}
