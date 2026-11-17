package ecole_221.sn.ecole.modules.corbeille;

import ecole_221.sn.ecole.modules.admin.domain.model.RoomFactory;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.JpaPlanningRepository;
import ecole_221.sn.ecole.modules.admin.rbac.CataloguePermissions;
import ecole_221.sn.ecole.modules.admin.rbac.PermissionsEcoleService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.collaboration.application.service.GroupChatService;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.corbeille.application.CorbeilleService;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.planning.infrastructure.web.PlanningController;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.professor.application.service.QuizTentativeService;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.referential.application.dto.ReferenceCaller;
import ecole_221.sn.ecole.modules.referential.application.service.ReferenceCommandService;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceList;
import ecole_221.sn.ecole.modules.student.infrastructure.web.StudentController;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Corbeille (V21) pour les types qui n'avaient pas de test, et corrections du 16/11, sur la base de test H2 :
 * messages de groupe, séances en direct (auteur, V22), valeurs de référentiel propres à l'école, restauration par un
 * membre du personnel avec ou sans le droit du module, réponses « corbeille » des suppressions du planning, et
 * appartenance d'un cours à la classe de l'élève (détails de cours).
 */
@SpringBootTest
@ActiveProfiles("test")
class CorbeilleTypesIntegrationTest {

    @Autowired CorbeilleService corbeille;
    @Autowired OrganizationRepositoryPort ecoles;
    @Autowired GroupChatService discussions;
    @Autowired GroupChatJpaRepository groupes;
    @Autowired MessageJpaRepositoryOptimized messages;
    @Autowired MeetJpaRepository seances;
    @Autowired StudentController espaceEleve;
    @Autowired ReferenceCommandService referentiels;
    @Autowired PermissionsEcoleService droits;
    @Autowired PlanningController planningWeb;
    @Autowired PlanningRepositoryAdapter planning;
    @Autowired JpaPlanningRepository planningAdmin;
    @Autowired RoomJpaRepository salles;
    @Autowired ClasseJpaRepository classes;
    @Autowired CourseJpaRepository cours;
    @Autowired InscriptionClasseJpaRepository inscriptions;
    @Autowired QuizTentativeService quizEleve;
    @Autowired JdbcTemplate jdbc;

    private Organization ecoleA, ecoleB;
    private User profA, profA2, adminA, secretaireA;
    private String coursId, classeId, salleId, eleve;

