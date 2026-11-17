package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.RegistreDomaines;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolutionHoteEcole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Résolution de l'école (15/11, revue DevSecOps) : hôte d'école prioritaire sur l'en-tête (403 si incohérent),
 * X-Forwarded-Host lu seulement depuis un relais de confiance, Referer ignoré, /api/sync rattaché à une école,
 * statut contrôlé APRÈS l'authentification (StatutEcoleFilter, F1), JSON d'erreur sérialisé (F12).
 */
class TenantResolutionDomaineTest {

    private final OrganizationRepositoryPort repo = Mockito.mock(OrganizationRepositoryPort.class);
    private final Map<String, String> dns = new HashMap<>();
    private final Map<String, RegistreDomaines.DomaineDeclare> declares = new HashMap<>();
    private final Map<String, Organization> parSlug = new HashMap<>();
    private final TenantResolutionFilter filtre;
    private final StatutEcoleFilter statut = new StatutEcoleFilter();

    static Organization ecole(String slug, OrganizationStatus statut) {
        return OrganizationFactory.create("École " + slug, OrganizationType.ECOLE, statut, slug,
                "SN", "Dakar", null, null, slug + "@exemple.sn", null, null, null, null, null);
    }

    TenantResolutionDomaineTest() {
        Mockito.when(repo.findBySlug(anyString())).thenReturn(Optional.empty());
        Mockito.when(repo.findById(anyString())).thenReturn(Optional.empty());
        for (Organization o : new Organization[]{ecole("ecole-a", OrganizationStatus.ACTIVE), ecole("ecole-b", OrganizationStatus.ACTIVE),
                ecole("ecole-221", OrganizationStatus.ACTIVE), ecole("ecole-attente", OrganizationStatus.PENDING),
                ecole("ecole-suspendue", OrganizationStatus.SUSPENDED), ecole("ecole-archivee", OrganizationStatus.ARCHIVED),
                ecole("ecole-rejetee", OrganizationStatus.REJECTED)}) {
            parSlug.put(o.getSlug(), o);
            Mockito.when(repo.findBySlug(o.getSlug())).thenReturn(Optional.of(o));
            Mockito.when(repo.findById(o.getId())).thenReturn(Optional.of(o));
        }
        dns.put("portail.lycee-b.sn", "ecole-b.myschool.sn");
        dns.put("non-declare.sn", "ecole-a.myschool.sn");
        declares.put("portail.lycee-b.sn", new RegistreDomaines.DomaineDeclare("portail.lycee-b.sn", parSlug.get("ecole-b").getId(), "ACTIF"));
        declares.put("portail.test.sn", new RegistreDomaines.DomaineDeclare("portail.test.sn", parSlug.get("ecole-a").getId(), "ACTIF"));
        declares.put("suspendue.test.sn", new RegistreDomaines.DomaineDeclare("suspendue.test.sn", parSlug.get("ecole-suspendue").getId(), "ACTIF"));
        ResolutionHoteEcole resolution = new ResolutionHoteEcole(repo, h -> Optional.ofNullable(dns.get(h)),
                h -> Optional.ofNullable(declares.get(h)), "myschool.sn",
                "portail.test.sn=ecole-a.myschool.sn,suspendue.test.sn=ecole-suspendue.myschool.sn", "http://localhost:5173");
        filtre = new TenantResolutionFilter(repo, resolution, "127.0.0.1,10.0.0.5");
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    record Resultat(int statut, String corps, String ecole, boolean plateforme, boolean chaineAppelee) {
    }

    /** Chaîne complète : résolution → (authentification simulée) → contrôle du statut → application. */
    private Resultat executer(MockHttpServletRequest req) throws Exception {
        AtomicReference<String> ecole = new AtomicReference<>();
        AtomicReference<Boolean> plateforme = new AtomicReference<>(false);
        AtomicReference<Boolean> appelee = new AtomicReference<>(false);
        FilterChain application = (rq, rs) -> {
            appelee.set(true);
            ecole.set(TenantContext.getCurrentOrganization().map(Organization::getSlug).orElse(null));
            plateforme.set(TenantContext.isPlatformMode());
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtre.doFilter(req, res, (rq, rs) -> statut.doFilter(rq, rs, application));
        return new Resultat(res.getStatus(), res.getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                ecole.get(), plateforme.get(), appelee.get());
    }

    private static MockHttpServletRequest requete(String chemin, String hote) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", chemin);
        req.setServerName(hote);
        req.setRemoteAddr("203.0.113.9");   // client quelconque (pas un relais de confiance)
        return req;
    }

    private static void connecter(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("compte", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    // ── Adresse demandée ────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void sousDomaineDeLaPlateformeSertLEcoleSansEnTete() throws Exception {
        Resultat r = executer(requete("/api/admin/stats", "ecole-a.myschool.sn"));
        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ecole()).isEqualTo("ecole-a");
    }

    @Test
    void domaineDeclareEtPointeSertLEcole() throws Exception {
        assertThat(executer(requete("/api/admin/stats", "portail.lycee-b.sn")).ecole()).isEqualTo("ecole-b");
        assertThat(executer(requete("/api/admin/stats", "portail.test.sn")).ecole()).isEqualTo("ecole-a");
    }

    @Test
    void domainePointeMaisNonDeclareNeSertAucuneEcole() throws Exception {
        Resultat r = executer(requete("/api/admin/stats", "non-declare.sn"));
        assertThat(r.statut()).isEqualTo(404);
        assertThat(r.chaineAppelee()).isFalse();
    }

    @Test
    void xForwardedHostLuSeulementDepuisUnRelaisDeConfiance() throws Exception {
        MockHttpServletRequest relais = requete("/api/admin/stats", "backend.interne");
        relais.setRemoteAddr("10.0.0.5");
        relais.addHeader("X-Forwarded-Host", "Portail.Test.sn:443, autre.interne");
        assertThat(executer(relais).ecole()).isEqualTo("ecole-a");

        MockHttpServletRequest client = requete("/api/admin/stats", "backend.interne");
        client.addHeader("X-Forwarded-Host", "portail.test.sn");
        assertThat(executer(client).statut()).as("en-tête d'un client direct ignoré : Host seul").isEqualTo(404);
    }

    @Test
    void parDefautAucunRelaisDeConfianceMemeDepuis127() throws Exception {
        // B2 (revue phase 2) : derrière nginx ou le proxy Vite, la connexion vient de 127.0.0.1 ; l'en-tête est ignoré
        TenantResolutionFilter parDefaut = new TenantResolutionFilter(repo, new ResolutionHoteEcole(repo,
                h -> Optional.ofNullable(dns.get(h)), h -> Optional.ofNullable(declares.get(h)), "myschool.sn",
                "portail.test.sn=ecole-a.myschool.sn", "http://localhost:5173"));
        MockHttpServletRequest req = requete("/api/admin/stats", "api.myschool.sn");
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Forwarded-Host", "ecole-a.myschool.sn");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<Boolean> appelee = new AtomicReference<>(false);
        parDefaut.doFilter(req, res, (rq, rs) -> appelee.set(true));
        assertThat(res.getStatus()).isEqualTo(404);
        assertThat(appelee.get()).isFalse();
        assertThat(parDefaut.hoteDemande(req)).isEqualTo("api.myschool.sn");
    }

    @Test
    void ecoleEnAttenteNonServieParSonAdresse() throws Exception {
        Resultat r = executer(requete("/api/admin/stats", "ecole-attente.myschool.sn"));
        assertThat(r.statut()).isEqualTo(404);
        assertThat(r.corps()).contains(TenantResolutionFilter.MESSAGE_AUCUNE_ECOLE_A_CETTE_ADRESSE);
        assertThat(r.chaineAppelee()).isFalse();
    }

    @Test
    void hoteSansEcoleEtNomReserve404() throws Exception {
        assertThat(executer(requete("/api/admin/stats", "www.myschool.sn")).statut()).isEqualTo(404);
        assertThat(executer(requete("/api/admin/stats", "inconnu.ailleurs.sn")).statut()).isEqualTo(404);
        assertThat(executer(requete("/api/admin/stats", "inconnue.myschool.sn")).statut()).isEqualTo(404);
    }

    @Test
    void replisLocalEtEnTeteSurHoteNeutre() throws Exception {
        assertThat(executer(requete("/api/admin/stats", "localhost")).ecole()).isEqualTo("ecole-221");
        MockHttpServletRequest vercel = requete("/api/admin/stats", "backend.onrender.com");
        vercel.addHeader("Origin", "https://papa-221.vercel.app");
        vercel.addHeader("X-Tenant-Slug", "ecole-b");
        assertThat(executer(vercel).ecole()).isEqualTo("ecole-b");
        MockHttpServletRequest parId = requete("/api/admin/stats", "api.myschool.sn");
        parId.addHeader("X-Tenant-ID", parSlug.get("ecole-a").getId());
        assertThat(executer(parId).ecole()).isEqualTo("ecole-a");
    }

    @Test
    void refererNEstPlusUneSourceDEcole() throws Exception {
        MockHttpServletRequest req = requete("/api/admin/stats", "backend.onrender.com");
        req.addHeader("Referer", "https://papa-221.vercel.app/t/ecole-b/admin");
        assertThat(executer(req).statut()).isEqualTo(404);
        MockHttpServletRequest local = requete("/api/admin/stats", "127.0.0.1");
        local.addHeader("Referer", "http://127.0.0.1:5174/t/ecole-b/admin");
        assertThat(executer(local).ecole()).as("repli local, Referer ignoré").isEqualTo("ecole-221");
    }

    // ── Hôte d'école : en-tête différent → 403 ──────────────────────────────────────────────────────────────────

    @Test
    void hoteDeLEcoleAEtEnTeteEcoleBRefuse403() throws Exception {
        MockHttpServletRequest req = requete("/api/admin/stats", "ecole-a.myschool.sn");
        req.addHeader("X-Tenant-Slug", "ecole-b");
        Resultat r = executer(req);
        assertThat(r.statut()).isEqualTo(403);
        assertThat(r.corps()).contains(TenantResolutionFilter.MESSAGE_INCOHERENCE).contains("TENANT_ADDRESS_MISMATCH");
        assertThat(r.chaineAppelee()).isFalse();

        MockHttpServletRequest domainePropre = requete("/api/admin/stats", "portail.lycee-b.sn");
        domainePropre.addHeader("X-Tenant-ID", parSlug.get("ecole-a").getId());
        assertThat(executer(domainePropre).statut()).isEqualTo(403);

        MockHttpServletRequest inconnue = requete("/api/admin/stats", "portail.lycee-b.sn");
        inconnue.addHeader("X-Tenant-Slug", "n-existe-pas");
        assertThat(executer(inconnue).statut()).isEqualTo(403);
    }

    @Test
    void hoteEtEnTeteDeLaMemeEcoleServis() throws Exception {
        MockHttpServletRequest req = requete("/api/admin/stats", "portail.lycee-b.sn");
        req.addHeader("X-Tenant-Slug", "ecole-b");
        assertThat(executer(req).ecole()).isEqualTo("ecole-b");
        MockHttpServletRequest parId = requete("/api/admin/stats", "ecole-a.myschool.sn");
        parId.addHeader("X-Tenant-ID", parSlug.get("ecole-a").getId());
        assertThat(executer(parId).ecole()).isEqualTo("ecole-a");
    }

    // ── F1 : statut contrôlé après l'authentification ───────────────────────────────────────────────────────────

    @Test
    void ecoleSuspendueRefuseeMemeConnecteeEtAvecFauxJeton() throws Exception {
        MockHttpServletRequest fauxJeton = requete("/api/admin/stats", "127.0.0.1");
        fauxJeton.addHeader("X-Tenant-Slug", "ecole-suspendue");
        fauxJeton.addHeader("Authorization", "Bearer n-importe-quoi");
        fauxJeton.setCookies(new jakarta.servlet.http.Cookie("auth_token", "n-importe-quoi"));
        Resultat r = executer(fauxJeton);
        assertThat(r.statut()).isEqualTo(403);
        assertThat(r.chaineAppelee()).isFalse();
        assertThat(r.corps()).contains("TENANT_SUSPENDED").contains("suspendu");

        connecter("ADMIN");   // jeton valide d'un administrateur de l'école
        MockHttpServletRequest admin = requete("/api/student/dashboard", "127.0.0.1");
        admin.addHeader("X-Tenant-Slug", "ecole-suspendue");
        assertThat(executer(admin).statut()).isEqualTo(403);

        assertThat(executer(requete("/api/admin/stats", "suspendue.test.sn")).statut()).as("par son domaine").isEqualTo(403);
    }

    @Test
    void ecoleArchiveeOuRejeteeRefuseeMemeConnectee() throws Exception {
        connecter("ADMIN");
        for (String slug : new String[]{"ecole-archivee", "ecole-rejetee"}) {
            MockHttpServletRequest req = requete("/api/admin/stats", "127.0.0.1");
            req.addHeader("X-Tenant-Slug", slug);
            Resultat r = executer(req);
            assertThat(r.statut()).as(slug).isEqualTo(403);
            assertThat(r.corps()).as(slug).contains("TENANT_NOT_ACTIVE").contains("plus actif");
        }
    }

    @Test
    void superAdministrateurAuJetonValideExempte() throws Exception {
        connecter("SUPER_ADMIN");
        MockHttpServletRequest req = requete("/api/admin/stats", "127.0.0.1");
        req.addHeader("X-Tenant-Slug", "ecole-suspendue");
        assertThat(executer(req).ecole()).isEqualTo("ecole-suspendue");
    }

    @Test
    void ecoleEnAttenteGardeSonAccesParEnTete() throws Exception {
        MockHttpServletRequest req = requete("/api/admin/stats", "127.0.0.1");
        req.addHeader("X-Tenant-Slug", "ecole-attente");
        assertThat(executer(req).ecole()).isEqualTo("ecole-attente");
    }

    @Test
    void routesDuSuperAdministrateurNonConcernees() throws Exception {
        MockHttpServletRequest req = requete("/api/v1/super-admin/tenants", "127.0.0.1");
        req.addHeader("X-Tenant-Slug", "ecole-suspendue");
        Resultat r = executer(req);
        assertThat(r.chaineAppelee()).isTrue();
        assertThat(r.plateforme()).isTrue();
    }

    // ── I3 : /api/sync rattaché à une école ─────────────────────────────────────────────────────────────────────

    @Test
    void synchronisationSansEcoleOuEcoleSuspendueRefusee() throws Exception {
        MockHttpServletRequest sansEcole = new MockHttpServletRequest("POST", "/api/sync");
        sansEcole.setServerName("api.myschool.sn");
        sansEcole.setContent("[{\"type\":\"ATTENDANCE_SELF\",\"payload\":{\"type\":\"ENTREE\"}}]".getBytes());
        Resultat r = executer(sansEcole);
        assertThat(r.statut()).isEqualTo(404);
        assertThat(r.chaineAppelee()).isFalse();

        connecter("ETUDIANT");
        MockHttpServletRequest suspendue = new MockHttpServletRequest("POST", "/api/sync");
        suspendue.setServerName("api.myschool.sn");
        suspendue.addHeader("X-Tenant-Slug", "ecole-suspendue");
        assertThat(executer(suspendue).statut()).isEqualTo(403);

        MockHttpServletRequest voisine = new MockHttpServletRequest("POST", "/api/syncXYZ");
        voisine.setServerName("api.myschool.sn");
        assertThat(executer(voisine).plateforme()).as("/api/sync* n'est plus « plateforme »").isFalse();

        MockHttpServletRequest active = new MockHttpServletRequest("POST", "/api/sync");
        active.setServerName("api.myschool.sn");
        active.addHeader("X-Tenant-Slug", "ecole-a");
        assertThat(executer(active).ecole()).isEqualTo("ecole-a");

        MockHttpServletRequest version = new MockHttpServletRequest("GET", "/api/sync/check");
        version.setServerName("api.myschool.sn");
        assertThat(executer(version).plateforme()).as("vérification de version : sans école, sans donnée").isTrue();
    }

    // ── Divers ──────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void preverificationCorsTraverseSansResolution() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/api/admin/stats");
        req.setServerName("api.myschool.sn");
        req.addHeader("Origin", "https://ecole-a.myschool.sn");
        req.addHeader("Access-Control-Request-Method", "GET");
        Resultat r = executer(req);
        assertThat(r.chaineAppelee()).isTrue();
        assertThat(r.ecole()).isNull();
    }

    @Test
    void identifiantRecopieSerialiseDansLeJson404() throws Exception {
        MockHttpServletRequest req = requete("/api/admin/stats", "127.0.0.1");
        req.addHeader("X-Tenant-Slug", "x\",\"admin\":true,\"y\":\"z");
        Resultat r = executer(req);
        assertThat(r.statut()).isEqualTo(404);
        Map<String, Object> corps = new com.fasterxml.jackson.databind.ObjectMapper().readValue(r.corps(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
        assertThat(corps).doesNotContainKey("admin");
        assertThat(corps.get("message").toString()).contains("x\",\"admin\":true");
        assertThat(corps.get("code")).isEqualTo("TENANT_NOT_FOUND");
    }
}
