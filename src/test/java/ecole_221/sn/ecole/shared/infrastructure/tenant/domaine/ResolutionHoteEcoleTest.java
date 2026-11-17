package ecole_221.sn.ecole.shared.infrastructure.tenant.domaine;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Domaine propre d'une école (décisions du Tech Lead du 15/11 : pointage DNS + déclaration) : résolution hôte → école.
 * DNS simulé (ResolveurDns) et registre des déclarations simulé : aucun accès réseau ni base.
 */
class ResolutionHoteEcoleTest {

    static class HorlogeReglable extends Clock {
        volatile Instant t = Instant.parse("2026-11-15T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return t; }
    }

    /** DNS simulé : table nom → cible CNAME, nombre de lectures compté. */
    static class DnsSimule implements ResolveurDns {
        final Map<String, String> table = new java.util.concurrent.ConcurrentHashMap<>();
        final AtomicInteger lectures = new AtomicInteger();
        volatile CountDownLatch frein;
        @Override public Optional<String> cname(String hote) {
            lectures.incrementAndGet();
            if (frein != null) {
                try { frein.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            return Optional.ofNullable(table.get(hote));
        }
    }

    static Organization ecole(String slug, OrganizationStatus statut) {
        return OrganizationFactory.create("École " + slug, OrganizationType.ECOLE, statut, slug,
                "SN", "Dakar", null, null, slug + "@exemple.sn", null, null, null, null, null);
    }

    private final OrganizationRepositoryPort repo = Mockito.mock(OrganizationRepositoryPort.class);
    private final DnsSimule dns = new DnsSimule();
    private final Map<String, RegistreDomaines.DomaineDeclare> declares = new HashMap<>();
    private final HorlogeReglable horloge = new HorlogeReglable();
    private final Organization ecoleA = ecole("ecole-a", OrganizationStatus.ACTIVE);
    private final Organization ecoleB = ecole("ecole-b", OrganizationStatus.ACTIVE);
    private final Organization attente = ecole("ecole-attente", OrganizationStatus.PENDING);
    private final Organization suspendue = ecole("ecole-suspendue", OrganizationStatus.SUSPENDED);
    private ResolutionHoteEcole resolution;

    private void declarer(String hote, Organization o, String statut) {
        declares.put(hote, new RegistreDomaines.DomaineDeclare(hote, o.getId(), statut));
    }

    @BeforeEach
    void preparer() {
        Mockito.when(repo.findBySlug(anyString())).thenReturn(Optional.empty());
        for (Organization o : new Organization[]{ecoleA, ecoleB, attente, suspendue}) {
            Mockito.when(repo.findBySlug(o.getSlug())).thenReturn(Optional.of(o));
        }
        resolution = new ResolutionHoteEcole(repo, dns, h -> Optional.ofNullable(declares.get(h)), "MySchool.sn",
                "portail.test.sn=ecole-a.myschool.sn, attente.test.sn=ecole-attente.myschool.sn, mauvaise-entree",
                "https://papa-221.vercel.app");
        resolution.horloge = horloge;
    }

    @Test
    void sousDomaineDeLaPlateformeDonneLeSlugSansLectureDns() {
        assertThat(resolution.slugDepuisHote("ecole-a.myschool.sn")).contains("ecole-a");
        assertThat(resolution.slugDepuisHote("ECOLE-A.MySchool.SN.:443")).contains("ecole-a");
        assertThat(resolution.slugDepuisHote("inconnue.myschool.sn")).contains("inconnue");   // école absente : vu plus loin
        assertThat(resolution.estHoteDEcole("inconnue.myschool.sn")).isTrue();
        assertThat(dns.lectures.get()).isZero();
    }

    @Test
    void nomsDInfrastructureEtHotesDeLaPlateformeNeDesignentAucuneEcole() {
        for (String h : new String[]{"myschool.sn", "www.myschool.sn", "app.myschool.sn", "api.myschool.sn", "admin.myschool.sn",
                "super-admin.myschool.sn", "mail.myschool.sn", "cdn.myschool.sn", "status.myschool.sn", "docs.myschool.sn",
                "login.myschool.sn", "paiement.myschool.sn", "wave.myschool.sn", "mta-sts.myschool.sn", "staging.myschool.sn",
                "a.b.myschool.sn", "localhost", "127.0.0.1", "192.168.1.10", "papa-221.vercel.app"}) {
            assertThat(resolution.slugDepuisHote(h)).as(h).isEmpty();
            assertThat(resolution.estHoteDEcole(h)).as(h).isFalse();
        }
        assertThat(resolution.estHotePlateforme("papa-221.vercel.app")).as("hôte de FRONTEND_URL").isTrue();
        // l'école existante ecole-221 reste servie (réservé seulement à la création)
        assertThat(resolution.slugDepuisHote("ecole-221.myschool.sn")).contains("ecole-221");
        assertThat(dns.lectures.get()).isZero();
    }

    @Test
    void normalisationMinusculesPortPointFinalEtPunycode() {
        assertThat(ResolutionHoteEcole.normaliser(" Portail.Ecole.SN.:8443 ")).isEqualTo("portail.ecole.sn");
        assertThat(ResolutionHoteEcole.normaliser("école-thiès.sn")).isEqualTo(java.net.IDN.toASCII("école-thiès.sn"));
        assertThat(ResolutionHoteEcole.normaliser("école-thiès.sn")).startsWith("xn--");
        assertThat(ResolutionHoteEcole.normaliser("xn--zz.sn")).as("punycode mal formé").isNull();
        assertThat(ResolutionHoteEcole.normaliser("mauvais_nom.sn")).isNull();
        assertThat(ResolutionHoteEcole.normaliser("[::1]:8080")).isNull();
        assertThat(ResolutionHoteEcole.normaliser("a..sn")).isNull();
        assertThat(ResolutionHoteEcole.normaliser("x".repeat(64) + ".sn")).isNull();
    }

    @Test
    void domaineNonDeclareJamaisServiNiLuDansLeDns() {
        dns.table.put("portail.lycee.sn", "ecole-a.myschool.sn");
        assertThat(resolution.slugDepuisHote("portail.lycee.sn")).isEmpty();
        assertThat(resolution.estHoteDEcole("portail.lycee.sn")).isFalse();
        assertThat(dns.lectures.get()).as("aucune lecture DNS pour un nom non déclaré").isZero();
        // le pointage seul (vérification par l'administrateur) reste lisible
        assertThat(resolution.slugDuPointage("portail.lycee.sn", false)).contains("ecole-a");
    }

    @Test
    void domaineDeclareEtPointeServiPourSonEcoleSeulement() {
        dns.table.put("portail.lycee.sn", "lycee.hebergeur.net.");
        dns.table.put("lycee.hebergeur.net", "Ecole-A.myschool.sn.");
        declarer("portail.lycee.sn", ecoleA, RegistreDomaines.EN_ATTENTE_POINTAGE);
        assertThat(resolution.slugDepuisHote("portail.lycee.sn")).as("chaîne de 2 CNAME").contains("ecole-a");
        assertThat(resolution.domaineAutorise("portail.lycee.sn", ecoleA)).isTrue();
        assertThat(resolution.domaineAutorise("portail.lycee.sn", ecoleB)).isFalse();

        // déclaré par l'école B mais pointé vers l'école A : non servi
        dns.table.put("portail.b.sn", "ecole-a.myschool.sn");
        declarer("portail.b.sn", ecoleB, RegistreDomaines.ACTIF);
        assertThat(resolution.slugDepuisHote("portail.b.sn")).isEmpty();
        assertThat(resolution.estHoteDEcole("portail.b.sn")).isTrue();

        // désactivé (3 échecs) : plus servi, plus lu
        declarer("portail.lycee.sn", ecoleA, RegistreDomaines.DESACTIVE);
        assertThat(resolution.slugDepuisHote("portail.lycee.sn")).isEmpty();
        assertThat(resolution.domaineAutorise("portail.lycee.sn", ecoleA)).isFalse();
    }

    @Test
    void tableDnsStatiqueRemplaceLeVraiDns() {
        declarer("portail.test.sn", ecoleA, RegistreDomaines.ACTIF);
        dns.table.put("portail.test.sn", "ecole-b.myschool.sn");   // ignoré : la table statique passe d'abord
        assertThat(resolution.slugDepuisHote("Portail.Test.SN")).contains("ecole-a");
        assertThat(dns.lectures.get()).isZero();
    }

    @Test
    void chaineDePlusDeCinqSautsBoucleOuNomReserveRefusees() {
        for (int i = 0; i < 5; i++) {
            dns.table.put("s" + i + ".sn", "s" + (i + 1) + ".sn");
        }
        dns.table.put("s5.sn", "ecole-a.myschool.sn");
        assertThat(resolution.slugDuPointage("s0.sn", false)).as("6 sauts").isEmpty();
        assertThat(resolution.slugDuPointage("s1.sn", false)).as("5 sauts").contains("ecole-a");

        dns.table.put("boucle-a.sn", "boucle-b.sn");
        dns.table.put("boucle-b.sn", "boucle-a.sn");
        assertThat(resolution.slugDuPointage("boucle-a.sn", false)).isEmpty();

        dns.table.put("vers-www.sn", "www.myschool.sn");
        assertThat(resolution.slugDuPointage("vers-www.sn", false)).as("CNAME vers un nom réservé").isEmpty();
        dns.table.put("ailleurs.sn", "site.autre-hebergeur.com");
        assertThat(resolution.slugDuPointage("ailleurs.sn", false)).isEmpty();
    }

    @Test
    void memoireDixMinutesSiTrouveSoixanteSecondesSinonEtOubli() {
        dns.table.put("portail.lycee.sn", "ecole-a.myschool.sn");
        resolution.slugDuPointage("portail.lycee.sn", true);
        resolution.slugDuPointage("portail.lycee.sn", true);
        assertThat(dns.lectures.get()).isEqualTo(1);
        horloge.t = horloge.t.plusSeconds(9 * 60);
        resolution.slugDuPointage("portail.lycee.sn", true);
        assertThat(dns.lectures.get()).isEqualTo(1);
        horloge.t = horloge.t.plusSeconds(2 * 60);
        resolution.slugDuPointage("portail.lycee.sn", true);
        assertThat(dns.lectures.get()).isEqualTo(2);

        int avant = dns.lectures.get();
        assertThat(resolution.slugDuPointage("pas-encore.sn", true)).isEmpty();
        dns.table.put("pas-encore.sn", "ecole-a.myschool.sn");
        assertThat(resolution.slugDuPointage("pas-encore.sn", true)).as("absence gardée au plus 60 s").isEmpty();
        horloge.t = horloge.t.plusSeconds(61);
        assertThat(resolution.slugDuPointage("pas-encore.sn", true)).contains("ecole-a");
        assertThat(dns.lectures.get()).isEqualTo(avant + 2);

        resolution.oublier("pas-encore.sn");
        dns.table.remove("pas-encore.sn");
        assertThat(resolution.slugDuPointage("pas-encore.sn", true)).as("oublié : relu").isEmpty();
    }

    @Test
    void memoireBorneeSansVidageTotal() {
        dns.table.put("garde.sn", "ecole-a.myschool.sn");
        for (int i = 0; i < ResolutionHoteEcole.TAILLE_MAX_CACHE + 50; i++) {
            if (i % 1000 == 0) {
                resolution.slugDuPointage("garde.sn", true);   // nom souvent lu : reste en mémoire (LRU)
            }
            resolution.slugDuPointage("n" + i + ".sn", true);
        }
        assertThat(resolution.tailleCache()).isEqualTo(ResolutionHoteEcole.TAILLE_MAX_CACHE);
        int avant = dns.lectures.get();
        resolution.slugDuPointage("garde.sn", true);
        resolution.slugDuPointage("n" + (ResolutionHoteEcole.TAILLE_MAX_CACHE + 49) + ".sn", true);
        assertThat(dns.lectures.get()).as("entrées récentes conservées : aucune relecture").isEqualTo(avant);
    }

    @Test
    void uneSeuleLectureEnCoursParNom() throws Exception {
        dns.table.put("lent.sn", "ecole-a.myschool.sn");
        dns.frein = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            java.util.List<Future<Optional<String>>> r = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                r.add(pool.submit(() -> resolution.slugDuPointage("lent.sn", true)));
            }
            Thread.sleep(300);
            dns.frein.countDown();
            for (Future<Optional<String>> f : r) {
                assertThat(f.get(10, TimeUnit.SECONDS)).contains("ecole-a");
            }
            assertThat(dns.lectures.get()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void echecDuResolveurNAssocieJamaisDEcole() {
        ResolutionHoteEcole cassee = new ResolutionHoteEcole(repo, h -> { throw new IllegalStateException("DNS hors service"); },
                h -> Optional.ofNullable(declares.get(h)), "myschool.sn", "", "http://localhost:5173");
        declarer("portail.lycee.sn", ecoleA, RegistreDomaines.ACTIF);
        assertThat(cassee.slugDepuisHote("portail.lycee.sn")).isEmpty();
        assertThat(cassee.ecoleServieParHote("portail.lycee.sn")).isEmpty();
    }

    @Test
    void lecturesImpossiblesJamaisGardeesCommeAbsentes() {
        // critère 5 (revue phase 2) : panne DNS ou sémaphore saturé → rien en mémoire ; la lecture suivante réussit
        java.util.concurrent.atomic.AtomicBoolean panne = new java.util.concurrent.atomic.AtomicBoolean(true);
        AtomicInteger lectures = new AtomicInteger();
        ResolutionHoteEcole r = new ResolutionHoteEcole(repo, h -> {
            lectures.incrementAndGet();
            if (panne.get()) throw new ResolveurDns.Indisponible("délai dépassé");
            return Optional.of("ecole-a.myschool.sn");
        }, h -> Optional.ofNullable(declares.get(h)), "myschool.sn", "", "http://localhost:5173");
        r.horloge = horloge;
        assertThat(r.slugDuPointage("portail.lycee.sn", true)).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> r.slugDuPointageStrict("portail.lycee.sn"))
                .isInstanceOf(ResolveurDns.Indisponible.class);
        panne.set(false);
        assertThat(r.slugDuPointage("portail.lycee.sn", true)).as("aucune absence gardée après une panne").contains("ecole-a");
        assertThat(lectures.get()).isEqualTo(3);
        assertThat(r.tailleCache()).isEqualTo(1);
    }

    @Test
    void seuleUneEcoleActiveEstServie() {
        assertThat(resolution.ecoleServieParHote("ecole-a.myschool.sn")).isPresent();
        assertThat(resolution.ecoleServieParHote("ecole-attente.myschool.sn")).isEmpty();
        assertThat(resolution.ecoleServieParHote("ecole-suspendue.myschool.sn")).isEmpty();
        assertThat(resolution.ecoleServieParHote("inconnue.myschool.sn")).isEmpty();
        declarer("attente.test.sn", attente, RegistreDomaines.ACTIF);
        assertThat(resolution.ecoleServieParHote("attente.test.sn")).isEmpty();
        // l'école reste désignée (pour le contrôle de statut), mais n'est pas servie
        assertThat(resolution.ecoleDepuisHote("ecole-suspendue.myschool.sn")).isPresent();
    }

    @Test
    void adresseEtCibleDeLEcole() {
        assertThat(resolution.domainePlateforme()).isEqualTo("myschool.sn");
        assertThat(resolution.cible("ecole-a")).isEqualTo("ecole-a.myschool.sn");
        assertThat(resolution.adresseEcole("ecole-a")).isEqualTo("https://ecole-a.myschool.sn");
    }
}
