package ecole_221.sn.ecole.modules.sync.application.service;

import ecole_221.sn.ecole.modules.admin.application.port.usecase.AdminUseCase;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase;
import ecole_221.sn.ecole.modules.security.application.port.usecase.SecurityUseCase;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncAction;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncResult;
import ecole_221.sn.ecole.modules.sync.domain.valueobject.SyncActionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Lot /sync : chaque écriture réussie du lot est inscrite au journal d'audit (ENF-07, 2026-11-14). */
class SyncJournalAuditTest {

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    /** SyncActionType n'accepte que CREATE/UPDATE/DELETE/SYNC : on simule le type métier envoyé par le client. */
    private static SyncAction action(String id, String type, Map<String, Object> payload) {
        SyncActionType t = mock(SyncActionType.class);
        when(t.name()).thenReturn(type);
        SyncAction a = mock(SyncAction.class);
        when(a.getId()).thenReturn(id);
        when(a.getType()).thenReturn(t);
        when(a.getPayload()).thenReturn(payload);
        return a;
    }

    @Test
    void seulesLesActionsReussiesDuLotSontInscrites() {
        AdminUseCase admin = mock(AdminUseCase.class);
        SyncService sync = new SyncService(mock(StudentUseCase.class), mock(ProfessorUseCase.class), mock(SecurityUseCase.class), admin);
        JournalAudit journal = mock(JournalAudit.class);
        ReflectionTestUtils.setField(sync, "journal", journal);
        User u = UserFactory.create("Ndiaye", "Fallou", "fallou@test.sn", "x", RoleUtilisateur.fromString("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));

        List<SyncResult> res = sync.synchronize(List.of(
                action("a1", "LOG_ADD", Map.of("message", "Contrôle des présences")),
                action("a2", "GRADE_SAVE", Map.of("studentId", "e1", "courseId", "c1", "cc", 12))));   // refusé : contrôle du cours absent

        assertThat(res.get(0).isFailed()).isFalse();
        assertThat(res.get(1).isFailed()).isTrue();
        ArgumentCaptor<JournalAudit.Entree> e = ArgumentCaptor.forClass(JournalAudit.Entree.class);
        verify(journal, times(1)).inscrire(e.capture());
        assertThat(e.getValue().typeEntite()).isEqualTo("sync/LOG_ADD");
        assertThat(e.getValue().identifiants()).isEqualTo("a1");
        assertThat(e.getValue().acteurId()).isEqualTo(u.getId());
        assertThat(e.getValue().toString()).doesNotContain("Contrôle des présences");   // jamais les données du lot
    }
}
