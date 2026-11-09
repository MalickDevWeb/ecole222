package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.admin.application.port.usecase.AdminUseCase;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.PointagePersonnelJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Administration d'une école : cloisonnement par école, comptes avec mot de passe, aucune donnée inventée
 * (révision du 2026-11-09).
 */
class AdminControllerTest {

    private AdminUseCase adminUseCase;
    private ActionLogger actionLogger;
    private UserJpaRepository comptes;
    private OrganizationUserJpaRepository appartenances;
    private AdminRepositoryPort adminRepository;
    private ClasseJpaRepository classes;
    private ScheduleSessionJpaRepository seances;
    private InscriptionClasseJpaRepository inscriptions;
    private final PasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private Organization ecole;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminUseCase = Mockito.mock(AdminUseCase.class);
        actionLogger = Mockito.mock(ActionLogger.class);
        comptes = Mockito.mock(UserJpaRepository.class);
        appartenances = Mockito.mock(OrganizationUserJpaRepository.class);
        adminRepository = Mockito.mock(AdminRepositoryPort.class);
        classes = Mockito.mock(ClasseJpaRepository.class);
        seances = Mockito.mock(ScheduleSessionJpaRepository.class);
        inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
        ComptesEcoleService comptesEcole = new ComptesEcoleService(comptes, appartenances, encodeur);

        AdminController controller = new AdminController(adminUseCase, actionLogger, comptesEcole, adminRepository,
                classes, seances, Mockito.mock(PointagePersonnelJpaRepository.class), inscriptions);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        ecole = OrganizationFactory.create("Lycée A", OrganizationType.ECOLE, "a@exemple.sn");
        TenantContext.setCurrentOrganization(ecole);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private OrganizationUserJpaEntity lien(String userId, String role, String statut) {
        OrganizationUserJpaEntity l = new OrganizationUserJpaEntity();
        l.setId("ou-" + userId);
        l.setUserId(userId);
        l.setOrganizationId(ecole.getId());
        l.setRole(role);
        l.setStatus(statut);
        return l;
    }

    private UserJpaEntity compte(String id, String prenom, String nom, String email) {
        UserJpaEntity u = new UserJpaEntity();
        u.setId(id);
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setEmail(email);
        u.setRole("ETUDIANT");
        u.setMatricule("ETU-" + id);
        return u;
    }

    @Test
    void testAssignGate() throws Exception {
        mockMvc.perform(post("/admin/gates/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gate\":\"Gate A\",\"guard\":\"Guard 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.gate").value("Gate A"))
                .andExpect(jsonPath("$.assignedTo").value("Guard 1"));
        Mockito.verify(adminUseCase).assignGate("Gate A", "Guard 1");
    }

