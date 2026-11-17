package ecole_221.sn.ecole.modules.corbeille;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.JpaPlanningRepository;
import ecole_221.sn.ecole.modules.admin.domain.model.RoomFactory;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.corbeille.application.CorbeilleService;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.professor.application.service.GestionContenusService;
import ecole_221.sn.ecole.modules.professor.application.service.ProfessorService;
import ecole_221.sn.ecole.modules.professor.application.service.RendusDevoirsService;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.student.application.service.EmploiDuTempsEleveService;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Corbeille (décision du Tech Lead du 15/11, V21), sur la base de test H2 : @TenantId et @SQLRestriction sont
 * réellement appliqués par Hibernate. Invisibilité pour l'élève, le parent, le professeur et l'administration ;
 * restauration par l'auteur et par l'administration ; refus pour un autre professeur et une autre école ; conflits ;
 * purge manuelle et à 30 jours ; journal d'audit lisible.
 */
@SpringBootTest
@ActiveProfiles("test")
class CorbeilleIntegrationTest {

    @Autowired CorbeilleService corbeille;
    @Autowired GestionContenusService contenus;
    @Autowired ProfessorService professeur;
    @Autowired RendusDevoirsService rendusDevoirs;
    @Autowired EmploiDuTempsEleveService emploiDuTemps;
    @Autowired PlanningRepositoryAdapter planning;
    @Autowired JpaPlanningRepository planningAdmin;
    @Autowired OrganizationRepositoryPort ecoles;
    @Autowired CourseJpaRepository cours;
    @Autowired HomeworkJpaRepository devoirs;
    @Autowired RenduDevoirJpaRepository rendus;
    @Autowired QuizJpaRepository quiz;
    @Autowired TentativeQuizJpaRepository tentatives;
    @Autowired ModuleJpaRepository modules;
    @Autowired LessonJpaRepository supports;
    @Autowired ClasseJpaRepository classes;
    @Autowired FiliereJpaRepository filieres;
    @Autowired RoomJpaRepository salles;
    @Autowired ScheduleSessionJpaRepository seances;
    @Autowired InscriptionClasseJpaRepository inscriptions;
    @Autowired JdbcTemplate jdbc;

    private final Consumer<String> coursAutorise = c -> { };
    private Organization ecoleA;
    private Organization ecoleB;
    private User profA, profA2, adminA, adminB;
    private String eleve, coursId, classeId, filiereId, devoirId, quizId, salleId, moduleId, supportId;

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

    private List<String> journal(String ecole) {
        return jdbc.queryForList("SELECT action_description FROM audit_logs WHERE organization_id = ? AND entity_type LIKE 'corbeille/%' "
                + "ORDER BY action_timestamp", String.class, ecole);
    }

