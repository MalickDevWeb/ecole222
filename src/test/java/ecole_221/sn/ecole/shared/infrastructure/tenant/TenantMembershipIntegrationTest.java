package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APP-BE-02 — SEC-003 de bout en bout : vrai serveur, vrai JWT, vraie chaîne de filtres
 * (TenantResolutionFilter → JwtAuthenticationFilter → SecurityConfig → TenantMembershipInterceptor).
 * 1) un compte créé dans le contexte de l'école A devient membre de A (UserMembershipListener) et seulement de A ;
 * 2) ce compte est servi sur l'école A et reçoit 403 sur l'école B.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TenantMembershipIntegrationTest {

    /** Route rattachée à une école (hors /auth/**, traité en mode plateforme) et ouverte au rôle ETUDIANT. */
    static final String ROUTE_ECOLE = "/api/student/dashboard";

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

    private final HttpClient http = HttpClient.newHttpClient();
    private Organization ecoleA;
    private Organization ecoleB;
    private String suffixe;

    static Organization ecole(String nom, String slug) {
        return OrganizationFactory.create(nom, OrganizationType.ECOLE, OrganizationStatus.ACTIVE, slug,
                "SN", "Dakar", null, null, slug + "@exemple.sn", null, null, null, null, null);
    }

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        suffixe = UUID.randomUUID().toString().substring(0, 8);
        ecoleA = organisations.save(ecole("École A", "sec003-a-" + suffixe));
        ecoleB = organisations.save(ecole("École B", "sec003-b-" + suffixe));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /** Compte créé « pendant une requête de l'école A » (même contexte que l'API d'administration). */
    private User eleveCreeDansEcoleA() {
        TenantContext.setCurrentOrganization(ecoleA);
        TenantContext.setPlatformMode(false);
        try {
            return utilisateurs.save(UserFactory.create("Diop", "Awa", "awa." + suffixe + "@exemple.sn",
                    "jamais-utilise-ici", RoleUtilisateur.ETUDIANT));
        } finally {
            TenantContext.clear();
        }
    }

    private int get(String slug, String jwt) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ROUTE_ECOLE))
                .header("X-Tenant-Slug", slug).GET();
        if (jwt != null) {
            req.header("Authorization", "Bearer " + jwt);
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void compteCreeDansUneEcoleEnDevientMembreEtSeulementDeCelleCi() {
        User eleve = eleveCreeDansEcoleA();
        List<OrganizationUser> liens = appartenances.findByUserId(eleve.getId());
        assertEquals(1, liens.size(), "une seule appartenance attendue");
        assertEquals(ecoleA.getId(), liens.get(0).getOrganizationId());
        assertTrue(liens.get(0).isActive());
    }

    @Test
    void ecoleAServieEcoleBRefusee() throws Exception {
        User eleve = eleveCreeDansEcoleA();
        String jwt = jwtService.generateToken(utilisateurs.findByEmail(eleve.getEmail()).orElseThrow());

        int surA = get(ecoleA.getSlug(), jwt);
        int surB = get(ecoleB.getSlug(), jwt);

        assertEquals(200, surA, "membre de l'école A : servi");
        assertEquals(403, surB, "non membre de l'école B : 403 attendu (SEC-003)");
    }

    @Test
    void sansJetonRefuse() throws Exception {
        assertEquals(401, get(ecoleA.getSlug(), null));
    }
}
