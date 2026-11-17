package ecole_221.sn.ecole.modules.security.application.service;

import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.security.domain.port.repository.SecurityRepositoryPort;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.VigilProfileJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/** APP-SEC-18 : le contrôle d'accès repose sur le titulaire RÉEL du badge et son appartenance à l'école. */
class ControleAccesBadgeTest {

    private final AdminRepositoryPort admin = Mockito.mock(AdminRepositoryPort.class);
    private final UserRepositoryPort comptes = Mockito.mock(UserRepositoryPort.class);
    private final OrganizationUserRepositoryPort appartenances = Mockito.mock(OrganizationUserRepositoryPort.class);
    private SecurityService service;
    private Organization ecoleA;
    private Organization ecoleB;

    @BeforeEach
    void setUp() {
        service = new SecurityService(admin, Mockito.mock(SecurityRepositoryPort.class),
                Mockito.mock(VigilProfileJpaRepository.class), comptes, appartenances);
        Mockito.when(comptes.findById(anyString())).thenReturn(Optional.empty());
        Mockito.when(comptes.findByMatricule(anyString())).thenReturn(Optional.empty());
        Mockito.when(appartenances.findByUserIdAndOrganizationId(anyString(), anyString())).thenReturn(Optional.empty());
        ecoleA = OrganizationFactory.create("A", OrganizationType.ECOLE, "a@exemple.sn");
        ecoleB = OrganizationFactory.create("B", OrganizationType.ECOLE, "b@exemple.sn");
        TenantContext.setCurrentOrganization(ecoleA);
        service.setJetonBadge(jetons);
    }

    private final ecole_221.sn.ecole.partage.JetonBadge jetons = ecole_221.sn.ecole.partage.JetonBadge.aleatoire();

    /** QR dynamique signé par le serveur pour la minute courante. */
    private String badge(String matricule) {
        return jetons.contenu(matricule, ecole_221.sn.ecole.partage.JetonBadge.minuteCourante());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private User compte(String id, String matricule, RoleUtilisateur role, Organization ecole) {
        User u = UserFactory.reconstructSimple(id, "Ba", "Fatou", id + "@ex.sn", "a", matricule, role, null);
        Mockito.when(comptes.findById(id)).thenReturn(Optional.of(u));
        Mockito.when(comptes.findByMatricule(matricule)).thenReturn(Optional.of(u));
        Mockito.when(appartenances.findByUserIdAndOrganizationId(id, ecole.getId()))
                .thenReturn(Optional.of(OrganizationUserFactory.create(id, ecole.getId(), role.value())));
        return u;
    }

    @Test
    void badgeInconnuRefuse() {
        Map<String, Object> r = service.processBadgeScan("badge-n-importe-quoi");
        assertEquals(false, r.get("accessGranted"));
        assertEquals("Inconnu", r.get("studentName"));
    }

    @Test
    void eleveAutoriseAvecLeBadgeDynamique() {
        compte("e1", "ETU-001", RoleUtilisateur.ETUDIANT, ecoleA);
        Map<String, Object> r = service.processBadgeScan(badge("ETU-001"));
        assertEquals(true, r.get("accessGranted"));
        assertEquals("Fatou Ba", r.get("studentName"));
    }

    @Test
    void eleveRefuseAvecSonSeulMatricule() {
        // Un matricule tapé ou photocopié n'ouvre plus la porte
        compte("e1", "ETU-001", RoleUtilisateur.ETUDIANT, ecoleA);
        Map<String, Object> r = service.processBadgeScan("ETU-001");
        assertEquals(false, r.get("accessGranted"));
        assertTrue(String.valueOf(r.get("message")).contains("badge QR"));
    }

    @Test
    void badgeFalsifieOuExpireRefuse() {
        compte("e1", "ETU-001", RoleUtilisateur.ETUDIANT, ecoleA);
        long m = ecole_221.sn.ecole.partage.JetonBadge.minuteCourante();
        // signé avec une autre clé (ce que pouvait faire n'importe qui avec l'ancienne clé écrite dans le code)
        String falsifie = ecole_221.sn.ecole.partage.JetonBadge.aleatoire().contenu("ETU-001", m);
        assertEquals(false, service.processBadgeScan(falsifie).get("accessGranted"));
        // vrai badge, mais vieux de 5 minutes (photo d'écran)
        assertEquals(false, service.processBadgeScan(jetons.contenu("ETU-001", m - 5)).get("accessGranted"));
        // jeton tronqué / mal formé
        assertEquals(false, service.processBadgeScan("STU_SECURE:ETU-001:" + m + ":ABC").get("accessGranted"));
        assertEquals(false, service.processBadgeScan("STU_SECURE:ETU-001").get("accessGranted"));
    }

    /**
     * APP-TL-22 : l'accès au campus ne dépend jamais des frais. Un élève au QR valide et membre actif entre, quel que
     * soit son reste dû, et aucune donnée financière n'apparaît dans la décision ni dans le journal du vigile.
     */
    @Test
    void eleveAvecResteDuAutoriseSansDonneeFinanciere() {
        compte("e2", "ETU-002", RoleUtilisateur.ETUDIANT, ecoleA);
        Map<String, Object> r = service.processBadgeScan(badge("ETU-002"));
        assertEquals(true, r.get("accessGranted"));
        assertEquals("Accès autorisé.", r.get("message"));
        assertFalse(r.containsKey("statutFrais"));
        assertFalse(String.valueOf(r).toLowerCase().contains("frais"));
        assertFalse(String.valueOf(r).toLowerCase().contains("régulariser"));
        assertTrue(service.getVigilCheckIns().stream().noneMatch(l -> l.containsKey("statutFrais")));
    }

    /** APP-TL-22 : la liste hors ligne du vigile ne porte, pour un élève, que son matricule et son nom. */
    @Test
    void listeHorsLigneSansDonneeFinanciere() {
        compte("e2", "ETU-002", RoleUtilisateur.ETUDIANT, ecoleA);
        Mockito.when(appartenances.findByOrganizationId(ecoleA.getId()))
                .thenReturn(java.util.List.of(OrganizationUserFactory.create("e2", ecoleA.getId(), RoleUtilisateur.ETUDIANT.value())));
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> eleves = (java.util.List<Map<String, Object>>) service.listeAccesHorsLigne().get("eleves");
        assertEquals(1, eleves.size());
        assertEquals(java.util.Set.of("matricule", "nom"), eleves.get(0).keySet());
        assertEquals("ETU-002", eleves.get(0).get("matricule"));
    }

    @Test
    void badgeDUneAutreEcoleRefuse() {
        compte("e3", "ETU-003", RoleUtilisateur.ETUDIANT, ecoleB);
        Map<String, Object> r = service.processBadgeScan(badge("ETU-003"));
        assertEquals(false, r.get("accessGranted"));
        assertTrue(String.valueOf(r.get("message")).contains("n'appartient pas"));
    }

    @Test
    void personnelAutorise() {
        compte("p1", "PROF-001", RoleUtilisateur.PROFESSEUR, ecoleA);
        assertEquals(true, service.processBadgeScan("p1").get("accessGranted"));
    }

    @Test
    void journalCloisonneParEcole() {
        service.processBadgeScan("inconnu");
        assertEquals(1, service.getVigilCheckIns().size());
        TenantContext.setCurrentOrganization(ecoleB);
        assertTrue(service.getVigilCheckIns().isEmpty(), "l'école B ne voit pas les passages de A");
    }
}