    @BeforeEach
    void preparer() {
        SecurityContextHolder.clearContext();
        String s = UUID.randomUUID().toString().substring(0, 8);
        ecoleA = ecole("corbeille-a-" + s);
        ecoleB = ecole("corbeille-b-" + s);
        profA = UserFactory.create("Sarr", "Moussa", "prof-" + s + "@ex.sn", "x", RoleUtilisateur.PROFESSEUR);
        profA2 = UserFactory.create("Ba", "Aïda", "prof2-" + s + "@ex.sn", "x", RoleUtilisateur.PROFESSEUR);
        adminA = UserFactory.create("Ndiaye", "Awa", "admin-" + s + "@ex.sn", "x", RoleUtilisateur.ADMIN);
        adminB = UserFactory.create("Faye", "Ali", "adminb-" + s + "@ex.sn", "x", RoleUtilisateur.ADMIN);
        eleve = "eleve-" + s;

        dans(ecoleA);
        FiliereJpaEntity f = new FiliereJpaEntity();
        filiereId = id();
        f.setId(filiereId);
        f.setName("Informatique");
        filieres.save(f);
        ClasseJpaEntity cl = new ClasseJpaEntity();
        classeId = id();
        cl.setId(classeId);
        cl.setName("L1 Info");
        cl.setFiliereId(filiereId);
        classes.save(cl);
        inscriptions.save(new InscriptionClasseJpaEntity(id(), eleve, classeId));
        coursId = id();
        cours.save(new CourseJpaEntity(coursId, "Algorithmique", classeId, 40, profA.getId()));

        HomeworkJpaEntity h = new HomeworkJpaEntity();
        devoirId = id();
        h.setId(devoirId);
        h.setCourseId(coursId);
        h.setTitle("Exercices chapitre 3");
        h.setPriority("MEDIUM");
        h.setDeadline(LocalDateTime.now().plusDays(7).toLocalDate().toString());
        devoirs.save(h);
        RenduDevoirJpaEntity r = new RenduDevoirJpaEntity(id(), devoirId, coursId, eleve);
        r.setFichierNom("copie.pdf");
        r.setDeposeLe(LocalDateTime.now());
        rendus.save(r);

        QuizJpaEntity q = new QuizJpaEntity();
        quizId = id();
        q.setId(quizId);
        q.setCourseId(coursId);
        q.setTitle("Quiz boucles");
        quiz.save(q);
        TentativeQuizJpaEntity t = new TentativeQuizJpaEntity();
        t.setId(id());
        t.setQuizId(quizId);
        t.setCourseId(coursId);
        t.setStudentId(eleve);
        t.setReponsesJson("[]");
        t.setScore(3);
        t.setTotal(5);
        t.setDateTentative(LocalDateTime.now());
        tentatives.save(t);

        ModuleJpaEntity m = new ModuleJpaEntity();
        moduleId = id();
        m.setId(moduleId);
        m.setCourseId(coursId);
        m.setName("Chapitre 3");
        modules.save(m);
        LessonJpaEntity l = new LessonJpaEntity();
        supportId = id();
        l.setId(supportId);
        l.setCourseId(coursId);
        l.setModuleId(moduleId);
        l.setTitle("Diaporama du chapitre 3");
        supports.save(l);

        RoomJpaEntity salle = new RoomJpaEntity();
        salleId = id();
        salle.setId(salleId);
        salle.setName("Salle 12");
        salles.save(salle);

        ScheduleSessionJpaEntity se = new ScheduleSessionJpaEntity();
        se.setId(id());
        se.setCourseTitle("Algorithmique");
        se.setDay("Lundi");
        se.setTime("08:00 - 10:00");
        se.setClasse(classeId);
        se.setStatus("planifie");
        seances.save(se);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void devoirSupprimeInvisiblePourEleveProfesseurEtAdministrationPuisRestaureParSonAuteur() {
        dans(ecoleA);
        assertThat(rendusDevoirs.devoirsEleve(eleve)).extracting(d -> d.get("id")).contains(devoirId);

        connecte(profA, "PROFESSEUR");
        Map<String, Object> r = contenus.supprimerDevoir(devoirId, coursAutorise);
        assertThat(r).containsEntry("corbeille", true);
        assertThat((String) r.get("message")).contains("Devoir “Exercices chapitre 3” — Algorithmique (L1 Info)");

        // élève : ni le devoir ni son rendu
        assertThat(rendusDevoirs.devoirsEleve(eleve)).isEmpty();
        assertThat(rendus.findByStudentId(eleve)).isEmpty();
        // professeur : liste de son cours
        assertThat(professeur.getHomeworks(coursId, 0, 20).getContent()).isEmpty();
        // administration et toute lecture JPA
        assertThat(devoirs.findById(devoirId)).isEmpty();
        assertThat(devoirs.findAll()).extracting(HomeworkJpaEntity::getId).doesNotContain(devoirId);
        assertThat(devoirs.findByCourseId(coursId)).isEmpty();

        // la corbeille de son auteur
        List<Map<String, Object>> mes = elements();
        assertThat(mes).hasSize(1);
        assertThat(mes.get(0)).containsEntry("type", "devoir").containsEntry("id", devoirId)
                .containsEntry("libelle", "Devoir “Exercices chapitre 3” — Algorithmique (L1 Info)")
                .containsEntry("supprimePar", profA.getId()).containsEntry("peutRestaurer", true)
                .containsEntry("peutPurger", false).containsEntry("conflit", null);
        assertThat(((LocalDateTime) mes.get(0).get("purgeLe"))).isAfter(LocalDateTime.now().plusDays(29));

        corbeille.restaurer("devoir", devoirId);
        assertThat(rendusDevoirs.devoirsEleve(eleve)).extracting(d -> d.get("id")).containsExactly(devoirId);
        assertThat(rendus.findByStudentId(eleve)).hasSize(1);   // le rendu de l'élève revient intact
        assertThat(elements()).isEmpty();

        assertThat(journal(ecoleA.getId())).anySatisfy(d -> assertThat(d)
                        .startsWith("Mise à la corbeille : Devoir “Exercices chapitre 3” — Algorithmique (L1 Info)"))
                .anySatisfy(d -> assertThat(d).isEqualTo("Restauration depuis la corbeille : Devoir “Exercices chapitre 3” — Algorithmique (L1 Info)"));
    }

    @Test
    void classeSupprimeeInvisiblePourParentEtAdministration() {
        dans(ecoleA);
        assertThat(emploiDuTemps.seances(eleve)).extracting(EmploiDuTempsEleveService.Seance::classe).containsExactly("L1 Info");

        connecte(adminA, "ADMIN");
        planning.deleteClasse(classeId);

        // parent (même lecture que GET /parent/enfants/{id}/planning) : le nom de la classe supprimée n'est plus lu
        assertThat(emploiDuTemps.seances(eleve)).extracting(EmploiDuTempsEleveService.Seance::classe).containsExactly("");
        assertThat(classes.findById(classeId)).isEmpty();
        assertThat(planningAdmin.getAllClasses()).extracting(c -> c.getId()).doesNotContain(classeId);
        assertThat(elements()).extracting(e -> e.get("libelle")).containsExactly("Classe “L1 Info” — Informatique");
    }

    @Test
    void autreProfesseurRefuseAdministrationAutoriseeAutreEcoleIntrouvable() {
        dans(ecoleA);
        connecte(profA, "PROFESSEUR");
        contenus.supprimerDevoir(devoirId, coursAutorise);

        connecte(profA2, "PROFESSEUR");
        assertThat(elements()).isEmpty();   // il ne voit pas les suppressions d'un collègue
        refus(() -> corbeille.restaurer("devoir", devoirId), HttpStatus.FORBIDDEN, "auteur de la suppression");

        dans(ecoleB);
        connecte(adminB, "ADMIN");
        assertThat(elements()).isEmpty();
        refus(() -> corbeille.restaurer("devoir", devoirId), HttpStatus.NOT_FOUND, null);
        refus(() -> corbeille.purger("devoir", devoirId, true), HttpStatus.NOT_FOUND, null);

        dans(ecoleA);
        connecte(adminA, "ADMIN");
        List<Map<String, Object>> tout = elements();
        assertThat(tout).hasSize(1);
        assertThat(tout.get(0)).containsEntry("peutRestaurer", true).containsEntry("peutPurger", true);
        corbeille.restaurer("devoir", devoirId);
        assertThat(devoirs.findById(devoirId)).isPresent();
        refus(() -> corbeille.restaurer("devoir", devoirId), HttpStatus.NOT_FOUND, null);
        refus(() -> corbeille.restaurer("inconnu", devoirId), HttpStatus.NOT_FOUND, "inconnu");
    }

    @Test
    void professeurQuiNEnseignePlusLeCoursNePeutPasRestaurer() {
        dans(ecoleA);
        connecte(profA, "PROFESSEUR");
        contenus.supprimerDevoir(devoirId, coursAutorise);
        CourseJpaEntity c = cours.findById(coursId).orElseThrow();
        c.setProfId(profA2.getId());
        cours.save(c);
        assertThat(elements().get(0)).containsEntry("peutRestaurer", false);
        refus(() -> corbeille.restaurer("devoir", devoirId), HttpStatus.FORBIDDEN, "n'est plus le vôtre");
    }

    @Test
    void conflitsParentDansLaCorbeille() {
        dans(ecoleA);
        connecte(profA, "PROFESSEUR");
        contenus.supprimerSupport(supportId, coursAutorise);
        contenus.supprimerModule(moduleId, coursAutorise);   // vide désormais : le support est dans la corbeille
        refus(() -> corbeille.restaurer("support", supportId), HttpStatus.CONFLICT, "Module “Chapitre 3”");
        assertThat(elements()).filteredOn(e -> "support".equals(e.get("type")))
                .singleElement().satisfies(e -> assertThat((String) e.get("conflit")).contains("restaurez-le d'abord"));
        corbeille.restaurer("module", moduleId);
        corbeille.restaurer("support", supportId);
        assertThat(supports.findById(supportId)).isPresent();

        connecte(adminA, "ADMIN");
        planning.deleteClasse(classeId);
        planning.deleteFiliere(filiereId);
        refus(() -> corbeille.restaurer("classe", classeId), HttpStatus.CONFLICT, "Filière “Informatique”");
        corbeille.restaurer("filiere", filiereId);
        corbeille.restaurer("classe", classeId);
        assertThat(classes.findById(classeId)).isPresent();
    }

    @Test
    void recreerSousLeMemeIdentifiantUnElementDeLaCorbeilleEstRefuse() {
        dans(ecoleA);
        connecte(adminA, "ADMIN");
        planning.deleteRoom(salleId);
        refus(() -> planningAdmin.saveRooms(List.of(RoomFactory.reconstruct(salleId, "Salle 12", 30, List.of()))),
                HttpStatus.CONFLICT, "dans la corbeille");
    }

    @Test
    void purgeManuelleParLAdministrationAvecConfirmation() {
        dans(ecoleA);
        connecte(adminA, "ADMIN");
        planning.deleteRoom(salleId);

        connecte(profA, "PROFESSEUR");
        refus(() -> corbeille.purger("salle", salleId, true), HttpStatus.FORBIDDEN, "administration");

        connecte(adminA, "ADMIN");
        refus(() -> corbeille.purger("salle", salleId, false), HttpStatus.BAD_REQUEST, "Confirmez");
        Map<String, Object> r = corbeille.purger("salle", salleId, true);
        assertThat(r).containsEntry("purge", true);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rooms WHERE id = ?", Integer.class, salleId)).isZero();
        refus(() -> corbeille.restaurer("salle", salleId), HttpStatus.NOT_FOUND, null);
        assertThat(journal(ecoleA.getId())).anySatisfy(d -> assertThat(d)
                .startsWith("Suppression définitive (vidée de la corbeille par l'administration) : Salle “Salle 12”"));
    }

    @Test
    void purgeAutomatiqueApresTrenteJoursAvecLesTentativesDuQuiz() {
        dans(ecoleA);
        connecte(profA, "PROFESSEUR");
        refus(() -> contenus.supprimerQuiz(quizId, false, coursAutorise), HttpStatus.CONFLICT, "confirmez");
        contenus.supprimerQuiz(quizId, true, coursAutorise);
        assertThat(tentatives.findByQuizId(quizId)).isEmpty();   // masquées avec le quiz
        contenus.supprimerDevoir(devoirId, coursAutorise);

        // le quiz a été supprimé il y a 31 jours, le devoir aujourd'hui
        jdbc.update("UPDATE quizzes SET supprime_le = ? WHERE id = ?", Timestamp.valueOf(LocalDateTime.now().minusDays(31)), quizId);
        TenantContext.clear();   // la tâche planifiée tourne hors de toute école
        int n = corbeille.purgerExpires();

        assertThat(n).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quizzes WHERE id = ?", Integer.class, quizId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quiz_tentatives WHERE quiz_id = ?", Integer.class, quizId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM homeworks WHERE id = ?", Integer.class, devoirId)).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rendus_devoirs WHERE homework_id = ?", Integer.class, devoirId)).isOne();
        assertThat(journal(ecoleA.getId())).anySatisfy(d -> assertThat(d)
                .startsWith("Suppression définitive (30 jours dans la corbeille) : Quiz “Quiz boucles”")
                .endsWith("avec 1 tentative d'élève"));   // 16/11 : pluriel réel (avant : « tentative(s) d'élèves »)

        dans(ecoleA);
        assertThat(elements()).extracting(e -> e.get("id")).containsExactly(devoirId);
    }
}
