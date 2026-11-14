package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Situation financière réelle d'un élève (2026-11-14) : dossier de la comptabilité + versements enregistrés. */
class SituationFinanciereEleveServiceTest {

    private final StudentFinanceJpaRepository dossiers = mock(StudentFinanceJpaRepository.class);
    private final TransactionJpaRepository transactions = mock(TransactionJpaRepository.class);
    private final SituationFinanciereEleveService service = new SituationFinanciereEleveService(dossiers, transactions);

    static StudentFinanceJpaEntity dossier(String classe, long total, long paye, Long reste, String statut) {
        StudentFinanceJpaEntity d = new StudentFinanceJpaEntity();
        d.setStudentId("e1");
        d.setClasse(classe);
        d.setTotalRequired(total);
        d.setPaid(paye);
        d.setDebt(reste);
        d.setStatus(statut);
        return d;
    }

    static TransactionJpaEntity transaction(String id, String type, long montant, LocalDateTime date, String moyen) {
        TransactionJpaEntity t = new TransactionJpaEntity();
        t.setId(id);
        t.setStudentId("e1");
        t.setTransactionType(type);
        t.setAmount(montant);
        t.setTransactionDate(date);
        t.setPaymentMethod(moyen);
        return t;
    }

    @Test
    void sansDossierFinancierRienNEstRenvoye() {
        when(dossiers.findAllByStudentIdOrderByCreatedAtDesc("e1")).thenReturn(List.of());
        assertThat(service.situation("e1")).isEmpty();
        assertThat(service.situation(null)).isEmpty();
        verifyNoInteractions(transactions);
    }

    @Test
    void versementsSeulementLesPaiementsPositifsDansLOrdreDesDates() {
        when(dossiers.findAllByStudentIdOrderByCreatedAtDesc("e1")).thenReturn(List.of(dossier("L1", 300_000, 150_000, 150_000L, "Non soldé")));
        LocalDateTime j = LocalDateTime.of(2026, 10, 5, 10, 0);
        when(transactions.findByStudentId("e1")).thenReturn(List.of(
                transaction("t2", "PAYMENT", 60_000, j.plusDays(20), null),
                transaction("t1", "PAYMENT", 40_000, j, "Wave"),
                transaction("t3", "EXPENSE", 5_000, j, null),       // pas un versement de l'élève
                transaction("t4", "PAYMENT", 0, j, null)));          // montant nul ignoré
        var s = service.situation("e1").orElseThrow();
        assertThat(s.versements()).extracting(SituationFinanciereEleveService.Versement::id).containsExactly("t1", "t2");
        assertThat(s.totalDu()).isEqualTo(300_000);
        assertThat(s.resteDu()).isEqualTo(150_000);
        assertThat(s.versementsSansDetail()).isEqualTo(50_000);   // 150 000 payés, 100 000 détaillés
        assertThat(s.versements().get(0).moyen()).isEqualTo("Wave");
    }

    @Test
    void versementsDUnAncienDossierExclus() {
        StudentFinanceJpaEntity actuel = dossier("L2", 400_000, 50_000, 350_000L, "Non soldé");
        actuel.setId("d2");
        StudentFinanceJpaEntity ancien = dossier("L1", 300_000, 300_000, 0L, "En Règle");
        ancien.setId("d1");
        when(dossiers.findAllByStudentIdOrderByCreatedAtDesc("e1")).thenReturn(List.of(actuel, ancien));
        LocalDateTime j = LocalDateTime.of(2026, 10, 5, 10, 0);
        TransactionJpaEntity anneePassee = transaction("t1", "PAYMENT", 300_000, j.minusYears(1), "Espèces");
        anneePassee.setStudentFinanceId("d1");
        TransactionJpaEntity cetteAnnee = transaction("t2", "PAYMENT", 50_000, j, "Wave");
        cetteAnnee.setStudentFinanceId("d2");
        when(transactions.findByStudentId("e1")).thenReturn(List.of(anneePassee, cetteAnnee));
        var s = service.situation("e1").orElseThrow();
        assertThat(s.versements()).extracting(SituationFinanciereEleveService.Versement::id).containsExactly("t2");
        assertThat(s.versementsSansDetail()).isZero();
    }

    @Test
    void dossierLePlusRecentEtResteCalculeSiAbsent() {
        when(dossiers.findAllByStudentIdOrderByCreatedAtDesc("e1")).thenReturn(List.of(
                dossier("L2", 400_000, 100_000, null, null), dossier("L1", 300_000, 300_000, 0L, "En Règle")));
        when(transactions.findByStudentId("e1")).thenReturn(List.of());
        var s = service.situation("e1").orElseThrow();
        assertThat(s.classe()).isEqualTo("L2");
        assertThat(s.resteDu()).isEqualTo(300_000);
    }
}
