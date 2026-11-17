package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Écolages : ouverture de dossier et encaissement contrôlés ; payé, reste et statut recalculés. */
class EcolagesServiceTest {

    private final ComptesEcoleService comptes = mock(ComptesEcoleService.class);
    private final StudentFinanceJpaRepository dossiers = mock(StudentFinanceJpaRepository.class);
    private final TransactionJpaRepository transactions = mock(TransactionJpaRepository.class);
    private final EcolagesService service = new EcolagesService(comptes, dossiers, transactions);

    @BeforeEach
    void bases() {
        UserJpaEntity awa = new UserJpaEntity();
        awa.setPrenom("Awa");
        awa.setNom("Sarr");
        when(comptes.membre("e1", "ETUDIANT")).thenReturn(awa);
        when(comptes.membre("intrus", "ETUDIANT")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "introuvable"));
        when(dossiers.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactions.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static StudentFinanceJpaEntity dossier(long du, long paye) {
        StudentFinanceJpaEntity d = new StudentFinanceJpaEntity();
        d.setId("d1");
        d.setStudentId("e1");
        d.setStudentName("Awa Sarr");
        d.setTotalRequired(du);
        d.setPaid(paye);
        d.setDebt(Math.max(0, du - paye));
        d.setStatus(EcolagesService.statut(du, paye));
        return d;
    }

    @Test
    void ouvertureDuDossierDUnVraiEleve() {
        when(dossiers.findAllByStudentIdOrderByCreatedAtDesc("e1")).thenReturn(List.of());
        Map<String, Object> d = service.ouvrirDossier(Map.of("eleveId", "e1", "montantDu", 450000, "classe", "L1 Informatique"));
        assertThat(d.get("id")).isNotNull();
        assertThat(d).containsEntry("studentName", "Awa Sarr").containsEntry("totalRequired", 450000L)
                .containsEntry("paid", 0L).containsEntry("debt", 450000L).containsEntry("status", "Non soldé")
                .containsEntry("classe", "L1 Informatique");
    }

    @Test
    void ouvertureRefuseeElevesInconnusDoublonsEtMontantsAbsurdes() {
        assertThatThrownBy(() -> service.ouvrirDossier(Map.of("eleveId", "intrus", "montantDu", 1000)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        when(dossiers.findAllByStudentIdOrderByCreatedAtDesc("e1")).thenReturn(List.of(dossier(1000, 0)));
        assertThatThrownBy(() -> service.ouvrirDossier(Map.of("eleveId", "e1", "montantDu", 1000)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("déjà un dossier");
        assertThatThrownBy(() -> service.ouvrirDossier(Map.of("eleveId", "e1", "montantDu", 0))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.ouvrirDossier(Map.of("montantDu", 1000))).isInstanceOf(ResponseStatusException.class);
        verify(dossiers, never()).save(any());
    }

    @Test
    void versementEnregistreEtDossierRecalcule() {
        when(dossiers.verrouillerParEleve("e1")).thenReturn(List.of(dossier(450000, 100000)));
        Map<String, Object> d = service.encaisser(Map.of("eleveId", "e1", "montant", 150000, "moyen", "Wave", "reference", "W-77"));
        assertThat(d).containsEntry("paid", 250000L).containsEntry("debt", 200000L).containsEntry("status", "Non soldé");
        ArgumentCaptor<TransactionJpaEntity> t = ArgumentCaptor.forClass(TransactionJpaEntity.class);
        verify(transactions).save(t.capture());
        assertThat(t.getValue().getTransactionType()).isEqualTo("PAYMENT");
        assertThat(t.getValue().getPaymentMethod()).isEqualTo("Wave");
        assertThat(t.getValue().getReference()).isEqualTo("W-77");
        assertThat(t.getValue().getStudentFinanceId()).isEqualTo("d1");
        assertThat(t.getValue().getTransactionDate().toLocalDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void soldeEtAvanceDeterminesParLeServeur() {
        when(dossiers.verrouillerParEleve("e1")).thenReturn(List.of(dossier(300000, 200000)));
        assertThat(service.encaisser(Map.of("eleveId", "e1", "montant", 100000))).containsEntry("status", "En Règle").containsEntry("debt", 0L);
        when(dossiers.verrouillerParEleve("e1")).thenReturn(List.of(dossier(300000, 300000)));
        assertThat(service.encaisser(Map.of("eleveId", "e1", "montant", 5000))).containsEntry("status", "En Avance");
    }

    @Test
    void versementsRefusesSansDossierMontantMoyenOuDateInvalides() {
        when(dossiers.verrouillerParEleve("e1")).thenReturn(List.of());
        assertThatThrownBy(() -> service.encaisser(Map.of("eleveId", "e1", "montant", 1000)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("ouvrez-le d'abord");
        when(dossiers.verrouillerParEleve("e1")).thenReturn(List.of(dossier(1000, 0)));
        assertThatThrownBy(() -> service.encaisser(Map.of("eleveId", "e1", "montant", -5))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.encaisser(Map.of("eleveId", "e1", "montant", 100, "moyen", "Bitcoin")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Wave");
        assertThatThrownBy(() -> service.encaisser(Map.of("eleveId", "e1", "montant", 100, "date", LocalDate.now().plusDays(3).toString())))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("futur");
        verify(transactions, never()).save(any());
    }
}
