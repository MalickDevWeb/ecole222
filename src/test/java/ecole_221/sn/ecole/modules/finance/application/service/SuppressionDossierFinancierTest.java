package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.finance.domain.port.repository.FinanceRepositoryPort;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.ExpenseJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * APP-TL-05 (2026-11-14) : suppression d'un dossier financier. Avant : deleteById silencieux, « succès » pour un dossier
 * inexistant ou d'une autre école ; un dossier avec versements pouvait être effacé.
 */
class SuppressionDossierFinancierTest {

    private final StudentFinanceJpaRepository dossiers = mock(StudentFinanceJpaRepository.class);
    private final TransactionJpaRepository transactions = mock(TransactionJpaRepository.class);
    private final FinanceUseCaseImpl service = new FinanceUseCaseImpl(mock(FinanceRepositoryPort.class), dossiers,
            mock(ExpenseJpaRepository.class), transactions);

    private static StudentFinanceJpaEntity dossier(long paye) {
        StudentFinanceJpaEntity d = new StudentFinanceJpaEntity();
        d.setId("d1");
        d.setStudentId("e1");
        d.setPaid(paye);
        return d;
    }

    @Test
    void dossierInexistantOuDUneAutreEcole404() {
        when(dossiers.findById("d-x")).thenReturn(Optional.empty());   // filtré par @TenantId
        assertThatThrownBy(() -> service.deleteStudentFinance("d-x"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(dossiers, never()).delete(any());
        verify(dossiers, never()).deleteById(any());
    }

    @Test
    void dossierAvecVersementsJamaisSupprime() {
        when(dossiers.findById("d1")).thenReturn(Optional.of(dossier(5000)));
        assertThatThrownBy(() -> service.deleteStudentFinance("d1"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        TransactionJpaEntity t = new TransactionJpaEntity();
        t.setStudentFinanceId("d1");
        when(dossiers.findById("d1")).thenReturn(Optional.of(dossier(0)));
        when(transactions.findByStudentId("e1")).thenReturn(List.of(t));
        assertThatThrownBy(() -> service.deleteStudentFinance("d1")).hasMessageContaining("409");
        verify(dossiers, never()).delete(any());
    }

    @Test
    void dossierSansVersementSupprime() {
        StudentFinanceJpaEntity d = dossier(0);
        when(dossiers.findById("d1")).thenReturn(Optional.of(d));
        when(transactions.findByStudentId("e1")).thenReturn(List.of());
        service.deleteStudentFinance("d1");
        verify(dossiers).delete(d);
    }
}
