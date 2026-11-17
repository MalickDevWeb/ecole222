package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.DomaineEcoleJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.DomaineEcoleJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.RegistreDomainesJpa;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.RegistreDomaines;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolutionHoteEcole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Domaines propres déclarés (décision du Tech Lead du 15/11 : pointage + déclaration) : déclaration, quota, unicité,
 * vérification, désactivation après 3 échecs, retour à ACTIF, suppression (le domaine cesse d'être servi aussitôt).
 */
class DomainesEcoleServiceTest {

    private final Map<String, DomaineEcoleJpaEntity> table = new ConcurrentHashMap<>();
    private final Map<String, String> dns = new ConcurrentHashMap<>();
    private final OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
    private final Organization ecoleA = ecole("ecole-a");
    private final Organization ecoleB = ecole("ecole-b");
    private ResolutionHoteEcole resolution;
    private DomainesEcoleService service;

    static Organization ecole(String slug) {
        return OrganizationFactory.create("École " + slug, OrganizationType.ECOLE, OrganizationStatus.ACTIVE, slug,
                "SN", "Dakar", null, null, slug + "@exemple.sn", null, null, null, null, null);
    }

    @BeforeEach
    void preparer() {
        DomaineEcoleJpaRepository repo = Mockito.mock(DomaineEcoleJpaRepository.class);
        Mockito.when(repo.findByHostname(anyString())).thenAnswer(i ->
                table.values().stream().filter(d -> d.getHostname().equals(i.getArgument(0))).findFirst());
        Mockito.when(repo.countByOrganizationId(anyString())).thenAnswer(i ->
                table.values().stream().filter(d -> d.getOrganizationId().equals(i.getArgument(0))).count());
        Mockito.when(repo.findByOrganizationIdOrderByCreatedAtAsc(anyString())).thenAnswer(i ->
                table.values().stream().filter(d -> d.getOrganizationId().equals(i.getArgument(0))).toList());
        Mockito.when(repo.findAll()).thenAnswer(i -> List.copyOf(table.values()));
        Mockito.when(repo.save(any())).thenAnswer(i -> {
            DomaineEcoleJpaEntity d = i.getArgument(0);
            table.put(d.getId(), d);
            return d;
        });
        Mockito.doAnswer(i -> table.remove(((DomaineEcoleJpaEntity) i.getArgument(0)).getId())).when(repo).delete(any());
        for (Organization o : new Organization[]{ecoleA, ecoleB}) {
            Mockito.when(ecoles.findBySlug(o.getSlug())).thenReturn(Optional.of(o));
            Mockito.when(ecoles.findById(o.getId())).thenReturn(Optional.of(o));
        }
        Mockito.when(ecoles.findBySlug(anyString())).thenAnswer(i ->
                Optional.ofNullable(Map.of("ecole-a", ecoleA, "ecole-b", ecoleB).get(i.<String>getArgument(0))));
        resolution = new ResolutionHoteEcole(ecoles, h -> Optional.ofNullable(dns.get(h)), new RegistreDomainesJpa(repo),
                "myschool.sn", "", "http://localhost:5173");
        service = new DomainesEcoleService(repo, ecoles, resolution);
    }

    private static void refus(Runnable r, HttpStatus statut, String extrait) {
        assertThatThrownBy(r::run).isInstanceOf(ResponseStatusException.class).satisfies(e -> {
            assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(statut);
            assertThat(((ResponseStatusException) e).getReason()).contains(extrait);
        });
    }

    @Test
    void declarationPointeeActiveEtServie() {
        dns.put("portail.lycee.sn", "ecole-a.myschool.sn");
        Map<String, Object> r = service.declarer(ecoleA, "https://Portail.Lycee.SN/", "u-admin");
        assertThat(r).containsOnlyKeys("domaine", "statut", "pointeVersCetteEcole", "message");
        assertThat(r.get("domaine")).isEqualTo("portail.lycee.sn");
        assertThat(r.get("statut")).isEqualTo(RegistreDomaines.ACTIF);
        assertThat(r.get("pointeVersCetteEcole")).isEqualTo(true);
        assertThat(resolution.ecoleServieParHote("portail.lycee.sn")).contains(ecoleA);
        assertThat(table.values().iterator().next().getCreePar()).isEqualTo("u-admin");

        Map<String, Object> liste = service.lister(ecoleA);
        assertThat(liste.get("adresseEcole")).isEqualTo("https://ecole-a.myschool.sn");
        assertThat(liste.get("cible")).isEqualTo("ecole-a.myschool.sn");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> domaines = (List<Map<String, Object>>) liste.get("domaines");
        assertThat(domaines).hasSize(1);
        assertThat(domaines.get(0)).containsOnlyKeys("domaine", "statut", "pointeVersCetteEcole", "verifieLe", "message");
        assertThat(domaines.get(0).get("verifieLe")).isNotNull();
    }

    @Test
    void declarationNonPointeeEnAttenteAvecLeCnameAttendu() {
        Map<String, Object> r = service.declarer(ecoleA, "portail.lycee.sn", null);
        assertThat(r.get("statut")).isEqualTo(RegistreDomaines.EN_ATTENTE_POINTAGE);
        assertThat(r.get("pointeVersCetteEcole")).isEqualTo(false);
        assertThat(r.get("message").toString()).contains("portail.lycee.sn CNAME ecole-a.myschool.sn");
        assertThat(resolution.ecoleServieParHote("portail.lycee.sn")).isEmpty();

        dns.put("portail.lycee.sn", "ecole-a.myschool.sn");
        Map<String, Object> v = service.verifier(ecoleA, "portail.lycee.sn");
        assertThat(v.get("statut")).isEqualTo(RegistreDomaines.ACTIF);
        assertThat(resolution.ecoleServieParHote("portail.lycee.sn")).contains(ecoleA);
    }

    @Test
    void adressesRefusees400() {
        for (String d : new String[]{"", "nimporte quoi", "mauvais_nom.sn", "192.168.1.10", "myschool.sn", "www.myschool.sn",
                "ecole-a.myschool.sn", "localhost", "xn--zz.sn"}) {
            assertThatThrownBy(() -> service.declarer(ecoleA, d, null)).as(d).isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    @Test
    void unHostnameUneSeuleEcoleEtQuotaDeTrois() {
        service.declarer(ecoleA, "portail.lycee.sn", null);
        refus(() -> service.declarer(ecoleB, "portail.lycee.sn", null), HttpStatus.CONFLICT, "un autre établissement");
        assertThatThrownBy(() -> service.declarer(ecoleB, "portail.lycee.sn", null))
                .satisfies(e -> assertThat(((ResponseStatusException) e).getReason()).doesNotContain("ecole-a"));
        refus(() -> service.declarer(ecoleA, "PORTAIL.lycee.sn", null), HttpStatus.CONFLICT, "votre établissement");
        service.declarer(ecoleA, "www.lycee.sn", null);
        service.declarer(ecoleA, "eleves.lycee.sn", null);
        refus(() -> service.declarer(ecoleA, "parents.lycee.sn", null), HttpStatus.CONFLICT, "Quota");
    }

    @Test
    void domaineDUneAutreEcole404() {
        service.declarer(ecoleA, "portail.lycee.sn", null);
        refus(() -> service.verifier(ecoleB, "portail.lycee.sn"), HttpStatus.NOT_FOUND, "introuvable");
        refus(() -> service.supprimer(ecoleB, "portail.lycee.sn"), HttpStatus.NOT_FOUND, "introuvable");
        assertThat(table).hasSize(1);
    }

    @Test
    void pointeVersUneAutreEcoleNonServiSansLaNommer() {
        dns.put("portail.lycee.sn", "ecole-b.myschool.sn");
        Map<String, Object> r = service.declarer(ecoleA, "portail.lycee.sn", null);
        assertThat(r.get("pointeVersCetteEcole")).isEqualTo(false);
        assertThat(r.get("message").toString()).contains("un autre établissement").doesNotContain("ecole-b");
        assertThat(resolution.ecoleServieParHote("portail.lycee.sn")).isEmpty();
    }

    @Test
    void troisEchecsDesactiventPuisLeRetourDuCnameReactive() {
        dns.put("portail.lycee.sn", "ecole-a.myschool.sn");
        service.declarer(ecoleA, "portail.lycee.sn", null);
        dns.remove("portail.lycee.sn");
        service.verifierTout();
        service.verifierTout();
        assertThat(table.values().iterator().next().getStatut()).isEqualTo(RegistreDomaines.ACTIF);
        service.verifierTout();
        DomaineEcoleJpaEntity d = table.values().iterator().next();
        assertThat(d.getStatut()).isEqualTo(RegistreDomaines.DESACTIVE);
        assertThat(d.getEchecsConsecutifs()).isEqualTo(3);
        dns.put("portail.lycee.sn", "ecole-a.myschool.sn");
        assertThat(resolution.ecoleServieParHote("portail.lycee.sn")).as("désactivé : plus servi").isEmpty();

        service.verifierTout();
        d = table.values().iterator().next();
        assertThat(d.getStatut()).isEqualTo(RegistreDomaines.ACTIF);
        assertThat(d.getEchecsConsecutifs()).isZero();
        assertThat(resolution.ecoleServieParHote("portail.lycee.sn")).contains(ecoleA);
    }

    @Test
    void suppressionArreteAussitotLeService() {
        dns.put("portail.lycee.sn", "ecole-a.myschool.sn");
        service.declarer(ecoleA, "portail.lycee.sn", null);
        assertThat(resolution.ecoleServieParHote("portail.lycee.sn")).isPresent();
        service.supprimer(ecoleA, "portail.lycee.sn");
        assertThat(table).isEmpty();
        assertThat(resolution.ecoleServieParHote("portail.lycee.sn")).isEmpty();
        assertThat(resolution.estHoteDEcole("portail.lycee.sn")).isFalse();
    }

    @Test
    void accaparementLeProprietaireDuCnameRecupereLaDeclaration() {
        // B7 (revue phase 2) : B déclare le domaine de A (CNAME → A) avant elle ; A le récupère
        dns.put("portail.lycee-a.sn", "ecole-a.myschool.sn");
        Map<String, Object> deB = service.declarer(ecoleB, "portail.lycee-a.sn", "u-b");
        assertThat(deB.get("statut")).isEqualTo(RegistreDomaines.EN_ATTENTE_POINTAGE);
        assertThat(resolution.ecoleServieParHote("portail.lycee-a.sn")).isEmpty();

        Map<String, Object> deA = service.declarer(ecoleA, "portail.lycee-a.sn", "u-a");
        assertThat(deA.get("statut")).isEqualTo(RegistreDomaines.ACTIF);
        assertThat(table).hasSize(1);
        DomaineEcoleJpaEntity d = table.values().iterator().next();
        assertThat(d.getOrganizationId()).isEqualTo(ecoleA.getId());
        assertThat(d.getCreePar()).isEqualTo("u-a");
        assertThat(resolution.ecoleServieParHote("portail.lycee-a.sn")).contains(ecoleA);
        refus(() -> service.verifier(ecoleB, "portail.lycee-a.sn"), HttpStatus.NOT_FOUND, "introuvable");
    }

    @Test
    void pasDeTransfertSiLaDeclarationEstActiveOuSiLeCnameNeMenePasAuDemandeur() {
        dns.put("portail.lycee-b.sn", "ecole-b.myschool.sn");
        service.declarer(ecoleB, "portail.lycee-b.sn", null);   // ACTIF pour B
        refus(() -> service.declarer(ecoleA, "portail.lycee-b.sn", null), HttpStatus.CONFLICT, "un autre établissement");
        service.declarer(ecoleB, "portail.nulle-part.sn", null); // EN_ATTENTE, CNAME absent
        refus(() -> service.declarer(ecoleA, "portail.nulle-part.sn", null), HttpStatus.CONFLICT, "un autre établissement");
        assertThat(table.values()).allMatch(d -> d.getOrganizationId().equals(ecoleB.getId()));
    }

    @Test
    void panneDnsNeCompteJamaisCommeUnEchec() {
        java.util.concurrent.atomic.AtomicBoolean panne = new java.util.concurrent.atomic.AtomicBoolean(false);
        DomaineEcoleJpaRepository repo = Mockito.mock(DomaineEcoleJpaRepository.class);
        Mockito.when(repo.findByHostname(anyString())).thenAnswer(i ->
                table.values().stream().filter(d -> d.getHostname().equals(i.getArgument(0))).findFirst());
        Mockito.when(repo.findAll()).thenAnswer(i -> List.copyOf(table.values()));
        Mockito.when(repo.save(any())).thenAnswer(i -> { DomaineEcoleJpaEntity d = i.getArgument(0); table.put(d.getId(), d); return d; });
        ResolutionHoteEcole r = new ResolutionHoteEcole(ecoles, h -> {
            if (panne.get()) throw new ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolveurDns.Indisponible("panne");
            return Optional.ofNullable(dns.get(h));
        }, new RegistreDomainesJpa(repo), "myschool.sn", "", "http://localhost:5173");
        DomainesEcoleService s = new DomainesEcoleService(repo, ecoles, r);
        dns.put("portail.lycee.sn", "ecole-a.myschool.sn");
        s.declarer(ecoleA, "portail.lycee.sn", null);
        panne.set(true);
        for (int i = 0; i < 5; i++) s.verifierTout();
        DomaineEcoleJpaEntity d = table.values().iterator().next();
        assertThat(d.getStatut()).isEqualTo(RegistreDomaines.ACTIF);
        assertThat(d.getEchecsConsecutifs()).isZero();
        Map<String, Object> v = s.verifier(ecoleA, "portail.lycee.sn");
        assertThat(v.get("message").toString()).contains("n'a pas pu être lu");

        Map<String, Object> nouveau = s.declarer(ecoleA, "www.lycee.sn", null);
        assertThat(nouveau.get("statut")).isEqualTo(RegistreDomaines.EN_ATTENTE_POINTAGE);
        assertThat(table.values().stream().filter(x -> x.getHostname().equals("www.lycee.sn")).findFirst().orElseThrow()
                .getEchecsConsecutifs()).isZero();
    }

    @Test
    void domaineNuExpliqueLaRedirection() {
        Map<String, Object> r = service.declarer(ecoleA, "lycee.sn", null);
        assertThat(r.get("message").toString()).contains("domaine nu").contains("www.lycee.sn");
    }
}
