package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.finance.application.service.SituationFinanciereEleveService;
import ecole_221.sn.ecole.modules.finance.application.service.SituationFinanciereEleveService.Situation;
import ecole_221.sn.ecole.modules.finance.application.service.SituationFinanciereEleveService.Versement;
import ecole_221.sn.ecole.modules.parent.application.port.dto.PaymentDto;
import ecole_221.sn.ecole.modules.parent.domain.model.ParentEnfant;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** GET /parent/paiements (2026-11-14) : situation réelle des enfants, jamais de donnée inventée. */
class PaiementsParentReelsTest {

    private final ParentRepositoryPort parents = mock(ParentRepositoryPort.class);
    private final UserRepositoryPort comptes = mock(UserRepositoryPort.class);
    private final SituationFinanciereEleveService situations = mock(SituationFinanciereEleveService.class);
    private final ParentService service = new ParentService(parents, mock(AppointmentRepositoryPort.class), comptes);

    @BeforeEach
    void enfants() {
        service.setSituationsFinancieres(situations);
        when(comptes.findById("e1")).thenReturn(Optional.of(UserFactory.create("Sarr", "Awa", "awa@test.sn", "x", RoleUtilisateur.fromString("ETUDIANT"))));
        when(comptes.findById("e2")).thenReturn(Optional.of(UserFactory.create("Sarr", "Modou", "modou@test.sn", "x", RoleUtilisateur.fromString("ETUDIANT"))));
    }

    private static Situation situation(String id, long total, long paye, long reste, String statut, Versement... v) {
        return new Situation(id, "L1", total, paye, reste, statut, LocalDateTime.of(2026, 10, 1, 9, 0), List.of(v));
    }

    @Test
    void sansDossierFinancierNonDisponible() {
        when(parents.findParentEnfantsByParentId("p1")).thenReturn(List.of(ParentEnfant.create("p1", "e1", "MERE")));
        when(situations.situation("e1")).thenReturn(Optional.empty());
        PaymentDto r = service.getParentPayments("p1");
        assertThat(r.statut()).isEqualTo(PaymentDto.STATUT_NON_DISPONIBLE);
        assertThat(r.echeances()).isEmpty();
    }

    @Test
    void versementsPayesResteEnCoursSansDateInventee() {
        when(parents.findParentEnfantsByParentId("p1")).thenReturn(List.of(ParentEnfant.create("p1", "e1", "MERE")));
        when(situations.situation("e1")).thenReturn(Optional.of(situation("e1", 300_000, 150_000, 150_000, "Partiel",
                new Versement("t1", 100_000, LocalDateTime.of(2026, 10, 5, 10, 0), "Wave", "W-77", null))));
        PaymentDto r = service.getParentPayments("p1");
        assertThat(r.statut()).isEqualTo(PaymentDto.STATUT_DISPONIBLE);
        assertThat(r.totalAnnuel()).isEqualTo(300_000);
        assertThat(r.echeances()).extracting(PaymentDto.PaymentInstallmentDto::statut).containsExactly("PAYE", "PAYE", "EN_COURS");
        var versement = r.echeances().get(0);
        assertThat(versement.label()).isEqualTo("Versement (Wave), réf. W-77");
        assertThat(versement.datePaiement()).isEqualTo("2026-10-05");
        assertThat(r.echeances().get(1).montant()).isEqualTo(50_000);              // payé enregistré sans détail
        var reste = r.echeances().get(2);
        assertThat(reste.montant()).isEqualTo(150_000);
        assertThat(reste.echeance()).isNull();                                     // aucune date inventée
        assertThat(reste.label()).isEqualTo("Reste à payer (L1)");
    }

    @Test
    void plusieursEnfantsLignesNommeesEtResteDuEnCours() {
        when(parents.findParentEnfantsByParentId("p1")).thenReturn(List.of(
                ParentEnfant.create("p1", "e1", "MERE"), ParentEnfant.create("p1", "e2", "MERE")));
        when(situations.situation("e1")).thenReturn(Optional.of(situation("e1", 300_000, 300_000, 0, "En Règle",
                new Versement("t1", 300_000, LocalDateTime.of(2026, 9, 1, 9, 0), null, null, null))));
        when(situations.situation("e2")).thenReturn(Optional.of(situation("e2", 200_000, 0, 200_000, "Non soldé")));
        PaymentDto r = service.getParentPayments("p1");
        assertThat(r.totalAnnuel()).isEqualTo(500_000);
        assertThat(r.echeances()).extracting(PaymentDto.PaymentInstallmentDto::label)
                .containsExactly("Versement — Awa Sarr", "Reste à payer (L1) — Modou Sarr");
        // APP-TL-22 : sans échéance enregistrée, un reste dû n'est jamais présenté « en retard »
        assertThat(r.echeances().get(1).statut()).isEqualTo("EN_COURS");
    }
}
