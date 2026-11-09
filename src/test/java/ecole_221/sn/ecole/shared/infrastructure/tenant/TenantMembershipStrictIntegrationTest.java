package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APP-BE-02 — SEC-003 en mode strict (cible après APP-TL-04) : un compte SANS aucune appartenance
 * (donnée historique) n'a plus l'exception de transition et reçoit 403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "ecole.security.tenant-membership.strict=true")
class TenantMembershipStrictIntegrationTest {

    @LocalServerPort
    int port;
    @Autowired
    OrganizationRepositoryPort organisations;
    @Autowired
    OrganizationUserRepositoryPort appartenances;
    @Autowired
    UserRepositoryPort utilisateurs;
    @Autowired
    JwtService jwtService;

    @Test
    void compteSansAppartenanceRefuseEnModeStrict() throws Exception {
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        Organization ecole = organisations.save(TenantMembershipIntegrationTest.ecole("École S", "sec003-s-" + suffixe));
        TenantContext.clear();   // création hors école : aucune appartenance (cas des données historiques)
        User eleve = utilisateurs.save(UserFactory.create("Fall", "Moussa", "moussa." + suffixe + "@exemple.sn",
                "jamais-utilise-ici", RoleUtilisateur.ETUDIANT));
        assertTrue(appartenances.findByUserId(eleve.getId()).isEmpty(), "précondition : aucune appartenance");

        String jwt = jwtService.generateToken(utilisateurs.findByEmail(eleve.getEmail()).orElseThrow());
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + TenantMembershipIntegrationTest.ROUTE_ECOLE))
                .header("X-Tenant-Slug", ecole.getSlug()).header("Authorization", "Bearer " + jwt).GET().build();
        int code = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
        assertEquals(403, code);
    }
}
