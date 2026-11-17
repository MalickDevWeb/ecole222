package ecole_221.sn.ecole.modules.notifications.application;

import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.MembershipStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Annonces de l'école (décision du Tech Lead du 15/11), sur la vraie base de test (H2) : le cloisonnement par école
 * (@TenantId) est réellement appliqué par Hibernate.
 */
@SpringBootTest
@ActiveProfiles("test")
class AnnoncesEcoleIntegrationTest {

    @Autowired AnnoncesEcoleService service;
    @Autowired OrganizationRepositoryPort ecoles;
    @Autowired OrganizationUserRepositoryPort appartenances;
    @Autowired NotificationJpaRepository notifications;

    private Organization ecoleA;
    private Organization ecoleB;
    private final Map<String, String> c = new HashMap<>();   // nom → id du compte

    private Organization ecole(String slug) {
        return ecoles.save(OrganizationFactory.create("École " + slug, OrganizationType.ECOLE, OrganizationStatus.ACTIVE, slug,
                "SN", "Dakar", null, null, slug + "@exemple.sn", null, null, null, null, null));
    }

    private void membre(Organization ecole, String nom, String role) {
        String id = nom + "-" + UUID.randomUUID();
        c.put(nom, id);
        appartenances.save(OrganizationUserFactory.create(id, ecole.getId(), role));
    }

