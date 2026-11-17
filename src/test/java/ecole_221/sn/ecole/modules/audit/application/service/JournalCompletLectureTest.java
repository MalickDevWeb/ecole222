package ecole_221.sn.ecole.modules.audit.application.service;

import ecole_221.sn.ecole.modules.audit.application.port.dto.AuditLogDto;
import ecole_221.sn.ecole.modules.audit.domain.port.repository.AuditRepositoryPort;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.AuditLogJpaEntity;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.AuditLogJpaRepository;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.SecurityEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GET /audit/logs : une entrée anonyme (candidature déposée sans compte) n'a pas d'auteur ; la lecture complète du
 * journal la renvoie sans passer par l'objet métier AuditLog, qui exige un auteur.
 */
class JournalCompletLectureTest {

    private static AuditLogJpaEntity entree(String id, String acteur, String nom, String role, LocalDateTime quand) {
        AuditLogJpaEntity e = new AuditLogJpaEntity();
        e.setId(id);
        e.setActorId(acteur);
        e.setActorName(nom);
        e.setActorRole(role);
        e.setActionType("CREATE");
        e.setActionDescription("Création ou action — POST /candidatures/submit");
        e.setActionTimestamp(quand);
        return e;
    }

    @Test
    void journalCompletLisibleAvecUneEntreeAnonymeEtAuteursNommes() {
        AuditLogJpaRepository depot = mock(AuditLogJpaRepository.class);
        LocalDateTime t = LocalDateTime.of(2026, 11, 14, 11, 0);
        when(depot.findAll(any(Sort.class))).thenReturn(List.of(
                entree("aud-2", null, "anonyme (formulaire public)", null, t.plusMinutes(1)),
                entree("aud-1", "u-1", "Admin Test", "ADMIN", t)));
        AuditUseCaseImpl service = new AuditUseCaseImpl(mock(AuditRepositoryPort.class), depot, mock(SecurityEventJpaRepository.class));

        List<AuditLogDto> journal = service.getAllAuditLogs();

        assertThat(journal).extracting(AuditLogDto::id).containsExactly("aud-2", "aud-1");
        assertThat(journal.get(0).userId()).isNull();
        assertThat(journal.get(0).actorName()).startsWith("anonyme");
        assertThat(journal.get(1).actorName()).isEqualTo("Admin Test");
        assertThat(journal.get(1).actorRole()).isEqualTo("ADMIN");
        verify(depot).findAll(Sort.by(Sort.Direction.DESC, "actionTimestamp"));
    }
}
