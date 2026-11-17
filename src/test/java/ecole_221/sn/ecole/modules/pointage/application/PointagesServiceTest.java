package ecole_221.sn.ecole.modules.pointage.application;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.pointage.infrastructure.persistence.PointageJpaEntity;
import ecole_221.sn.ecole.modules.pointage.infrastructure.persistence.PointageJpaRepository;
import ecole_221.sn.ecole.partage.JetonBadge;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** Pointage d'arrivée / départ par le QR d'entrée signé du vigile. */
class PointagesServiceTest {

    /** Horloge réglable : le QR est signé pour la minute de CETTE horloge, comme le ferait le serveur. */
    static final class Horloge extends Clock {
        Instant instant;
        Horloge(Instant instant) { this.instant = instant; }
        void avancer(Duration d) { instant = instant.plus(d); }
        @Override public ZoneId getZone() { return PointagesService.FUSEAU; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }

    private final JetonBadge jetons = JetonBadge.aleatoire();
    private final PointageJpaRepository depot = Mockito.mock(PointageJpaRepository.class);
    private final OrganizationUserRepositoryPort appartenances = Mockito.mock(OrganizationUserRepositoryPort.class);
    private final List<PointageJpaEntity> enregistres = new ArrayList<>();
    // le QR est vérifié par le vrai JetonBadge à l'heure réelle ; l'horloge du service ne règle que les horodatages,
    // le délai entre deux pointages et le sens (arrivée / départ)
    private final Horloge horloge = new Horloge(Instant.now());
    private PointagesService service;
    private Organization ecoleA;
    private Organization ecoleB;