    private Organization ecole(String slug) {
        return ecoles.save(OrganizationFactory.create("École " + slug, OrganizationType.ECOLE, OrganizationStatus.ACTIVE, slug,
                "SN", "Dakar", null, null, slug + "@exemple.sn", null, null, null, null, null));
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private void dans(Organization ecole) {
        TenantContext.setCurrentOrganization(ecole);
        TenantContext.setPlatformMode(false);
    }

    private static void connecte(User u, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static void refus(Runnable r, HttpStatus statut, String extrait) {
        assertThatThrownBy(r::run).isInstanceOf(ResponseStatusException.class).satisfies(e -> {
            assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(statut);
            if (extrait != null) assertThat(((ResponseStatusException) e).getReason()).contains(extrait);
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> elements() {
        return (List<Map<String, Object>>) corbeille.lister(null).get("elements");
    }

    @BeforeEach
    void preparer() {
        SecurityContextHolder.clearContext();
        String s = UUID.randomUUID().toString().substring(0, 8);
        ecoleA = ecole("corbeille-types-a-" + s);
        ecoleB = ecole("corbeille-types-b-" + s);
        profA = UserFactory.create("Sarr", "Moussa", "prof-" + s + "@ex.sn", "x", RoleUtilisateur.PROFESSEUR);
        profA2 = UserFactory.create("Ba", "Aïda", "prof2-" + s + "@ex.sn", "x", RoleUtilisateur.PROFESSEUR);
        adminA = UserFactory.create("Ndiaye", "Awa", "admin-" + s + "@ex.sn", "x", RoleUtilisateur.ADMIN);
        secretaireA = UserFactory.create("Diop", "Fatou", "secr-" + s + "@ex.sn", "x", RoleUtilisateur.SECRETAIRE);
        eleve = "eleve-" + s;

        dans(ecoleA);
        ClasseJpaEntity cl = new ClasseJpaEntity();
        classeId = id();
        cl.setId(classeId);
        cl.setName("L1 Info");
        classes.save(cl);
        inscriptions.save(new InscriptionClasseJpaEntity(id(), eleve, classeId));
        coursId = id();
        cours.save(new CourseJpaEntity(coursId, "Algorithmique", classeId, 40, profA.getId()));
        RoomJpaEntity salle = new RoomJpaEntity();
        salleId = id();
        salle.setId(salleId);
        salle.setName("Salle 12");
        salles.save(salle);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // ================================================================ messages de groupe

    @Test
    void messageDeGroupeMisALaCorbeillePuisRestaureParSonAuteur() {
        dans(ecoleA);
        String groupe = id();
        GroupChatJpaEntity g = new GroupChatJpaEntity(groupe, "Projet tutoré", null, null, profA.getId(), "GROUP", LocalDateTime.now());
        groupes.save(g);
        String messageId = id();
        messages.save(new MessageJpaEntityOptimized(messageId, groupe, profA.getId(), "Rendez-vous à 10 h", "TEXT", "SENT", LocalDateTime.now()));

        connecte(profA2, "PROFESSEUR");
        refus(() -> discussions.supprimerMessage(messageId, profA2.getId()), HttpStatus.FORBIDDEN, "auteur du message");

        connecte(profA, "PROFESSEUR");
        Map<String, Object> r = discussions.supprimerMessage(messageId, profA.getId());
        assertThat(r).containsEntry("corbeille", true).containsEntry("type", "message").containsEntry("id", messageId)
                .containsEntry("message", "Message dans la discussion “Projet tutoré” placé dans la corbeille.");
        assertThat(messages.findById(messageId)).isEmpty();
        // jamais le contenu du message dans la corbeille
        assertThat(elements()).singleElement().satisfies(e -> assertThat(String.valueOf(e.get("libelle"))).doesNotContain("Rendez-vous"));

        Map<String, Object> restaure = corbeille.restaurer("message", messageId);
        assertThat(restaure).containsEntry("message", "Message dans la discussion “Projet tutoré” a été restauré.");
        assertThat(messages.findById(messageId)).isPresent();
    }

    // ================================================================ séances en direct (V22)

    private String lancer(User prof, String titre, String coursNom) {
        connecte(prof, "PROFESSEUR");
        ResponseEntity<Map<String, Object>> r = espaceEleve.startLiveSession(
                Map.of("title", titre, "courseName", coursNom, "meetUrl", "https://meet.google.com/abc-defg-hij"));
        return (String) r.getBody().get("id");
    }

    @Test
    void seanceEnDirectArreteeSeulementParSonAuteurLEnseignantDuCoursOuLAdministration() {
        dans(ecoleA);
        String id = lancer(profA, "Révision du chapitre 3", "Algorithmique");
        MeetJpaEntity enregistree = seances.findById(id).orElseThrow();
        assertThat(enregistree.getAuteurId()).isEqualTo(profA.getId());
        assertThat(enregistree.getCourseId()).isEqualTo(coursId);

        connecte(profA2, "PROFESSEUR");
        refus(() -> espaceEleve.stopLiveSession(id), HttpStatus.FORBIDDEN, "Seul l'enseignant qui a lancé cette séance");
        assertThat(seances.findById(id)).isPresent();

        connecte(profA, "PROFESSEUR");
        Map<String, Object> r = espaceEleve.stopLiveSession(id).getBody();
        assertThat(r).containsEntry("corbeille", true)
                .containsEntry("message", "Séance en direct “Révision du chapitre 3” placée dans la corbeille.");
        assertThat(seances.findById(id)).isEmpty();
        assertThat(corbeille.restaurer("seance-en-direct", id))
                .containsEntry("message", "Séance en direct “Révision du chapitre 3” a été restaurée.");
        assertThat(seances.findById(id)).isPresent();

        // l'administration de l'école garde le droit ; une autre école ne la voit pas
        dans(ecoleB);
        connecte(adminA, "ADMIN");
        refus(() -> espaceEleve.stopLiveSession(id), HttpStatus.NOT_FOUND, null);
        dans(ecoleA);
        assertThat(espaceEleve.stopLiveSession(id).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void seanceLanceeParUnCollegueArreteeParLEnseignantDuCours() {
        dans(ecoleA);
        MeetJpaEntity m = new MeetJpaEntity(id(), "Cours partagé", "https://meet.google.com/abc-defg-hij", "08:00", true);
        m.setAuteurId(profA2.getId());
        m.setCourseId(coursId);
        seances.save(m);
        connecte(profA, "PROFESSEUR");   // enseignant du cours
        assertThat(espaceEleve.stopLiveSession(m.getId()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void seanceAnterieureSansAuteurArreteeParLEnseignantDuCoursDeMemeIntitule() {
        dans(ecoleA);
        MeetJpaEntity ancienne = new MeetJpaEntity(id(), "Séance du lundi", "https://meet.google.com/abc-defg-hij", "08:00", true);
        ancienne.setSubjectName("algorithmique ");
        seances.save(ancienne);

        connecte(profA2, "PROFESSEUR");
        refus(() -> espaceEleve.stopLiveSession(ancienne.getId()), HttpStatus.FORBIDDEN, null);
        connecte(profA, "PROFESSEUR");
        assertThat(espaceEleve.stopLiveSession(ancienne.getId()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ================================================================ valeurs de référentiel

    @Test
    void valeurDeReferentielPropreAL_ecoleMiseALaCorbeillePuisRestauree() {
        dans(ecoleA);
        connecte(adminA, "ADMIN");
        ReferenceCaller admin = new ReferenceCaller(ecoleA.getId(), true, false, false);
        referentiels.create(ReferenceList.EXPENSE_CATEGORY, admin, "FOURNITURES", "Fournitures de bureau", null, null);

        Optional<Map<String, Object>> r = referentiels.delete(ReferenceList.EXPENSE_CATEGORY, "FOURNITURES", admin);
        assertThat(r).isPresent();
        assertThat(r.get()).containsEntry("corbeille", true).containsEntry("type", "referentiel");
        assertThat((String) r.get().get("message")).startsWith("Valeur de référentiel “Fournitures de bureau”")
                .endsWith(" placée dans la corbeille.");
        refus(() -> referentiels.create(ReferenceList.EXPENSE_CATEGORY, admin, "FOURNITURES", "Fournitures", null, null),
                HttpStatus.CONFLICT, "restaurez-la");

        String id = (String) r.get().get("id");
        assertThat((String) corbeille.restaurer("referentiel", id).get("message")).endsWith(" a été restaurée.");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reference_values WHERE id = ? AND supprime_le IS NULL", Integer.class, id)).isOne();
    }

    // ================================================================ personnel de l'école

    @Test
    void restaurationParUnMembreDuPersonnelSelonLeDroitDuModule() {
        dans(ecoleA);
        connecte(secretaireA, "SECRETAIRE");
        droits.enregistrer(ecoleA.getId(), "SECRETAIRE", List.of(CataloguePermissions.lecture("planning")), "test");
        planning.deleteRoom(salleId);
        assertThat(elements()).singleElement().satisfies(e -> assertThat(e).containsEntry("peutRestaurer", false));
        refus(() -> corbeille.restaurer("salle", salleId), HttpStatus.FORBIDDEN, "n'a pas la permission « planning.ecrire »");

        droits.enregistrer(ecoleA.getId(), "SECRETAIRE", List.of(CataloguePermissions.ecriture("planning")), "test");
        assertThat(elements()).singleElement().satisfies(e -> assertThat(e).containsEntry("peutRestaurer", true));
        assertThat(corbeille.restaurer("salle", salleId)).containsEntry("message", "Salle “Salle 12” a été restaurée.");
        assertThat(salles.findById(salleId)).isPresent();

        // un autre membre du personnel, même avec le droit, ne restaure pas la suppression d'un collègue
        planning.deleteRoom(salleId);
        User autre = UserFactory.create("Kane", "Ibou", "autre-" + UUID.randomUUID() + "@ex.sn", "x", RoleUtilisateur.SECRETAIRE);
        connecte(autre, "SECRETAIRE");
        refus(() -> corbeille.restaurer("salle", salleId), HttpStatus.FORBIDDEN, "restaurer cette salle");
    }

    // ================================================================ réponses du planning

    @Test
    void suppressionsDuPlanningAnnonceesAvecLeMessageDeLaCorbeille() {
        dans(ecoleA);
        connecte(adminA, "ADMIN");
        ResponseEntity<Map<String, Object>> r = planningWeb.deleteRoom(salleId);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).containsEntry("corbeille", true).containsEntry("id", salleId)
                .containsEntry("message", "Salle “Salle 12” placée dans la corbeille.");

        Map<String, Object> c = planningWeb.deleteClasse(classeId).getBody();
        assertThat(c).containsEntry("message", "Classe “L1 Info” placée dans la corbeille.");
    }

    @Test
    void listeDuPlanningRenvoieLesLignesMisesALaCorbeille() {
        dans(ecoleA);
        connecte(adminA, "ADMIN");
        String autre = id();
        planningAdmin.saveRooms(List.of(RoomFactory.reconstruct(salleId, "Salle 12", 30, List.of()),
                RoomFactory.reconstruct(autre, "Salle 14", 20, List.of())));

        List<Map<String, Object>> retirees = planningAdmin.saveRooms(List.of(RoomFactory.reconstruct(autre, "Salle 14", 20, List.of())));
        assertThat(retirees).singleElement().satisfies(e -> assertThat(e)
                .containsEntry("type", "salle").containsEntry("id", salleId).containsEntry("libelle", "Salle “Salle 12”"));
        assertThat(planningAdmin.saveRooms(List.of(RoomFactory.reconstruct(autre, "Salle 14", 20, List.of())))).isEmpty();
    }

    // ================================================================ détails de cours (point 1)

    @Test
    void coursDeLaClasseDeLEleveSeulement() {
        dans(ecoleA);
        String autreCours = id();
        cours.save(new CourseJpaEntity(autreCours, "Réseaux", id(), 20, profA.getId()));
        assertThat(quizEleve.estCoursDeSaClasse(coursId, eleve)).isTrue();
        assertThat(quizEleve.estCoursDeSaClasse(autreCours, eleve)).isFalse();
        assertThat(quizEleve.estCoursDeSaClasse("inconnu", eleve)).isFalse();
        assertThat(quizEleve.estCoursDeSaClasse(coursId, "eleve-sans-classe")).isFalse();
        dans(ecoleB);
        assertThat(quizEleve.estCoursDeSaClasse(coursId, eleve)).isFalse();
    }
}
