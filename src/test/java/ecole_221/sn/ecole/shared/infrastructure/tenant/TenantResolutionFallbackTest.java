package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Une école demandée explicitement mais inconnue ne doit JAMAIS être servie comme ecole-221
 * (constat du 09/11 : derrière le proxy Vite, X-Tenant-Slug « 192 » tombait sur ecole-221).
 */
class TenantResolutionFallbackTest {

    private final OrganizationRepositoryPort repo = Mockito.mock(OrganizationRepositoryPort.class);
    private final Organization ecole221 = OrganizationFactory.create("École 221", OrganizationType.ECOLE, "contact@ecole221.sn");

    TenantResolutionFallbackTest() {
        Mockito.when(repo.findBySlug(anyString())).thenReturn(Optional.empty());
        Mockito.when(repo.findBySlug("ecole-221")).thenReturn(Optional.of(ecole221));
        Mockito.when(repo.findById(anyString())).thenReturn(Optional.empty());
        Mockito.when(repo.findBySubdomain(anyString())).thenReturn(Optional.empty());
        Mockito.when(repo.findByCustomDomain(anyString())).thenReturn(Optional.empty());
        Mockito.when(repo.findByEmail(anyString())).thenReturn(Optional.empty());
    }

    private int statut(String enTete) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/admin/stats");
        req.setServerName("127.0.0.1");
        if (enTete != null) req.addHeader("X-Tenant-Slug", enTete);
        MockHttpServletResponse res = new MockHttpServletResponse();
        new TenantResolutionFilter(repo).doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void ecoleInconnueDemandeeExplicitementRefusee() throws Exception {
        assertEquals(404, statut("192"));
    }

    @Test
    void sansEnTeteLeReplisLocalResteDisponible() throws Exception {
        assertEquals(200, statut(null));
    }
}