    @BeforeEach
    void setUp() {
        ecoleA = OrganizationFactory.create("A", OrganizationType.ECOLE, "a@exemple.sn");
        ecoleB = OrganizationFactory.create("B", OrganizationType.ECOLE, "b@exemple.sn");
        TenantContext.setCurrentOrganization(ecoleA);
        Mockito.when(appartenances.findByUserIdAndOrganizationId(anyString(), anyString())).thenReturn(Optional.empty());
        Mockito.when(depot.save(any())).thenAnswer(a -> { enregistres.add(a.getArgument(0)); return a.getArgument(0); });
        Mockito.when(depot.findFirstByUtilisateurIdOrderByHorodatageDesc(anyString())).thenAnswer(a -> enregistres.stream()
                .filter(p -> p.getUtilisateurId().equals(a.getArgument(0))).max(Comparator.comparing(PointageJpaEntity::getHorodatage)));
        Mockito.when(depot.findTop60ByUtilisateurIdOrderByHorodatageDesc(anyString())).thenAnswer(a -> enregistres.stream()
                .filter(p -> p.getUtilisateurId().equals(a.getArgument(0)))
                .sorted(Comparator.comparing(PointageJpaEntity::getHorodatage).reversed()).toList());
        Mockito.when(depot.findByHorodatageBetweenOrderByHorodatageAsc(any(), any())).thenAnswer(a -> enregistres.stream()
                .filter(p -> !p.getHorodatage().isBefore(a.getArgument(0)) && !p.getHorodatage().isAfter(a.getArgument(1)))
                .sorted(Comparator.comparing(PointageJpaEntity::getHorodatage)).toList());
        service = new PointagesService(depot, jetons, appartenances, horloge);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void connecte(String id, RoleUtilisateur role, Organization ecole) {
        User u = UserFactory.reconstructSimple(id, "Ba", "Fatou", id + "@ex.sn", "a", "ETU-" + id, role, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
        Mockito.when(appartenances.findByUserIdAndOrganizationId(id, ecole.getId()))
                .thenReturn(Optional.of(OrganizationUserFactory.create(id, ecole.getId(), role.value())));
    }

    private String qr(Organization ecole) {
        return jetons.contenuEntree(ecole.getId(), JetonBadge.minuteCourante());
    }

    private static HttpStatus statut(Runnable r) {
        try {
            r.run();
        } catch (ResponseStatusException e) {
            return HttpStatus.valueOf(e.getStatusCode().value());
        }
        return null;
    }

    @Test
    void arriveePuisDepartPuisArriveeDecidesParLeServeur() {
        connecte("e1", RoleUtilisateur.ETUDIANT, ecoleA);
        Map<String, Object> r = service.pointer(qr(ecoleA));
        assertThat(r.get("type")).isEqualTo("ARRIVEE");
        assertThat(r.get("prochainType")).isEqualTo("DEPART");
        assertThat(String.valueOf(r.get("message"))).startsWith("Arrivée enregistrée à ");
        horloge.avancer(Duration.ofMinutes(10));
        assertThat(service.pointer(qr(ecoleA)).get("type")).isEqualTo("DEPART");
        horloge.avancer(Duration.ofMinutes(10));
        assertThat(service.pointer(qr(ecoleA)).get("type")).isEqualTo("ARRIVEE");
        assertThat(enregistres).hasSize(3).allSatisfy(p -> {
            assertThat(p.getOrganizationId()).isEqualTo(ecoleA.getId());
            assertThat(p.getRole()).isEqualTo("ETUDIANT");
            assertThat(p.getNom()).isEqualTo("Fatou Ba");
        });
        Map<String, Object> miens = service.mesPointages();
        assertThat((List<?>) miens.get("pointages")).hasSize(3);
        assertThat(miens.get("prochainType")).isEqualTo("DEPART");
    }

    @Test
    void lendemainOnRecommenceParUneArrivee() {
        connecte("p1", RoleUtilisateur.PROFESSEUR, ecoleA);
        assertThat(service.pointer(qr(ecoleA)).get("type")).isEqualTo("ARRIVEE");
        horloge.avancer(Duration.ofDays(1));   // arrivée d'hier sans départ : aujourd'hui commence par une arrivée
        assertThat(service.mesPointages().get("prochainType")).isEqualTo("ARRIVEE");
        assertThat(service.pointer(qr(ecoleA)).get("type")).isEqualTo("ARRIVEE");
        assertThat(enregistres.get(1).getRole()).isEqualTo("PROFESSEUR");
    }

    @Test
    void secondScanEnMoinsDeDeuxMinutesRefuseSansEnregistrer() {
        connecte("e1", RoleUtilisateur.ETUDIANT, ecoleA);
        service.pointer(qr(ecoleA));
        horloge.avancer(Duration.ofSeconds(30));
        assertThat(statut(() -> service.pointer(qr(ecoleA)))).isEqualTo(HttpStatus.CONFLICT);
        assertThat(enregistres).hasSize(1);
    }

    @Test
    void qrEtrangerFalsifieExpireOuDUneAutreEcoleRefuses() {
        connecte("e1", RoleUtilisateur.ETUDIANT, ecoleA);
        long m = JetonBadge.minuteCourante();
        assertThat(statut(() -> service.pointer("https://exemple.sn/menu-cantine"))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statut(() -> service.pointer("221-VIGILE-PORTAL-MAIN"))).isEqualTo(HttpStatus.BAD_REQUEST);   // ancien QR fixe
        assertThat(statut(() -> service.pointer(jetons.contenu("ETU-e1", m)))).isEqualTo(HttpStatus.BAD_REQUEST); // badge élève
        assertThat(statut(() -> service.pointer(JetonBadge.aleatoire().contenuEntree(ecoleA.getId(), m)))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statut(() -> service.pointer(jetons.contenuEntree(ecoleA.getId(), m - 5)))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statut(() -> service.pointer(qr(ecoleB)))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statut(() -> service.pointer(" "))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(enregistres).isEmpty();
    }

    @Test
    void compteNonMembreDeLEcoleRefuse() {
        connecte("e9", RoleUtilisateur.ETUDIANT, ecoleB);   // membre de B, connecté sur A
        assertThat(statut(() -> service.pointer(qr(ecoleA)))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(enregistres).isEmpty();
    }

    @Test
    void qrEntreeDeLEcoleCouranteEtDelaiDeRenouvellement() {
        Map<String, Object> r = service.qrEntree();
        var lu = jetons.lireEntree(String.valueOf(r.get("qrPayload"))).orElseThrow();
        assertThat(lu.ecoleId()).isEqualTo(ecoleA.getId());
        assertThat(lu.valide()).isTrue();
        assertThat((Long) r.get("renouvelerDansSecondes")).isBetween(1L, 60L);
    }

    @Test
    void administrationVoitLaJourneeParPersonne() {
        connecte("e1", RoleUtilisateur.ETUDIANT, ecoleA);
        service.pointer(qr(ecoleA));                        // élève : arrivée…
        horloge.avancer(Duration.ofMinutes(5));
        connecte("p1", RoleUtilisateur.PROFESSEUR, ecoleA);
        service.pointer(qr(ecoleA));                        // professeur : arrivée
        horloge.avancer(Duration.ofMinutes(5));
        connecte("e1", RoleUtilisateur.ETUDIANT, ecoleA);
        service.pointer(qr(ecoleA));                        // …puis départ de l'élève

        String jour = java.time.LocalDate.now(horloge).toString();
        Map<String, Object> tous = service.pointagesDuJour(jour, null, null, null);
        assertThat(tous.get("total")).isEqualTo(2);
        assertThat(tous.get("presents")).isEqualTo(1L);   // le professeur est encore là, l'élève est parti
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> personnes = (List<Map<String, Object>>) tous.get("personnes");
        Map<String, Object> eleve = personnes.stream().filter(m -> "e1".equals(m.get("utilisateurId"))).findFirst().orElseThrow();
        assertThat(eleve.get("arrivee")).isNotNull();
        assertThat(eleve.get("depart")).isNotNull();
        assertThat(eleve.get("present")).isEqualTo(false);
        assertThat((List<?>) eleve.get("passages")).hasSize(2);

        Map<String, Object> profs = service.pointagesDuJour(jour, "PROFESSEUR", null, null);
        assertThat(profs.get("total")).isEqualTo(1);
        assertThat(service.pointagesDuJour(java.time.LocalDate.now(horloge).minusDays(1).toString(), null, null, null).get("total")).isEqualTo(0);
        assertThat(statut(() -> service.pointagesDuJour("16/11/2026", null, null, null))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statut(() -> service.pointagesDuJour(jour, "PARENT", null, null))).isEqualTo(HttpStatus.BAD_REQUEST);

        // plage horaire : l'élève a pointé à t0 et t0+10 min, le professeur à t0+5 min
        java.time.LocalTime t0 = enregistres.get(0).getHorodatage().toLocalTime().withSecond(0).withNano(0);
        String h = t0.plusMinutes(5).toString(), h2 = t0.plusMinutes(6).toString();
        Map<String, Object> plage = service.pointagesDuJour(jour, null, h, h);
        assertThat(plage.get("total")).as("seul le professeur a un passage à " + h).isEqualTo(1);
        assertThat(service.pointagesDuJour(jour, null, h2, null).get("total")).as("après " + h2 + " : le départ de l'élève").isEqualTo(1);
        assertThat(statut(() -> service.pointagesDuJour(jour, null, "10:00", "09:00"))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statut(() -> service.pointagesDuJour(jour, null, "8h30", null))).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
