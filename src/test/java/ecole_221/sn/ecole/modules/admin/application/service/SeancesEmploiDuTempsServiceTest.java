package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.admin.domain.model.TimeSlot;
import ecole_221.sn.ecole.modules.admin.domain.model.TimeSlotFactory;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Emploi du temps de l'administration → séances des professeurs (2026-11-14). */
class SeancesEmploiDuTempsServiceTest {

    private final ComptesEcoleService comptes = mock(ComptesEcoleService.class);
    private final CourseJpaRepository cours = mock(CourseJpaRepository.class);
    private final ScheduleSessionJpaRepository seances = mock(ScheduleSessionJpaRepository.class);
    private final SeancesEmploiDuTempsService service = new SeancesEmploiDuTempsService(comptes, cours, seances);
    private final List<ScheduleSessionJpaEntity> enregistrees = new ArrayList<>();

    private static UserJpaEntity prof(String id, String prenom, String nom) {
        UserJpaEntity u = new UserJpaEntity();
        u.setId(id);
        u.setPrenom(prenom);
        u.setNom(nom);
        return u;
    }

    private static TimeSlot creneau(String id, String jour, String nom, String prof, String classe) {
        return TimeSlotFactory.reconstruct(id, jour, "08:00", "10:00", nom, "CM", prof, "Salle 1", "", classe);
    }

    @BeforeEach
    void bases() {
        when(comptes.membres("PROFESSEUR")).thenReturn(List.of(
                Map.entry(prof("p1", "Awa", "Diop"), mock(ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity.class)),
                Map.entry(prof("p2", "Moussa", "Fall"), mock(ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity.class))));
        when(cours.findAll()).thenReturn(List.of(new CourseJpaEntity("crs-algo", "Algorithmique", "cls-L1", 30, "p1")));
        when(seances.save(any())).thenAnswer(i -> { enregistrees.add(i.getArgument(0)); return i.getArgument(0); });
    }

    @Test
    void creneauDevientSeanceDuCoursEtDuProfesseur() {
        when(seances.findAll()).thenReturn(List.of());
        var bilan = service.synchroniser(List.of(creneau("c1", "Lundi", "algorithmique ", "Awa Diop", "cls-L1")));
        assertThat(bilan.seances()).isEqualTo(1);
        assertThat(bilan.sansCours()).isEmpty();
        ScheduleSessionJpaEntity s = enregistrees.get(0);
        assertThat(s.getId()).isEqualTo("cre-c1");
        assertThat(s.getCourseId()).isEqualTo("crs-algo");
        assertThat(s.getProfessorId()).isEqualTo("p1");
        assertThat(s.getClasse()).isEqualTo("cls-L1");
        assertThat(s.getDay()).isEqualTo("Lundi");
        assertThat(s.getTime()).isEqualTo("08:00-10:00");
        assertThat(s.getStatus()).isEqualTo("PLANIFIE");
    }

    @Test
    void creneauSansCoursSignaleEtProfesseurReconnuParSonNom() {
        when(seances.findAll()).thenReturn(List.of());
        var bilan = service.synchroniser(List.of(creneau("c2", "Mardi", "Réseaux", "Moussa Fall", "cls-L1"),
                creneau("c3", "Mardi", "Réseaux", "Moussa Fall", "")));   // sans classe : ignoré
        assertThat(bilan.seances()).isEqualTo(1);
        assertThat(bilan.sansCours()).hasSize(1).first().asString().contains("Réseaux");
        assertThat(enregistrees.get(0).getCourseId()).isNull();
        assertThat(enregistrees.get(0).getProfessorId()).isEqualTo("p2");
    }

    @Test
    void creneauRetireSeanceRetireeSansToucherAuxAutres() {
        ScheduleSessionJpaEntity derivee = new ScheduleSessionJpaEntity();
        derivee.setId("cre-ancien");
        derivee.setStatus("PLANIFIE");
        ScheduleSessionJpaEntity manuelle = new ScheduleSessionJpaEntity();
        manuelle.setId("ses-manuelle");
        manuelle.setStatus("PLANIFIE");
        when(seances.findAll()).thenReturn(List.of(derivee, manuelle));
        var bilan = service.synchroniser(List.of());
        assertThat(bilan.retirees()).isEqualTo(1);
        assertThat(derivee.getStatus()).isEqualTo("retire");
        assertThat(manuelle.getStatus()).isEqualTo("PLANIFIE");
    }

    @Test
    void annulationDuProfesseurGardeeTantQueLeCreneauNeBougePas() {
        ScheduleSessionJpaEntity s = new ScheduleSessionJpaEntity();
        s.setId("cre-c1");
        s.setDay("Lundi");
        s.setTime("08:00-10:00");
        s.setRoom("Salle 1");
        s.setStatus("annule");
        s.setCancellationReason("Grève");
        when(seances.findAll()).thenReturn(List.of(s));
        service.synchroniser(List.of(creneau("c1", "Lundi", "Algorithmique", "Awa Diop", "cls-L1")));
        assertThat(s.getStatus()).isEqualTo("annule");
        service.synchroniser(List.of(creneau("c1", "Jeudi", "Algorithmique", "Awa Diop", "cls-L1")));
        assertThat(s.getStatus()).isEqualTo("PLANIFIE");
        assertThat(s.getCancellationReason()).isNull();
    }

    @Test
    void horairesNormalisesAuFormatDuServeur() {
        assertThat(SeancesEmploiDuTempsService.horaire(TimeSlotFactory.reconstruct("x", "Mardi", "10h", "12h", "A", "CM", "", "", "", "c")))
                .isEqualTo("10:00-12:00");
        assertThat(SeancesEmploiDuTempsService.horaire(TimeSlotFactory.reconstruct("x", "Mardi", "8h30", "10:00", "A", "CM", "", "", "", "c")))
                .isEqualTo("08:30-10:00");
    }

    @Test
    void homonymesJamaisAttribuesAuHasard() {
        when(comptes.membres("PROFESSEUR")).thenReturn(List.of(
                Map.entry(prof("p1", "Awa", "Diop"), mock(ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity.class)),
                Map.entry(prof("p9", "Awa", "Diop"), mock(ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity.class))));
        when(seances.findAll()).thenReturn(List.of());
        var bilan = service.synchroniser(List.of(creneau("c4", "Lundi", "Inconnu", "Awa Diop", "cls-L1")));
        assertThat(enregistrees.get(0).getProfessorId()).isNull();
        assertThat(bilan.sansProfesseur()).hasSize(1);
    }
}
