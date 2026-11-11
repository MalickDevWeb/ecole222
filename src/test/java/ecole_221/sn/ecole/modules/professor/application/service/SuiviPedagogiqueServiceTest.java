package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Lot E : tableau de bord de classe, fiche élève et cahier de textes calculés sur les seules données saisies. */
class SuiviPedagogiqueServiceTest {

    private CourseJpaRepository cours;
    private GradeJpaRepository notes;
    private AttendanceJpaRepository presences;
    private InscriptionClasseJpaRepository inscriptions;
    private ScheduleSessionJpaRepository seances;
    private CahierDeTextesJpaRepository cahier;
    private SuiviPedagogiqueService service;
    private CourseJpaEntity c;
    private final List<CahierDeTextesJpaEntity> entrees = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cours = mock(CourseJpaRepository.class);
        notes = mock(GradeJpaRepository.class);
        presences = mock(AttendanceJpaRepository.class);
        inscriptions = mock(InscriptionClasseJpaRepository.class);
        seances = mock(ScheduleSessionJpaRepository.class);
        cahier = mock(CahierDeTextesJpaRepository.class);
        ComptesEcoleService comptesEcole = mock(ComptesEcoleService.class);
        when(comptesEcole.ecoleCourante()).thenReturn("org-a");
        when(comptesEcole.membres("ETUDIANT")).thenReturn(List.of(
                membre("e1"), membre("e2"), membre("e3"), membre("hors-classe")));
        service = new SuiviPedagogiqueService(cours, notes, presences, inscriptions, seances, cahier, comptesEcole);

