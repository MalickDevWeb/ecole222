package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Situation financière RÉELLE d'un élève : dossier financier saisi par la comptabilité de l'école
 * (montant dû, payé, reste, statut) et versements enregistrés (transactions de type PAYMENT).
 *
 * <p>Lue par l'espace parent (GET /parent/paiements) et par la fiche 360° de l'administration. Lecture cloisonnée :
 * les deux entités portent @TenantId (école courante). Aucune donnée inventée : sans dossier, rien n'est renvoyé.</p>
 */
@Service
public class SituationFinanciereEleveService {

    public static final String TYPE_VERSEMENT = "PAYMENT";

    /** Un versement enregistré par la comptabilité. */
    public record Versement(String id, long montant, LocalDateTime date, String moyen, String reference, String description) {
    }

    /** Dossier financier de l'élève ; {@code versementsSansDetail} : payé enregistré sans transaction détaillée. */
    public record Situation(String eleveId, String classe, long totalDu, long paye, long resteDu, String statut,
                            LocalDateTime dernierVersement, List<Versement> versements) {
        public long versementsSansDetail() {
            return Math.max(0, paye - versements.stream().mapToLong(Versement::montant).sum());
        }
    }

    private final StudentFinanceJpaRepository dossiers;
    private final TransactionJpaRepository transactions;

    public SituationFinanciereEleveService(StudentFinanceJpaRepository dossiers, TransactionJpaRepository transactions) {
        this.dossiers = dossiers;
        this.transactions = transactions;
    }

    private static long nul0(Long v) {
        return v == null ? 0 : v;
    }

    @Transactional(readOnly = true)
    public Optional<Situation> situation(String eleveId) {
        if (eleveId == null || eleveId.isBlank()) {
            return Optional.empty();
        }
        // dossier le plus récent (un élève peut en avoir plusieurs, ex. une année par dossier)
        List<StudentFinanceJpaEntity> liste = dossiers.findAllByStudentIdOrderByCreatedAtDesc(eleveId);
        if (liste.isEmpty()) {
            return Optional.empty();
        }
        StudentFinanceJpaEntity d = liste.get(0);
        List<Versement> versements = transactions.findByStudentId(eleveId).stream()
                .filter(t -> TYPE_VERSEMENT.equalsIgnoreCase(t.getTransactionType()))
                .filter(t -> nul0(t.getAmount()) > 0)
                // versements de CE dossier (ceux d'un ancien dossier ne comptent plus) ; sans rattachement : anciens
                // versements saisis par /finance/transactions, gardés
                .filter(t -> t.getStudentFinanceId() == null || t.getStudentFinanceId().equals(d.getId()))
                .sorted(Comparator.comparing((TransactionJpaEntity t) -> t.getTransactionDate() == null ? t.getCreatedAt() : t.getTransactionDate(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(t -> new Versement(t.getId(), nul0(t.getAmount()),
                        t.getTransactionDate() != null ? t.getTransactionDate() : t.getCreatedAt(),
                        t.getPaymentMethod(), t.getReference(), t.getDescription()))
                .toList();
        long total = nul0(d.getTotalRequired());
        long paye = nul0(d.getPaid());
        long reste = d.getDebt() != null ? Math.max(0, d.getDebt()) : Math.max(0, total - paye);
        return Optional.of(new Situation(eleveId, d.getClasse(), total, paye, reste, d.getStatus(), d.getLastPaymentDate(), versements));
    }
}
