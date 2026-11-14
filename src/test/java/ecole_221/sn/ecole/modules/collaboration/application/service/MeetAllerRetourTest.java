package ecole_221.sn.ecole.modules.collaboration.application.service;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.collaboration.application.port.usecase.CollaborationUseCase;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.web.CollaborationController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APP-BE-14 : tous les champs d'une classe virtuelle sont conservés (aller-retour POST → GET).
 * APP-TL-26 (2026-11-14) : plus de champ « compte Google obligatoire » (enregistré sans effet) ; seul un lien
 * Google Meet est accepté par POST /collaboration/meets, comme pour les cours en direct.
 */
class MeetAllerRetourTest {

    @Test
    void champsDeLaClasseVirtuelleConserves() {
        MeetJpaRepository repo = Mockito.mock(MeetJpaRepository.class);
        CollaborationService service = new CollaborationService(
                Mockito.mock(ecole_221.sn.ecole.modules.collaboration.domain.port.repository.CollaborationRepositoryPort.class),
                Mockito.mock(WorkgroupJpaRepository.class), Mockito.mock(DocumentJpaRepository.class),
                Mockito.mock(TaskJpaRepository.class), Mockito.mock(MessageJpaRepository.class), repo);

        service.createMeet(Map.of(
                "subjectName", "Algorithmique", "meetLink", "https://meet.google.com/abc-defg-hij",
                "date", "2026-11-12", "time", "10:00", "teacherName", "Awa Ndiaye",
                "classIds", List.of("c-l1", "c-l2"), "classNames", List.of("L1 GL", "L2 GL"),
                "isPublished", true));

        ArgumentCaptor<MeetJpaEntity> enregistre = ArgumentCaptor.forClass(MeetJpaEntity.class);
        Mockito.verify(repo).save(enregistre.capture());
        Mockito.when(repo.findAll()).thenReturn(List.of(enregistre.getValue()));

        Map<String, Object> relu = service.getMeets().get(0);
        assertEquals("Algorithmique", relu.get("subjectName"));
        assertEquals("https://meet.google.com/abc-defg-hij", relu.get("meetLink"));
        assertEquals("2026-11-12", relu.get("date"));
        assertEquals("10:00", relu.get("time"));
        assertEquals("Awa Ndiaye", relu.get("teacherName"));
        assertEquals(List.of("c-l1", "c-l2"), relu.get("classIds"));
        assertEquals(List.of("L1 GL", "L2 GL"), relu.get("classNames"));
        assertEquals(true, relu.get("isPublished"));
        // APP-TL-26 : le champ n'est plus renvoyé (l'application ne peut pas imposer un compte Google)
        assertFalse(relu.containsKey("restrictToGoogleAccount"));
        // compatibilité avec l'ancienne forme (title / link)
        assertEquals("Algorithmique", relu.get("title"));
        assertEquals("https://meet.google.com/abc-defg-hij", relu.get("link"));
    }

    private static Map<String, Object> corps(String cle, String lien) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("subjectName", "Algorithmique");
        corps.put(cle, lien);
        corps.put("classIds", List.of("c-l1"));
        corps.put("classNames", List.of("L1 GL"));
        corps.put("isPublished", true);
        return corps;
    }

    @Test
    void lienQuiNEstPasUnLienMeetRefuse() {
        CollaborationUseCase useCase = Mockito.mock(CollaborationUseCase.class);
        CollaborationController controleur = new CollaborationController(useCase, Mockito.mock(ActionLogger.class));

        for (String lien : List.of(
                "https://exemple-malveillant.com/meet.google.com/abc-defg-hij",   // domaine piégé
                "http://meet.google.com/abc-defg-hij",                            // sans https
                "https://meet.google.com/",                                        // sans code de réunion
                "https://zoom.us/j/123456789",                                     // autre outil
                "javascript:alert(1)")) {
            for (String cle : List.of("meetLink", "link")) {
                ResponseStatusException e = assertThrows(ResponseStatusException.class,
                        () -> controleur.createMeet(corps(cle, lien)), lien);
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode(), lien);
                assertTrue(e.getReason() != null && e.getReason().contains("meet.google.com"), e.getReason());
            }
        }
        Mockito.verify(useCase, Mockito.never()).createMeet(Mockito.<Map<String, Object>>any());
    }

    @Test
    void lienMeetAccepteEtCompteGoogleIgnore() {
        CollaborationUseCase useCase = Mockito.mock(CollaborationUseCase.class);
        CollaborationController controleur = new CollaborationController(useCase, Mockito.mock(ActionLogger.class));

        Map<String, Object> corps = corps("meetLink", "https://meet.google.com/abc-defg-hij");
        corps.put("restrictToGoogleAccount", true);   // ancien champ : ignoré, plus enregistré
        assertEquals(HttpStatus.OK, controleur.createMeet(corps).getStatusCode());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> transmis = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(useCase).createMeet(transmis.capture());
        assertEquals("https://meet.google.com/abc-defg-hij", transmis.getValue().get("meetLink"));
        assertFalse(transmis.getValue().containsKey("restrictToGoogleAccount"));
    }
}
