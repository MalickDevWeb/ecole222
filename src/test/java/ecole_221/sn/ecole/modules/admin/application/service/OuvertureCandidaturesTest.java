package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ParametresCandidaturesJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ParametresCandidaturesJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Un dossier n'est accepté que si l'école accepte des candidatures AUJOURD'HUI (APP-BE-15). */
class OuvertureCandidaturesTest {

    private ParametresCandidaturesJpaRepository depot;
    private ParametresCandidaturesService service;

    @BeforeEach
    void preparer() {
        Organization ecole = mock(Organization.class);
        when(ecole.getId()).thenReturn("ecole-1");
        TenantContext.setCurrentOrganization(ecole);
        depot = mock(ParametresCandidaturesJpaRepository.class);
        service = new ParametresCandidaturesService(depot, Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneId.of("Africa/Dakar")));
    }

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
    }

    private void reglages(boolean ouvert, String debut, String fin) {
        ParametresCandidaturesJpaEntity p = new ParametresCandidaturesJpaEntity();
        p.setOrganizationId("ecole-1");
        p.setOuvert(ouvert);
        p.setDateOuverture(debut);
        p.setDateFermeture(fin);
        when(depot.findById("ecole-1")).thenReturn(Optional.of(p));
    }

    private void refus(String extrait) {
        assertThatThrownBy(service::exigerOuverture).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessageContaining(extrait);
    }

    @Test
    void ecoleNonConfigureeOuFermee() {
        when(depot.findById("ecole-1")).thenReturn(Optional.empty());
        refus("ne sont pas ouvertes");
        reglages(false, "2026-11-01", "2027-01-31");
        refus("ne sont pas ouvertes");
    }

    @Test
    void avantOuvertureEtApresCloture() {
        reglages(true, "2026-12-01", "2027-01-31");
        refus("ouvriront le 01/12/2026");
        reglages(true, "2026-09-01", "2026-11-10");
        refus("closes depuis le 10/11/2026");
    }

    @Test
    void ouverteAujourdhuiBornesComprises() {
        reglages(true, "2026-11-11", "2026-11-11");
        assertThatCode(service::exigerOuverture).doesNotThrowAnyException();
        reglages(true, null, null);
        assertThatCode(service::exigerOuverture).doesNotThrowAnyException();
    }
}