    @Test
    void statutFraisModifiableSeulementPourUnEleveDeLEcole() throws Exception {
        Mockito.when(comptes.findById("e1")).thenReturn(Optional.of(compte("e1", "Awa", "Fall", "awa@a.sn")));
        Mockito.when(appartenances.findByUserIdAndOrganizationId("e1", ecole.getId()))
                .thenReturn(Optional.of(lien("e1", "ETUDIANT", "ACTIVE")));

        mockMvc.perform(patch("/admin/students/e1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"statutFrais\":\"En Règle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("En Règle"));
        Mockito.verify(adminUseCase).updateStudentStatus(eq("e1"), eq(StatutFrais.EN_REGLE));

        // élève d'une autre école : invisible
        mockMvc.perform(patch("/admin/students/autre/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"statutFrais\":\"En Règle\"}"))
                .andExpect(status().isNotFound());
        // valeur inconnue : refusée (avant : devenait « En Retard »)
        mockMvc.perform(patch("/admin/students/e1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"statutFrais\":\"n'importe quoi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRealtimeLogs() throws Exception {
        Mockito.when(adminUseCase.getRealtimeLogs()).thenReturn(Arrays.asList("Log 1", "Log 2"));
        mockMvc.perform(get("/admin/realtime-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Log 1"));
    }

    @Test
    void testUpdateCandidaturesSettings() throws Exception {
        Mockito.doNothing().when(adminUseCase).updateCandidaturesSettings(anyBoolean());
        mockMvc.perform(post("/admin/candidatures/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidaturesEnabled").value(true));
        Mockito.verify(adminUseCase).updateCandidaturesSettings(true);
    }

    @Test
    void statistiquesDeLEcoleSansIndicateurInvente() throws Exception {
        Mockito.when(appartenances.findByOrganizationIdAndRole(ecole.getId(), "ETUDIANT")).thenReturn(List.of(
                lien("e1", "ETUDIANT", "ACTIVE"), lien("e2", "ETUDIANT", "ACTIVE"), lien("e3", "ETUDIANT", "SUSPENDED")));
        Mockito.when(appartenances.findByOrganizationIdAndRole(ecole.getId(), "PROFESSEUR")).thenReturn(List.of(
                lien("p1", "PROFESSEUR", "ACTIVE")));
        Mockito.when(seances.count()).thenReturn(7L);
        Mockito.when(classes.count()).thenReturn(4L);

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentsCount").value(2))
                .andExpect(jsonPath("$.professorsCount").value(1))
                .andExpect(jsonPath("$.coursesCount").value(7))
                .andExpect(jsonPath("$.classesCount").value(4))
                .andExpect(jsonPath("$.salleOccupation").doesNotExist())
                .andExpect(jsonPath("$.presentProfessors").doesNotExist());
        // jamais de comptage global, toutes écoles confondues
        Mockito.verify(comptes, Mockito.never()).findByRole(anyString());
    }

    @Test
    void listeDesElevesCloisonneeEtSansValeurInventee() throws Exception {
        Mockito.when(appartenances.findByOrganizationIdAndRole(ecole.getId(), "ETUDIANT"))
                .thenReturn(List.of(lien("e1", "ETUDIANT", "ACTIVE")));
        Mockito.when(comptes.findAllById(List.of("e1"))).thenReturn(List.of(compte("e1", "Awa", "Fall", "awa@a.sn")));
        Mockito.when(adminRepository.getStudentStatus("e1")).thenReturn(null);

        mockMvc.perform(get("/admin/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Awa Fall"))
                .andExpect(jsonPath("$[0].matricule").value("ETU-e1"))
                .andExpect(jsonPath("$[0].statutFrais").doesNotExist())   // non renseigné, pas « En Règle »
                .andExpect(jsonPath("$[0].qrStatus").value("AUTORISÉ"))
                .andExpect(jsonPath("$[0].moyenne").doesNotExist())
                .andExpect(jsonPath("$[0].presence").doesNotExist())
                .andExpect(jsonPath("$[0].filiere").doesNotExist());
        Mockito.verify(comptes, Mockito.never()).findByRole(anyString());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students[0].id").value("e1"))
                .andExpect(jsonPath("$.students[0].average").doesNotExist())
                .andExpect(jsonPath("$.students[0].phoneParent").doesNotExist());
    }

    @Test
    void eleveCreeAvecMotDePasseProvisoireEtRattacheALEcole() throws Exception {
        Mockito.when(comptes.findFirstByMatricule(anyString())).thenReturn(Optional.empty());
        Mockito.when(adminRepository.getStudentStatus(anyString())).thenReturn(null);

        mockMvc.perform(post("/admin/students").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Awa Fall\",\"email\":\"Awa.Fall@exemple.sn\",\"matricule\":\"2026-TEST-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Awa Fall"))
                .andExpect(jsonPath("$.email").value("awa.fall@exemple.sn"))
                .andExpect(jsonPath("$.matricule").value("2026-TEST-001"))
                .andExpect(jsonPath("$.motDePasseProvisoire").isString())
                .andExpect(jsonPath("$.average").doesNotExist());

        ArgumentCaptor<UserJpaEntity> saisi = ArgumentCaptor.forClass(UserJpaEntity.class);
        Mockito.verify(comptes).save(saisi.capture());
        assertNotNull(saisi.getValue().getPassword(), "un compte sans mot de passe ne peut pas se connecter");
        assertTrue(saisi.getValue().getPassword().startsWith("$2"), "mot de passe haché (BCrypt)");
        assertEquals("ETUDIANT", saisi.getValue().getRole());

        ArgumentCaptor<OrganizationUserJpaEntity> lien = ArgumentCaptor.forClass(OrganizationUserJpaEntity.class);
        Mockito.verify(appartenances).save(lien.capture());
        assertEquals(ecole.getId(), lien.getValue().getOrganizationId());
        assertEquals("ACTIVE", lien.getValue().getStatus());
    }

    @Test
    void motDePasseSaisiEstUtiliseEtJamaisRenvoye() throws Exception {
        Mockito.when(comptes.findFirstByMatricule(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(post("/admin/students").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Awa Fall\",\"email\":\"awa@exemple.sn\",\"password\":\"MotDePasse2026\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motDePasseProvisoire").doesNotExist())
                .andExpect(jsonPath("$.matricule").isString());
        ArgumentCaptor<UserJpaEntity> saisi = ArgumentCaptor.forClass(UserJpaEntity.class);
        Mockito.verify(comptes).save(saisi.capture());
        assertTrue(encodeur.matches("MotDePasse2026", saisi.getValue().getPassword()));
    }

    @Test
    void creationRefuseeSansEmailOuSiEmailDejaPris() throws Exception {
        // avant : e-mail inventé « awa.fall@ecole221.sn »
        mockMvc.perform(post("/admin/students").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Awa Fall\"}"))
                .andExpect(status().isBadRequest());
        Mockito.when(comptes.existsByEmail("pris@exemple.sn")).thenReturn(true);
        mockMvc.perform(post("/admin/students").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Awa Fall\",\"email\":\"pris@exemple.sn\"}"))
                .andExpect(status().isConflict());
        Mockito.verify(comptes, Mockito.never()).save(any());
    }

    @Test
    void retraitDUnEleveDUneAutreEcoleImpossible() throws Exception {
        mockMvc.perform(delete("/admin/students/autre"))
                .andExpect(status().isNotFound());
        Mockito.verify(comptes, Mockito.never()).deleteById(anyString());
    }

    @Test
    void retraitDUnEleveDeLEcole() throws Exception {
        OrganizationUserJpaEntity l = lien("e1", "ETUDIANT", "ACTIVE");
        Mockito.when(appartenances.findByUserIdAndOrganizationId("e1", ecole.getId())).thenReturn(Optional.of(l));
        Mockito.when(appartenances.findByUserId("e1")).thenReturn(List.of());
        mockMvc.perform(delete("/admin/students/e1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("e1"));
        Mockito.verify(appartenances).delete(l);
        Mockito.verify(comptes).deleteById("e1");
    }

    @Test
    void professeurCreeAvecMotDePasse() throws Exception {
        Mockito.when(comptes.findFirstByMatricule(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(post("/admin/professors").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Professor\",\"email\":\"test@exemple.sn\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Professor"))
                .andExpect(jsonPath("$.email").value("test@exemple.sn"))
                .andExpect(jsonPath("$.motDePasseProvisoire").isString());
    }

    @Test
    void suspensionDUnEleveReelle() throws Exception {
        OrganizationUserJpaEntity l = lien("e1", "ETUDIANT", "ACTIVE");
        Mockito.when(appartenances.findByUserIdAndOrganizationId("e1", ecole.getId())).thenReturn(Optional.of(l));
        mockMvc.perform(patch("/admin/students/e1/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Suspendu"))
                .andExpect(jsonPath("$.qrStatus").value("SUSPENDU"));
        assertEquals("SUSPENDED", l.getStatus());
    }

    @Test
    void eleveCreeDansSaClasseEtListeAvecElle() throws Exception {
        ClasseJpaEntity l1 = new ClasseJpaEntity();
        l1.setId("cls-1");
        l1.setName("L1 GL");
        Mockito.when(classes.findById("cls-1")).thenReturn(Optional.of(l1));
        Mockito.when(comptes.findFirstByMatricule(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/students").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Awa Fall\",\"email\":\"awa@exemple.sn\",\"classeId\":\"cls-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classeId").value("cls-1"))
                .andExpect(jsonPath("$.classe").value("L1 GL"));
        ArgumentCaptor<InscriptionClasseJpaEntity> ins = ArgumentCaptor.forClass(InscriptionClasseJpaEntity.class);
        Mockito.verify(inscriptions).save(ins.capture());
        assertEquals("cls-1", ins.getValue().getClasseId());

        // relecture : la classe persiste (avant : ignorée, l'élève disparaissait de la classe au rechargement)
        Mockito.when(appartenances.findByOrganizationIdAndRole(ecole.getId(), "ETUDIANT"))
                .thenReturn(List.of(lien("e1", "ETUDIANT", "ACTIVE")));
        Mockito.when(comptes.findAllById(List.of("e1"))).thenReturn(List.of(compte("e1", "Awa", "Fall", "awa@a.sn")));
        Mockito.when(classes.findAll()).thenReturn(List.of(l1));
        Mockito.when(inscriptions.findAll()).thenReturn(List.of(new InscriptionClasseJpaEntity("i1", "e1", "cls-1")));
        mockMvc.perform(get("/admin/students"))
                .andExpect(jsonPath("$[0].classe").value("L1 GL"));
    }

    @Test
    void classeDUneAutreEcoleRefuseeAvantCreation() throws Exception {
        mockMvc.perform(post("/admin/students").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Awa Fall\",\"email\":\"awa@exemple.sn\",\"classeId\":\"cls-autre\"}"))
                .andExpect(status().isBadRequest());
        Mockito.verify(comptes, Mockito.never()).save(any());
    }

    @Test
    void sansEcoleIdentifieeAucuneListe() throws Exception {
        TenantContext.clear();
        mockMvc.perform(get("/admin/students"))
                .andExpect(status().isBadRequest());
    }
}
