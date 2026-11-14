package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/** Emploi du temps réel de l'élève (2026-11-14) : séances de SES classes, triées, jamais inventées. */
class EmploiDuTempsEleveServiceTest {

    private final InscriptionClasseJpaRepository inscriptions = mock(InscriptionClasseJpaRepository.class);
    private final ScheduleSessionJpaRepository seances = mock(ScheduleSessionJpaRepository.class);
    private final UserJpaRepository comptes = mock(UserJpaRepository.class);
    private final ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository classes =
            mock(ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository.class);
    private final EmploiDuTempsEleveService service = new EmploiDuTempsEleveService(inscriptions, seances, comptes, classes);

    private static ScheduleSessionJpaEntity seance(String id, String jour, String horaire, String statut) {
        ScheduleSessionJpaEntity s = new ScheduleSessionJpaEntity();
        s.setId(id);
        s.setCourseTitle("Cours " + id);
        s.setDay(jour);
        s.setTime(horaire);
        s.setStatus(statut);
        s.setClasse("cls-L1");
        s.setProfessorId("p1");
        s.setType("td");
        return s;
    }

    @Test
    void sansClasseRienNEstRenvoye() {
        when(inscriptions.findByUserId("e1")).thenReturn(List.of());
        assertThat(service.seances("e1")).isEmpty();
        assertThat(service.seances(null)).isEmpty();
        verifyNoInteractions(seances);
    }

    @Test
    void seancesDeSaClasseTrieesAvecLeNomDuProfesseur() {
        when(inscriptions.findByUserId("e1")).thenReturn(List.of(new InscriptionClasseJpaEntity("i1", "e1", "cls-L1")));
        UserJpaEntity prof = new UserJpaEntity();
        prof.setId("p1");
        prof.setPrenom("Awa");
        prof.setNom("Diop");
        when(comptes.findAllById(any())).thenReturn(List.of(prof));
        var l1 = new ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity();
        l1.setId("cls-L1");
        l1.setName("L1 Informatique");
        when(classes.findAllById(any())).thenReturn(List.of(l1));
        when(seances.findByClasseIn(Set.of("cls-L1"))).thenReturn(List.of(
                seance("s3", "Samedi", "08:00-10:00", "PLANIFIE"),
                seance("s2", "MONDAY", "14h-16h", "annule"),
                seance("s1", "Lundi", "08:00 - 10:00", "PLANIFIE"),
                seance("s4", "Lundi", "10:00-12:00", "retire"),       // créneau retiré : absent
                seance("s5", "Jour inconnu", "10:00-12:00", "PLANIFIE")));
        var liste = service.seances("e1");
        assertThat(liste).extracting(EmploiDuTempsEleveService.Seance::id).containsExactly("s1", "s2", "s3");
        var lundi = liste.get(0);
        assertThat(lundi.jourCode()).isEqualTo("LUN");
        assertThat(lundi.jour()).isEqualTo("Lundi");
        assertThat(lundi.heureDebut()).isEqualTo("08:00");
        assertThat(lundi.heureFin()).isEqualTo("10:00");
        assertThat(lundi.professeur()).isEqualTo("Awa Diop");
        assertThat(lundi.type()).isEqualTo("TD");
        assertThat(lundi.classe()).isEqualTo("L1 Informatique");
        assertThat(liste.get(1).heureDebut()).isEqualTo("14:00");
        assertThat(liste.get(1).statut()).isEqualTo("annule");
        assertThat(liste.get(2).jourCode()).isEqualTo("SAM");
    }
}