        c = new CourseJpaEntity();
        c.setId("c-1");
        c.setName("Algèbre");
        c.setClasse("cl-1");
        c.setTotalHours(20);
        c.setOrganizationId("org-a");
        when(cours.findById("c-1")).thenReturn(Optional.of(c));
        when(inscriptions.findByClasseId("cl-1")).thenReturn(List.of(inscr("e1", "cl-1"), inscr("e2", "cl-1"), inscr("e3", "cl-1")));
        when(cahier.findByCourseIdOrderByDateDescCreatedAtDesc("c-1")).thenAnswer(i -> entrees);
        when(cahier.save(any())).thenAnswer(i -> { entrees.add(i.getArgument(0)); return i.getArgument(0); });
        ScheduleSessionJpaEntity s = new ScheduleSessionJpaEntity("ses-1", "Algèbre", "Lundi", "08:00-10:00", "S1", "c-1", "CM", null, "cl-1", "prof-1");
        when(seances.findById("ses-1")).thenReturn(Optional.of(s));
        when(seances.findByCourseId("c-1")).thenReturn(List.of(s));
    }

    @Test
    void sans_note_ni_presence_la_moyenne_est_nulle_et_personne_n_est_en_difficulte() {
        Map<String, Object> t = service.tableauDeBord("c-1");
        assertThat(t.get("effectif")).isEqualTo(3);
        assertThat(t.get("moyenneClasse")).isNull();
        assertThat((List<?>) t.get("elevesEnDifficulte")).isEmpty();
        assertThat(t.get("heuresFaites")).isEqualTo(0.0);
        assertThat(t.get("progressionProgramme")).isEqualTo(0.0);
    }

    @Test
    void moyenne_difficultes_et_progression_suivent_les_donnees() {
        when(notes.findByCourseId("c-1")).thenReturn(List.of(note("e1", 8.0), note("e2", 14.0)));
        when(presences.findByCourseId("c-1")).thenReturn(List.of(
                presence("e3", "ABSENT"), presence("e3", "ABSENT"), presence("e3", "absent"), presence("e2", "ABSENT")));
        service.ajouterEntree("c-1", "prof-1", Map.of("sessionId", "ses-1", "date", LocalDate.now().toString(), "contenu", "Chapitre 1"));

        Map<String, Object> t = service.tableauDeBord("c-1");
        assertThat(t.get("moyenneClasse")).isEqualTo(11.0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diff = (List<Map<String, Object>>) t.get("elevesEnDifficulte");
        assertThat(diff).extracting(m -> m.get("id")).containsExactlyInAnyOrder("e1", "e3");
        assertThat(t.get("heuresFaites")).isEqualTo(2.0);
        assertThat(t.get("progressionProgramme")).isEqualTo(10.0);
    }

    @Test
    void progression_nulle_sans_volume_horaire() {
        c.setTotalHours(0);
        assertThat(service.tableauDeBord("c-1").get("progressionProgramme")).isNull();
    }

    @Test
    void fiche_eleve_notes_presences_et_taux() {
        GradeJpaEntity g = note("e1", 12.4);
        g.setCc(10.0);
        g.setExamen(14.0);
        when(notes.findByStudentIdAndCourseId("e1", "c-1")).thenReturn(Optional.of(g));
        when(presences.findByStudentIdAndCourseId("e1", "c-1")).thenReturn(List.of(
                presence("e1", "PRESENT", "2026-11-03"), presence("e1", "ABSENT", "2026-11-05"),
                presence("e1", "RETARD", "2026-11-04"), presence("e1", "PRESENT", "2026-11-06")));
        Map<String, Object> f = service.ficheEleve("c-1", "e1");
        @SuppressWarnings("unchecked") Map<String, Object> n = (Map<String, Object>) f.get("notes");
        assertThat(n.get("moyenne")).isEqualTo(12.4);
        @SuppressWarnings("unchecked") Map<String, Object> p = (Map<String, Object>) f.get("presences");
        assertThat(p.get("tauxPresence")).isEqualTo(75.0);
        assertThat(p.get("absences")).isEqualTo(1);
        @SuppressWarnings("unchecked") List<Map<String, Object>> liste = (List<Map<String, Object>>) p.get("liste");
        assertThat(liste.get(0).get("date")).isEqualTo("2026-11-06");
        assertThat(f).doesNotContainKey("rendus");   // dépôt absent : rubrique absente, pas inventée
    }

    @Test
    void fiche_sans_donnees_indicateurs_nuls() {
        Map<String, Object> f = service.ficheEleve("c-1", "e2");
        assertThat(f.get("notes")).isNull();
        @SuppressWarnings("unchecked") Map<String, Object> p = (Map<String, Object>) f.get("presences");
        assertThat(p.get("tauxPresence")).isNull();
    }

    @Test
    void fiche_d_un_eleve_hors_de_la_classe_404() {
        assertStatut(() -> service.ficheEleve("c-1", "hors-classe"), HttpStatus.NOT_FOUND);
    }

    @Test
    void cahier_de_textes_validations() {
        assertStatut(() -> service.ajouterEntree("c-1", "p", Map.of("sessionId", "ses-1", "date", LocalDate.now().toString())), HttpStatus.BAD_REQUEST);
        assertStatut(() -> service.ajouterEntree("c-1", "p", Map.of("sessionId", "ses-1", "date", LocalDate.now().plusDays(2).toString(), "contenu", "x")), HttpStatus.BAD_REQUEST);
        assertStatut(() -> service.ajouterEntree("c-1", "p", Map.of("sessionId", "ses-x", "date", LocalDate.now().toString(), "contenu", "x")), HttpStatus.NOT_FOUND);
        when(cahier.existsBySessionIdAndDate("ses-1", "2026-11-02")).thenReturn(true);
        assertStatut(() -> service.ajouterEntree("c-1", "p", Map.of("sessionId", "ses-1", "date", "2026-11-02", "contenu", "x")), HttpStatus.CONFLICT);
    }

    @Test
    void cahier_lu_par_un_eleve_de_la_classe_seulement() {
        service.ajouterEntree("c-1", "prof-1", Map.of("sessionId", "ses-1", "date", "2026-11-02", "contenu", "Chapitre 1", "travailAFaire", "Exercices 1 à 5"));
        when(inscriptions.findByUserId("e1")).thenReturn(List.of(inscr("e1", "cl-1")));
        when(inscriptions.findByUserId("autre")).thenReturn(List.of(inscr("autre", "cl-2")));
        List<Map<String, Object>> lu = service.cahierDeTextesPourEleve("c-1", "e1");
        assertThat(lu).hasSize(1);
        assertThat(lu.get(0).get("travailAFaire")).isEqualTo("Exercices 1 à 5");
        assertStatut(() -> service.cahierDeTextesPourEleve("c-1", "autre"), HttpStatus.NOT_FOUND);
    }

    @Test
    void duree_des_creneaux() {
        assertThat(SuiviPedagogiqueService.dureeHeures("08:00-10:00")).isEqualTo(2.0);
        assertThat(SuiviPedagogiqueService.dureeHeures("8h30 - 10h")).isEqualTo(1.5);
        assertThat(SuiviPedagogiqueService.dureeHeures("matin")).isNull();
        assertThat(SuiviPedagogiqueService.dureeHeures("10:00-08:00")).isNull();
    }

    private static Map.Entry<UserJpaEntity, OrganizationUserJpaEntity> membre(String id) {
        UserJpaEntity u = new UserJpaEntity(id, "Nom" + id, "Prenom" + id, id + "@exemple.sn", "h", "ETUDIANT", "M-" + id, null);
        OrganizationUserJpaEntity l = new OrganizationUserJpaEntity();
        l.setUserId(id);
        l.setStatus("ACTIVE");
        return Map.entry(u, l);
    }

    private static InscriptionClasseJpaEntity inscr(String user, String classe) {
        return new InscriptionClasseJpaEntity("ins-" + user, user, classe);
    }

    private static GradeJpaEntity note(String eleve, double moyenne) {
        GradeJpaEntity g = new GradeJpaEntity();
        g.setStudentId(eleve);
        g.setCourseId("c-1");
        g.setCc(moyenne);
        g.setAverage(moyenne);
        return g;
    }

    private static AttendanceJpaEntity presence(String eleve, String statut) {
        return presence(eleve, statut, "2026-11-02");
    }

    private static AttendanceJpaEntity presence(String eleve, String statut, String date) {
        AttendanceJpaEntity a = new AttendanceJpaEntity();
        a.setStudentId(eleve);
        a.setStatus(statut);
        a.setDate(date);
        a.setCourseId("c-1");
        return a;
    }

    private static void assertStatut(Runnable r, HttpStatus attendu) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(attendu));
    }
}
