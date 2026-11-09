package ecole_221.sn.ecole.modules.collaboration.application.service;

import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** APP-BE-14 : tous les champs d'une classe virtuelle sont conservés (aller-retour POST → GET). */
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
                "isPublished", true, "restrictToGoogleAccount", true));

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
        assertEquals(true, relu.get("restrictToGoogleAccount"));
        // compatibilité avec l'ancienne forme (title / link)
        assertEquals("Algorithmique", relu.get("title"));
        assertEquals("https://meet.google.com/abc-defg-hij", relu.get("link"));
    }
}
