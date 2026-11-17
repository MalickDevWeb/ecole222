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

    /**
     * I8 (audit DevSecOps) : badge QR d'un élève de l'école B scanné dans l'école A. Refus, et son nom n'est
     * révélé ni dans la réponse, ni dans le journal des passages de A, ni dans le journal temps réel de A.
     */
    @Test
    void badgeDUneAutreEcoleNeRevelePasLeNom() {
        compte("e3", "ETU-003", RoleUtilisateur.ETUDIANT, ecoleB);
        Map<String, Object> r = service.processBadgeScan(badge("ETU-003"));
        assertEquals(false, r.get("accessGranted"));
        assertEquals("Inconnu", r.get("studentName"));
        assertFalse(String.valueOf(r).contains("Fatou"), "aucun élément du compte de l'école B dans la réponse");
        assertTrue(service.getVigilCheckIns().stream().allMatch(l -> "Inconnu".equals(l.get("studentName"))
                && !String.valueOf(l).contains("Fatou")));
        org.mockito.ArgumentCaptor<String> ligne = org.mockito.ArgumentCaptor.forClass(String.class);
        Mockito.verify(admin).addRealtimeLog(ligne.capture());
        assertTrue(ligne.getValue().contains("'Inconnu'"));
        assertFalse(ligne.getValue().contains("Fatou"));
    }

    /**
     * I8 : matricule (ou identifiant) d'un élève de B saisi dans A : même réponse qu'un badge inexistant — pas d'oracle
     * « ce matricule existe dans une autre école ».
     */
    @Test
    void matriculeDUneAutreEcoleRepondCommeUnBadgeInconnu() {
        compte("e3", "ETU-003", RoleUtilisateur.ETUDIANT, ecoleB);
        Map<String, Object> autreEcole = service.processBadgeScan("ETU-003");
        Map<String, Object> idAutreEcole = service.processBadgeScan("e3");
        Map<String, Object> inexistant = service.processBadgeScan("ETU-999");
        for (Map<String, Object> r : java.util.List.of(autreEcole, idAutreEcole)) {
            assertEquals(false, r.get("accessGranted"));
            assertEquals("Inconnu", r.get("studentName"));
            assertEquals(inexistant.get("message"), r.get("message"));
        }
    }

    /** I8 : appartenance suspendue à l'école courante : traitée comme un non-membre (nom non révélé). */
    @Test
    void membreSuspenduNonRevele() {
        compte("e4", "ETU-004", RoleUtilisateur.ETUDIANT, ecoleA);
        var suspendue = OrganizationUserFactory.create("e4", ecoleA.getId(), RoleUtilisateur.ETUDIANT.value());
        suspendue.suspend();
        Mockito.when(appartenances.findByUserIdAndOrganizationId("e4", ecoleA.getId())).thenReturn(Optional.of(suspendue));
        Map<String, Object> r = service.processBadgeScan(badge("ETU-004"));
        assertEquals(false, r.get("accessGranted"));
        assertEquals("Inconnu", r.get("studentName"));
    }

    /** I8 : scan hors ligne d'un élève de B transmis à A : « Inconnu » dans la réponse et le journal, quel que soit le terminal. */
    @Test
    void scanHorsLigneDUneAutreEcoleNeRevelePasLeNom() {
        compte("e3", "ETU-003", RoleUtilisateur.ETUDIANT, ecoleB);
        java.time.Instant maintenant = java.time.Instant.now();
        String qr = jetons.contenu("ETU-003", maintenant.toEpochMilli() / 60_000L);
        Map<String, Object> r = service.enregistrerScanHorsLigne(qr, maintenant, true);
        assertEquals("Inconnu", r.get("studentName"));
        assertEquals(true, r.get("conteste"), "le serveur conteste l'accès accordé par le terminal");
        assertFalse(String.valueOf(r).contains("Fatou"));
        assertFalse(String.valueOf(service.getVigilCheckIns()).contains("Fatou"));
        // matricule saisi hors ligne : idem
        assertEquals("Inconnu", service.enregistrerScanHorsLigne("ETU-003", maintenant, false).get("studentName"));
    }

    /** Contre-épreuve : scan hors ligne d'un élève de A → son nom. */
    @Test
    void scanHorsLigneDUnEleveDeLEcoleDonneSonNom() {
        compte("e1", "ETU-001", RoleUtilisateur.ETUDIANT, ecoleA);
        java.time.Instant maintenant = java.time.Instant.now();
        Map<String, Object> r = service.enregistrerScanHorsLigne(jetons.contenu("ETU-001", maintenant.toEpochMilli() / 60_000L), maintenant, true);
        assertEquals("Fatou Ba", r.get("studentName"));
        assertEquals(false, r.get("conteste"));
    }

    /** I8 : la liste hors ligne ne contient que les membres ACTIFS de l'école courante (appartenance suspendue exclue). */
    @Test
    void listeHorsLigneLimiteeAuxMembresActifsDeLEcole() {
        compte("e1", "ETU-001", RoleUtilisateur.ETUDIANT, ecoleA);
        compte("e4", "ETU-004", RoleUtilisateur.ETUDIANT, ecoleA);
        compte("e3", "ETU-003", RoleUtilisateur.ETUDIANT, ecoleB);
        var suspendue = OrganizationUserFactory.create("e4", ecoleA.getId(), RoleUtilisateur.ETUDIANT.value());
        suspendue.suspend();
        Mockito.when(appartenances.findByOrganizationId(ecoleA.getId())).thenReturn(java.util.List.of(
                OrganizationUserFactory.create("e1", ecoleA.getId(), RoleUtilisateur.ETUDIANT.value()), suspendue));
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> eleves = (java.util.List<Map<String, Object>>) service.listeAccesHorsLigne().get("eleves");
        assertEquals(java.util.List.of("ETU-001"), eleves.stream().map(e -> e.get("matricule")).toList());
        Mockito.verify(appartenances, Mockito.never()).findByOrganizationId(ecoleB.getId());
    }

    private static final String REFUS_NI_PERSONNEL_NI_ELEVE = "Accès refusé : ce compte n'est pas un badge du personnel ni d'un élève.";

    /** B5 (revue DevSecOps phase 2) : un parent ou un visiteur membre de l'école n'entre plus « (personnel) ». */
    @Test
    void parentEtVisiteurMembresRefusesEnLigne() {
        compte("par1", "PAR-001", RoleUtilisateur.PARENT, ecoleA);
        compte("vis1", "VIS-001", RoleUtilisateur.VISITEUR, ecoleA);
        for (String badge : java.util.List.of("par1", "PAR-001", "vis1")) {
            Map<String, Object> r = service.processBadgeScan(badge);
            assertEquals(false, r.get("accessGranted"), badge);
            assertEquals(REFUS_NI_PERSONNEL_NI_ELEVE, r.get("message"), badge);
        }
    }

    /** B5 : le rôle lu est celui de l'APPARTENANCE à l'école ; un rôle hors liste est refusé (refus par défaut). */
    @Test
    void roleDAppartenanceDecideEtRefusParDefaut() {
        compte("p2", "PROF-002", RoleUtilisateur.PROFESSEUR, ecoleA);
        Mockito.when(appartenances.findByUserIdAndOrganizationId("p2", ecoleA.getId()))
                .thenReturn(Optional.of(OrganizationUserFactory.create("p2", ecoleA.getId(), "PARENT")));
        assertEquals(REFUS_NI_PERSONNEL_NI_ELEVE, service.processBadgeScan("p2").get("message"));
        Mockito.when(appartenances.findByUserIdAndOrganizationId("p2", ecoleA.getId()))
                .thenReturn(Optional.of(OrganizationUserFactory.create("p2", ecoleA.getId(), "ROLE_INVENTE")));
        assertEquals(false, service.processBadgeScan("p2").get("accessGranted"));
        // alias historiques des appartenances : TEACHER = PROFESSEUR
        Mockito.when(appartenances.findByUserIdAndOrganizationId("p2", ecoleA.getId()))
                .thenReturn(Optional.of(OrganizationUserFactory.create("p2", ecoleA.getId(), "TEACHER")));
        assertEquals("Accès autorisé (personnel).", service.processBadgeScan("p2").get("message"));
    }

    /** B5 : rôles du personnel de la matrice RBAC, administration et vigile admis. */
    @Test
    void rolesDuPersonnelAutorises() {
        for (String role : java.util.List.of("ADMIN", "DIRECTEUR", "SECRETAIRE", "COMPTABLE", "SURVEILLANT", "RH", "VIGIL")) {
            compte("s-" + role, "S-" + role, RoleUtilisateur.fromString(role), ecoleA);
            Map<String, Object> r = service.processBadgeScan("s-" + role);
            assertEquals(true, r.get("accessGranted"), role);
            assertEquals("Accès autorisé (personnel).", r.get("message"), role);
        }
    }

    /** B5 : parents et visiteurs absents de la liste hors ligne ; professeur présent dans le personnel. */
    @Test
    void listeHorsLigneSansParentNiVisiteur() {
        compte("e1", "ETU-001", RoleUtilisateur.ETUDIANT, ecoleA);
        compte("p1", "PROF-001", RoleUtilisateur.PROFESSEUR, ecoleA);
        compte("par1", "PAR-001", RoleUtilisateur.PARENT, ecoleA);
        compte("vis1", "VIS-001", RoleUtilisateur.VISITEUR, ecoleA);
        Mockito.when(appartenances.findByOrganizationId(ecoleA.getId())).thenReturn(java.util.List.of(
                OrganizationUserFactory.create("e1", ecoleA.getId(), "ETUDIANT"),
                OrganizationUserFactory.create("p1", ecoleA.getId(), "PROFESSEUR"),
                OrganizationUserFactory.create("par1", ecoleA.getId(), "PARENT"),
                OrganizationUserFactory.create("vis1", ecoleA.getId(), "VISITEUR")));
        Map<String, Object> liste = service.listeAccesHorsLigne();
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> personnel = (java.util.List<Map<String, Object>>) liste.get("personnel");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> eleves = (java.util.List<Map<String, Object>>) liste.get("eleves");
        assertEquals(java.util.List.of("p1"), personnel.stream().map(p -> p.get("id")).toList());
        assertEquals(java.util.List.of("ETU-001"), eleves.stream().map(e -> e.get("matricule")).toList());
        assertFalse(String.valueOf(liste).contains("PAR-001"));
        assertFalse(String.valueOf(liste).contains("VIS-001"));
    }

    /** B5 : un scan hors ligne d'un parent accepté par le terminal est contesté par le serveur. */
    @Test
    void scanHorsLigneDUnParentConteste() {
        compte("par1", "PAR-001", RoleUtilisateur.PARENT, ecoleA);
        Map<String, Object> r = service.enregistrerScanHorsLigne("par1", java.time.Instant.now(), true);
        assertEquals(true, r.get("conteste"));
        assertTrue(String.valueOf(r.get("message")).contains(REFUS_NI_PERSONNEL_NI_ELEVE));
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
