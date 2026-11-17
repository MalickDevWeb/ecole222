package ecole_221.sn.ecole.modules.archivage.domain.model;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Règles de l'agrégat Organization utilisées par l'archivage (défaut relevé par l'étude : suspended_at). */
class OrganizationArchivageTest {

    private static Organization ecole(String statut, LocalDateTime suspendue) {
        return OrganizationFactory.reconstruct("org-a", "École A", "ECOLE", statut, "ecole-a", null, null, null, null, null,
                null, null, null, null, null, LocalDateTime.of(2026, 1, 1, 0, 0), null, null, suspendue);
    }

    @Test
    void unarchiveRemetSuspendedAtAMaintenant() {
        LocalDateTime ancienne = LocalDateTime.of(2026, 1, 1, 0, 0);
        Organization o = ecole("ARCHIVED", ancienne);
        LocalDateTime avant = LocalDateTime.now().minusSeconds(1);
        o.unarchive();
        assertThat(o.getStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
        // avant le correctif : l'ancienne date restait, et l'archivage automatique repartait aussitôt
        assertThat(o.getSuspendedAt()).isAfter(avant);
    }

    @Test
    void restaurerRendLEcoleActiveSansDateDeSuspension() {
        Organization o = ecole("ARCHIVED", LocalDateTime.of(2026, 1, 1, 0, 0));
        o.restaurer();
        assertThat(o.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(o.getSuspendedAt()).isNull();
        assertThatThrownBy(() -> ecole("SUSPENDED", null).restaurer()).isInstanceOf(IllegalStateException.class);
    }
}
