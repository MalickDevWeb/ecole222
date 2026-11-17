package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.application.DomainesEcoleService;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolutionHoteEcole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;

/** N3 (revue DevSecOps phase 2) : GET /admin/domaine ne révèle pas l'autre école et est freiné. */
class VerificationDomaineSansDeclarationTest {

    private final Organization ecoleA = OrganizationFactory.create("École A", OrganizationType.ECOLE, OrganizationStatus.ACTIVE,
            "ecole-a", "SN", "Dakar", null, null, "a@exemple.sn", null, null, null, null, null);
    private final DomaineEcoleController controleur;

    VerificationDomaineSansDeclarationTest() {
        OrganizationRepositoryPort repo = Mockito.mock(OrganizationRepositoryPort.class);
        Mockito.when(repo.findBySlug(anyString())).thenReturn(Optional.empty());
        Map<String, String> dns = Map.of("portail.de-b.sn", "ecole-b.myschool.sn", "portail.de-a.sn", "ecole-a.myschool.sn");
        ResolutionHoteEcole resolution = new ResolutionHoteEcole(repo, h -> Optional.ofNullable(dns.get(h)), h -> Optional.empty(),
                "myschool.sn", "", "http://localhost:5173");
        controleur = new DomaineEcoleController(resolution, Mockito.mock(DomainesEcoleService.class));
        TenantContext.setCurrentOrganization(ecoleA);
    }

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
    }

    @Test
    void lIdentifiantDUneAutreEcoleNEstJamaisRenvoye() {
        Map<String, Object> b = controleur.verifierSansDeclarer("portail.de-b.sn").getBody();
        assertThat(b.get("pointeVersCetteEcole")).isEqualTo(false);
        assertThat(b.get("pointeVers")).isNull();
        assertThat(b.toString()).doesNotContain("ecole-b");
        Map<String, Object> a = controleur.verifierSansDeclarer("portail.de-a.sn").getBody();
        assertThat(a.get("pointeVersCetteEcole")).isEqualTo(true);
        assertThat(a.get("pointeVers")).isEqualTo("ecole-a");
    }

    @Test
    void freineeParEcole() {
        for (int i = 0; i < DomaineEcoleController.VERIFICATIONS_MAX; i++) {
            controleur.verifierSansDeclarer("x" + i + ".ailleurs.sn");
        }
        assertThatThrownBy(() -> controleur.verifierSansDeclarer("portail.de-a.sn"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(429));
    }
}