    @BeforeEach
    void preparer() {
        TenantContext.clear();
        String s = UUID.randomUUID().toString().substring(0, 8);
        ecoleA = ecole("annonces-a-" + s);
        ecoleB = ecole("annonces-b-" + s);
        membre(ecoleA, "eleve1", "ETUDIANT");
        membre(ecoleA, "eleve2", "ETUDIANT");
        membre(ecoleA, "parent1", "PARENT");
        membre(ecoleA, "prof1", "PROFESSEUR");
        membre(ecoleA, "secretaire", "SECRETAIRE");
        membre(ecoleA, "admin", "ADMIN");
        membre(ecoleA, "vigile", "VIGIL");
        // exclus : appartenance suspendue, appartenance expirée, compte de la plateforme
        String suspendu = "suspendu-" + UUID.randomUUID();
        c.put("suspendu", suspendu);
        appartenances.save(OrganizationUserFactory.create(suspendu, ecoleA.getId(), "ETUDIANT", null, null, null,
                MembershipStatus.SUSPENDED, null));
        String expire = "expire-" + UUID.randomUUID();
        c.put("expire", expire);
        appartenances.save(OrganizationUserFactory.create(expire, ecoleA.getId(), "ETUDIANT", null, null, null,
                MembershipStatus.ACTIVE, LocalDateTime.now().minusDays(1)));
        membre(ecoleA, "superadmin", "SUPER_ADMIN");
        membre(ecoleB, "eleveB", "ETUDIANT");
    }

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
    }

    private void dans(Organization ecole) {
        TenantContext.setCurrentOrganization(ecole);
        TenantContext.setPlatformMode(false);
    }

    private static Map<String, Object> annonce(String destinataires) {
        Map<String, Object> m = new HashMap<>();
        m.put("titre", "Réunion des parents");
        m.put("message", "La réunion aura lieu samedi à 10 h dans la grande salle.");
        m.put("destinataires", destinataires);
        m.put("nature", "ANNONCE");
        return m;
    }

    private static void refus(Runnable r, HttpStatus statut, String extrait) {
        assertThatThrownBy(r::run).isInstanceOf(ResponseStatusException.class).satisfies(e -> {
            assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(statut);
            assertThat(((ResponseStatusException) e).getReason()).contains(extrait);
        });
    }

    @Test
    void apercuNombresReelsParRoleMembresInactifsExclus() {
        dans(ecoleA);
        assertThat(service.apercu("ELEVES").get("nombre")).isEqualTo(2);
        assertThat(service.apercu("PARENTS").get("nombre")).isEqualTo(1);
        assertThat(service.apercu("PROFESSEURS").get("nombre")).isEqualTo(1);
        assertThat(service.apercu("PERSONNEL").get("nombre")).isEqualTo(3);
        assertThat(service.apercu("toute_ecole")).containsEntry("destinataires", "TOUTE_ECOLE").containsEntry("nombre", 7);
        refus(() -> service.apercu("TOUT_LE_MONDE"), HttpStatus.BAD_REQUEST, "inconnus");
        dans(ecoleB);
        assertThat(service.apercu("TOUTE_ECOLE").get("nombre")).isEqualTo(1);
    }

    @Test
    void uneNotificationParCompteViseEtHistorique() {
        dans(ecoleA);
        Map<String, Object> r = service.envoyer(annonce("ELEVES"), c.get("admin"), "Awa Ndiaye");
        assertThat(r).containsKeys("id", "destinatairesTouches", "envoyeeLe");
        assertThat(r.get("destinatairesTouches")).isEqualTo(2);
        for (String eleve : List.of("eleve1", "eleve2")) {
            List<Map<String, Object>> mes = service.mes(c.get(eleve));
            assertThat(mes).as(eleve).hasSize(1);
            assertThat(mes.get(0)).containsEntry("titre", "Réunion des parents").containsEntry("lue", false)
                    .containsEntry("nature", "ANNONCE");
            assertThat(service.nonLues(c.get(eleve))).containsEntry("nombre", 1L);
        }
        for (String exclu : List.of("parent1", "prof1", "admin", "suspendu", "expire", "superadmin")) {
            assertThat(service.mes(c.get(exclu))).as(exclu).isEmpty();
        }
        List<Map<String, Object>> h = service.historique();
        assertThat(h).hasSize(1);
        assertThat(h.get(0)).containsEntry("destinataires", "ELEVES").containsEntry("destinatairesTouches", 2)
                .containsEntry("lues", 0L).containsEntry("auteur", "Awa Ndiaye").containsEntry("nature", "ANNONCE");

        String idNotif = (String) service.mes(c.get("eleve1")).get(0).get("id");
        service.marquerLue(idNotif, c.get("eleve1"));
        assertThat(service.mes(c.get("eleve1")).get(0)).containsEntry("lue", true);
        assertThat(service.nonLues(c.get("eleve1"))).containsEntry("nombre", 0L);
        assertThat(service.historique().get(0)).containsEntry("lues", 1L);
    }

    @Test
    void messageLongEtUrgence() {
        dans(ecoleA);
        Map<String, Object> a = annonce("PERSONNEL");
        a.put("message", "x".repeat(5_000));
        a.put("nature", "urgence");
        assertThat(service.envoyer(a, c.get("admin"), null).get("destinatairesTouches")).isEqualTo(3);
        Map<String, Object> recue = service.mes(c.get("vigile")).get(0);
        assertThat(recue).containsEntry("nature", "URGENCE");
        assertThat((String) recue.get("message")).hasSize(5_000);
    }

    @Test
    void leClientNImposeJamaisLaListe() {
        dans(ecoleA);
        Map<String, Object> a = annonce("PARENTS");
        a.put("recipientIds", List.of(c.get("eleve1"), c.get("eleveB")));
        refus(() -> service.envoyer(a, c.get("admin"), null), HttpStatus.BAD_REQUEST, "calculée par le serveur");
        assertThat(service.mes(c.get("eleve1"))).isEmpty();
        a.remove("recipientIds");
        a.put("destinataireIds", List.of(c.get("eleve1")));
        refus(() -> service.envoyer(a, c.get("admin"), null), HttpStatus.BAD_REQUEST, "calculée par le serveur");
        a.remove("destinataireIds");
        assertThat(service.envoyer(a, c.get("admin"), null).get("destinatairesTouches")).isEqualTo(1);
        assertThat(service.mes(c.get("eleve1"))).isEmpty();
        assertThat(service.mes(c.get("parent1"))).hasSize(1);
    }

    @Test
    void champsInvalides400EtAucunDestinataire409() {
        dans(ecoleA);
        Map<String, Object> a = annonce("ELEVES");
        a.put("titre", " ");
        refus(() -> service.envoyer(a, null, null), HttpStatus.BAD_REQUEST, "titre");
        a.put("titre", "t".repeat(201));
        refus(() -> service.envoyer(a, null, null), HttpStatus.BAD_REQUEST, "200");
        a.put("titre", "Titre");
        a.put("message", "m".repeat(5_001));
        refus(() -> service.envoyer(a, null, null), HttpStatus.BAD_REQUEST, "message");
        a.put("message", "Message");
        a.put("nature", "PUB");
        refus(() -> service.envoyer(a, null, null), HttpStatus.BAD_REQUEST, "Nature");
        a.put("nature", "ANNONCE");
        a.put("destinataires", null);
        refus(() -> service.envoyer(a, null, null), HttpStatus.BAD_REQUEST, "destinataires");
        refus(() -> service.envoyer(Map.of(), null, null), HttpStatus.BAD_REQUEST, "Aucune donnée");

        dans(ecoleB);   // aucun parent dans l'école B
        refus(() -> service.envoyer(annonce("PARENTS"), null, null), HttpStatus.CONFLICT, AnnoncesEcoleService.AUCUN_DESTINATAIRE);
        assertThat(service.historique()).isEmpty();
    }

    @Test
    void marquageLimiteASoiEtAutreEcoleInvisible() {
        dans(ecoleA);
        service.envoyer(annonce("ELEVES"), c.get("admin"), null);
        String idEleve1 = (String) service.mes(c.get("eleve1")).get(0).get("id");
        refus(() -> service.marquerLue(idEleve1, c.get("eleve2")), HttpStatus.NOT_FOUND, "introuvable");
        refus(() -> service.marquerLue("n-existe-pas", c.get("eleve1")), HttpStatus.NOT_FOUND, "introuvable");

        dans(ecoleB);
        service.envoyer(annonce("TOUTE_ECOLE"), c.get("eleveB"), null);
        assertThat(service.historique()).hasSize(1);
        refus(() -> service.marquerLue(idEleve1, c.get("eleve1")), HttpStatus.NOT_FOUND, "introuvable");
        refus(() -> service.marquerLue(idEleve1, c.get("eleveB")), HttpStatus.NOT_FOUND, "introuvable");
        assertThat(service.mes(c.get("eleve1"))).as("vue depuis l'école B : rien de l'école A").isEmpty();

        dans(ecoleA);
        assertThat(service.historique()).as("l'historique de l'école A ne montre pas l'annonce de B").hasSize(1);
        assertThat(service.mes(c.get("eleve1"))).hasSize(1);
        assertThat(service.mes(c.get("eleve1")).get(0)).containsEntry("lue", false);
        assertThat(service.mes(c.get("eleveB"))).isEmpty();
        assertThat(notifications.countUnreadByRecipientId(c.get("eleveB"))).isZero();
    }
}
