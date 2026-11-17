package ecole_221.sn.ecole.modules.audit.application.service;

import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Durée de conservation du journal d'audit, paramétrable (30 minutes pour l'instant). */
class ConservationJournalAuditServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final JournalAudit journal = mock(JournalAudit.class);
    private final ConservationJournalAuditService service = new ConservationJournalAuditService(jdbc, journal);

    @BeforeEach
    void dureeEnBase() {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put("minutes", 30L);
        ligne.put("modifie_le", null);
        ligne.put("modifie_par", null);
        when(jdbc.queryForList(anyString(), eq(ConservationJournalAuditService.CLE))).thenReturn(List.of(ligne));
    }

    @ParameterizedTest
    @CsvSource({"30, 30, minutes, 30 minutes", "1, 1, minutes, 1 minute", "120, 2, heures, 2 heures",
            "1440, 1, jours, 1 jour", "129600, 90, jours, 90 jours", "90, 90, minutes, 90 minutes"})
    void dureeAfficheeDansLUniteLaPlusLisible(long minutes, long valeur, String unite, String libelle) {
        Map<String, Object> m = ConservationJournalAuditService.lisible(minutes);
        assertThat(m).containsEntry("valeur", valeur).containsEntry("unite", unite).containsEntry("libelle", libelle);
    }

    @Test
    void lectureDeLaDureeEnBase() {
        assertThat(service.lire()).containsEntry("libelle", "30 minutes").containsEntry("minutes", 30L);
    }

    @Test
    void modificationEnregistreeEnIntervallePostgresql() {
        service.modifier(Map.of("valeur", 2, "unite", "heures"));
        verify(jdbc).update(startsWith("UPDATE parametres_plateforme"), eq("2 hours"), any(), any(), eq("audit.conservation"));
        service.modifier(Map.of("valeur", "90", "unite", "jours"));
        verify(jdbc).update(startsWith("UPDATE parametres_plateforme"), eq("90 days"), any(), any(), eq("audit.conservation"));
    }

    @Test
    void valeursInvalidesRefuseesAvecUnMessageClair() {
        assertThatThrownBy(() -> service.modifier(Map.of())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Aucune donnée");
        assertThatThrownBy(() -> service.modifier(Map.of("valeur", 0, "unite", "minutes"))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.modifier(Map.of("valeur", -5, "unite", "jours"))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.modifier(Map.of("valeur", 3, "unite", "semaines")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("minutes, heures, jours");
        assertThatThrownBy(() -> service.modifier(Map.of("valeur", 3651, "unite", "jours")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("10 ans");
        verify(jdbc, never()).update(startsWith("UPDATE"), any(Object[].class));
    }

    @Test
    void purgeInscriteAuJournalSeulementSiDesEntreesSontSupprimees() {
        when(jdbc.update(startsWith("DELETE FROM audit_logs"), eq("audit.conservation"))).thenReturn(0);
        assertThat(service.purger()).isZero();
        verifyNoInteractions(journal);

        when(jdbc.update(startsWith("DELETE FROM audit_logs"), eq("audit.conservation"))).thenReturn(3);
        assertThat(service.purger()).isEqualTo(3);
        ArgumentCaptor<JournalAudit.Entree> e = ArgumentCaptor.forClass(JournalAudit.Entree.class);
        verify(journal).inscrire(e.capture());
        assertThat(e.getValue().description()).isEqualTo("Purge de conservation : 3 entrée(s) de plus de 30 minutes supprimée(s)");
        assertThat(e.getValue().ecole()).isNull();   // plateforme
    }
}
