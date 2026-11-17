package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.finance.application.port.dto.TransactionDto;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** 15/11 : GET /finance/transactions rend moyen, référence, élève, classe, dossier et auteur de la saisie (V19). */
class VersementsDetaillesTest {

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static StudentFinanceJpaEntity dossier() {
        StudentFinanceJpaEntity d = new StudentFinanceJpaEntity();
        d.setId("dossier-1");
        d.setStudentId("eleve-1");
        d.setStudentName("Awa Diop");
        d.setClasse("Licence 1 Informatique");
        d.setTotalRequired(300_000L);
        d.setPaid(0L);
        d.setDebt(300_000L);
        return d;
    }

    @Test
    void versementSaisiEnregistreSonAuteurEtLaReponseLeRend() {
        User comptable = UserFactory.create("Sarr", "Moussa", "moussa@ecole.sn", "x", RoleUtilisateur.fromString("COMPTABLE"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(comptable, null, List.of()));
        StudentFinanceJpaRepository dossiers = Mockito.mock(StudentFinanceJpaRepository.class);
        TransactionJpaRepository transactions = Mockito.mock(TransactionJpaRepository.class);
        StudentFinanceJpaEntity d = dossier();
        Mockito.when(dossiers.verrouillerParEleve("eleve-1")).thenReturn(List.of(d));
        Mockito.when(dossiers.save(any())).thenAnswer(i -> i.getArgument(0));
        new EcolagesService(Mockito.mock(ComptesEcoleService.class), dossiers, transactions)
                .encaisser(Map.of("eleveId", "eleve-1", "montant", 50_000, "moyen", "Wave", "reference", "WV-0042"));
        ArgumentCaptor<TransactionJpaEntity> t = ArgumentCaptor.forClass(TransactionJpaEntity.class);
        Mockito.verify(transactions).save(t.capture());
        assertThat(t.getValue().getSaisiParId()).isEqualTo(comptable.getId());
        assertThat(t.getValue().getSaisiPar()).isEqualTo("Moussa Sarr");

        TransactionDto dto = FinanceUseCaseImpl.toTransactionDtoFromEntity(t.getValue(), d);
        assertThat(dto.paymentMethod()).isEqualTo("Wave");
        assertThat(dto.reference()).isEqualTo("WV-0042");
        assertThat(dto.studentName()).isEqualTo("Awa Diop");
        assertThat(dto.classe()).isEqualTo("Licence 1 Informatique");
        assertThat(dto.dossierId()).isEqualTo("dossier-1");
        assertThat(dto.saisiPar()).isEqualTo("Moussa Sarr");
        assertThat(dto.saisiLe()).isNotNull();
        assertThat(dto.amount()).isEqualTo("50000");
        assertThat(dto.currency()).isEqualTo("FCFA");
    }

    @Test
    void ancienVersementSansAuteurNiDossierRendNullJamaisInvente() {
        TransactionJpaEntity t = new TransactionJpaEntity();
        t.setId("t-ancien");
        t.setStudentId("eleve-2");
        t.setAmount(10_000L);
        t.setTransactionType("Versement");
        t.setCreatedAt(null);
        TransactionDto dto = FinanceUseCaseImpl.toTransactionDtoFromEntity(t, null);
        assertThat(dto.saisiPar()).isNull();
        assertThat(dto.saisiLe()).isNull();
        assertThat(dto.paymentMethod()).isNull();
        assertThat(dto.reference()).isNull();
        assertThat(dto.studentName()).isNull();
        assertThat(dto.classe()).isNull();
        assertThat(dto.dossierId()).isNull();
        assertThat(dto.id()).isEqualTo("t-ancien");
        assertThat(dto.status()).isEqualTo("COMPLETED");
    }

    @Test
    void sansCompteConnecteAucunAuteur() {
        StudentFinanceJpaRepository dossiers = Mockito.mock(StudentFinanceJpaRepository.class);
        TransactionJpaRepository transactions = Mockito.mock(TransactionJpaRepository.class);
        Mockito.when(dossiers.verrouillerParEleve(anyString())).thenReturn(List.of(dossier()));
        Mockito.when(dossiers.save(any())).thenAnswer(i -> i.getArgument(0));
        new EcolagesService(Mockito.mock(ComptesEcoleService.class), dossiers, transactions)
                .encaisser(Map.of("eleveId", "eleve-1", "montant", 1_000));
        ArgumentCaptor<TransactionJpaEntity> t = ArgumentCaptor.forClass(TransactionJpaEntity.class);
        Mockito.verify(transactions).save(t.capture());
        assertThat(t.getValue().getSaisiPar()).isNull();
        assertThat(t.getValue().getSaisiParId()).isNull();
    }
}
