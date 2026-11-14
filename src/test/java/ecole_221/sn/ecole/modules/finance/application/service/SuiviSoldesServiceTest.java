package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * APP-TL-22 : suivi des soldes en lecture seule. Aucun échéancier n'est enregistré : le retard est ESTIMÉ sur un
 * paiement régulier en 9 mensualités d'octobre à juin ; un dossier est « à suivre » à partir de deux mensualités de
 * retard et 30 jours sans versement.
 */
class SuiviSoldesServiceTest {

    private static final LocalDate MI_NOVEMBRE = LocalDate.of(2026, 11, 14);

    @Test
    void moisEntamesDOctobreAJuin() {
        assertThat(SuiviSoldesService.moisEntames(LocalDate.of(2026, 10, 1))).isEqualTo(1);
        assertThat(SuiviSoldesService.moisEntames(MI_NOVEMBRE)).isEqualTo(2);
        assertThat(SuiviSoldesService.moisEntames(LocalDate.of(2027, 1, 5))).isEqualTo(4);
        assertThat(SuiviSoldesService.moisEntames(LocalDate.of(2027, 6, 30))).isEqualTo(9);
        assertThat(SuiviSoldesService.moisEntames(LocalDate.of(2027, 8, 15))).isEqualTo(9);   // vacances : année due
    }

    @Test
    void familleQuiPaieSesTrimestresALHeureJamaisASuivre() {
        // 450 000 FCFA par an ; premier trimestre (150 000) payé en octobre ; début janvier, trimestre suivant pas encore
        assertThat(SuiviSoldesService.retardEstime(450_000, 150_000, LocalDate.of(2027, 1, 5))).isEqualTo(50_000);
        assertThat(SuiviSoldesService.aSuivre(450_000, 150_000, 90, LocalDate.of(2027, 1, 5))).isFalse();
    }

    @Test
    void deuxMensualitesDeRetardEtTrenteJoursSansVersementASuivre() {
        // rien payé depuis la rentrée : mi-novembre, 2 mensualités attendues
        assertThat(SuiviSoldesService.aSuivre(450_000, 0, 44, MI_NOVEMBRE)).isTrue();
        // même retard, mais un versement il y a 10 jours : la famille est en train de payer
        assertThat(SuiviSoldesService.aSuivre(450_000, 0, 10, MI_NOVEMBRE)).isFalse();
        // une seule mensualité de retard : pas à suivre
        assertThat(SuiviSoldesService.aSuivre(450_000, 50_000, 44, MI_NOVEMBRE)).isFalse();
    }

    @Test
    void listeDesDossiersNonSoldesAvecParentsSansAucuneMesure() {
        StudentFinanceJpaRepository dossiers = mock(StudentFinanceJpaRepository.class);
        ParentEnfantJpaRepository liens = mock(ParentEnfantJpaRepository.class);
        UserJpaRepository comptes = mock(UserJpaRepository.class);
        StudentFinanceJpaEntity d = new StudentFinanceJpaEntity();
        d.setId("d1");
        d.setStudentId("e1");
        d.setStudentName("Awa Fall");
        d.setTotalRequired(450_000L);
        d.setPaid(0L);
        d.setDebt(450_000L);
        d.setCreatedAt(LocalDateTime.of(2026, 10, 1, 8, 0));
        when(dossiers.findStudentsWithDebt()).thenReturn(List.of(d));
        ParentEnfantJpaEntity lien = mock(ParentEnfantJpaEntity.class);
        when(lien.getParentId()).thenReturn("p1");
        when(liens.findByEnfantId("e1")).thenReturn(List.of(lien));
        UserJpaEntity parent = mock(UserJpaEntity.class);
        when(parent.getPrenom()).thenReturn("Moussa");
        when(parent.getNom()).thenReturn("Fall");
        when(parent.getTelephone()).thenReturn("+221770000000");
        when(comptes.findAllById(anyIterable())).thenReturn(List.of(parent));

        List<Map<String, Object>> r = new SuiviSoldesService(dossiers, liens, comptes).soldes(MI_NOVEMBRE);
        assertThat(r).hasSize(1);
        assertThat(r.get(0)).containsEntry("eleveId", "e1").containsEntry("resteDu", 450_000L)
                .containsEntry("mensualite", 50_000L).containsEntry("retardEstime", 100_000L)
                .containsEntry("joursSansVersement", 44L).containsEntry("aSuivre", true)
                .doesNotContainKeys("statutFrais", "bloque", "autorise");
        assertThat(r.get(0).get("parents")).isEqualTo(List.of(Map.of("nom", "Moussa Fall", "telephone", "+221770000000")));
    }
}
