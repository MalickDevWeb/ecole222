package ecole_221.sn.ecole.modules.collaboration.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Horaire des classes virtuelles (16/11, demande du Tech Lead) : le lien apparaît aux élèves et aux parents à l'heure
 * de début et la classe virtuelle disparaît de leur écran après l'heure de fin.
 */
class HoraireClasseVirtuelleTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 16, 10, 0);

    private static HttpStatus refus(Map<String, Object> corps) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> HoraireClasseVirtuelle.lire(corps, MAINTENANT));
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    private static Map<String, Object> corps(String debut, String fin) {
        Map<String, Object> m = new HashMap<>();
        if (debut != null) m.put("debut", debut);
        if (fin != null) m.put("fin", fin);
        return m;
    }

    @Test
    void horaireValideLuSansLesSecondes() {
        var h = HoraireClasseVirtuelle.lire(corps("2026-11-16T14:00", "2026-11-16T15:30:45"), MAINTENANT);
        assertEquals(LocalDateTime.of(2026, 11, 16, 14, 0), h.debut());
        assertEquals(LocalDateTime.of(2026, 11, 16, 15, 30), h.fin());
        // séance déjà commencée mais pas finie : acceptée (le professeur publie en retard)
        assertDoesNotThrow(() -> HoraireClasseVirtuelle.lire(corps("2026-11-16T09:30", "2026-11-16T11:00"), MAINTENANT));
        assertEquals("le 16/11/2026 de 14:00 à 15:30", HoraireClasseVirtuelle.libelle(h.debut(), h.fin()));
    }

    @Test
    void horaireAbsentIncoherentPasseOuTropLongRefuse() {
        assertEquals(HttpStatus.BAD_REQUEST, refus(corps(null, "2026-11-16T15:00")));              // début absent
        assertEquals(HttpStatus.BAD_REQUEST, refus(corps("2026-11-16T14:00", null)));              // fin absente
        assertEquals(HttpStatus.BAD_REQUEST, refus(corps("16/11/2026 14:00", "2026-11-16T15:00"))); // mauvais format
        assertEquals(HttpStatus.BAD_REQUEST, refus(corps("2026-11-16T14:00", "2026-11-16T14:00"))); // fin = début
        assertEquals(HttpStatus.BAD_REQUEST, refus(corps("2026-11-16T14:00", "2026-11-16T13:00"))); // fin avant début
        assertEquals(HttpStatus.BAD_REQUEST, refus(corps("2026-11-16T08:00", "2026-11-16T09:00"))); // déjà terminée
        assertEquals(HttpStatus.BAD_REQUEST, refus(corps("2026-11-16T14:00", "2026-11-17T03:00"))); // plus de 12 h
        assertEquals(HttpStatus.BAD_REQUEST, refus(corps("2028-01-01T14:00", "2028-01-01T15:00"))); // plus d'un an
    }

    @Test
    void statutSelonLHeure() {
        LocalDateTime d = LocalDateTime.of(2026, 11, 16, 14, 0), f = d.plusHours(1);
        assertEquals("a_venir", HoraireClasseVirtuelle.statut(d, f, d.minusMinutes(1)));
        assertEquals("en_cours", HoraireClasseVirtuelle.statut(d, f, d));
        assertEquals("en_cours", HoraireClasseVirtuelle.statut(d, f, f));
        assertEquals("terminee", HoraireClasseVirtuelle.statut(d, f, f.plusMinutes(1)));
        assertEquals("sans_horaire", HoraireClasseVirtuelle.statut(null, f, d));
    }

    private static Map<String, Object> seance(String id, String statut) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("isPublished", true);
        m.put("classIds", List.of("c-l1"));
        m.put("link", "https://meet.google.com/abc-defg-hij");
        m.put("meetLink", "https://meet.google.com/abc-defg-hij");
        m.put("statut", statut);
        return m;
    }

    @Test
    void eleveVoitLeLienSeulementPendantLeCreneau() {
        InscriptionClasseJpaRepository inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
        Mockito.when(inscriptions.findByUserId("e1")).thenReturn(List.of(new InscriptionClasseJpaEntity("i1", "e1", "c-l1")));
        ClassesVirtuellesService service = new ClassesVirtuellesService(inscriptions, Mockito.mock(ParentEnfantJpaRepository.class),
                Mockito.mock(NotificationJpaRepository.class), Mockito.mock(CourseJpaRepository.class), Mockito.mock(ScheduleSessionJpaRepository.class));
        User eleve = UserFactory.reconstructSimple("e1", "Ba", "Fatou", "e1@ex.sn", "a", "ETU-1", RoleUtilisateur.ETUDIANT, null);

        List<Map<String, Object>> vus = service.filtrer(List.of(seance("avenir", "a_venir"), seance("encours", "en_cours"),
                seance("finie", "terminee"), seance("ancienne", "sans_horaire")), eleve);

        assertEquals(List.of("avenir", "encours"), vus.stream().map(m -> m.get("id")).toList());
        assertNull(vus.get(0).get("meetLink"), "à venir : annoncée, mais sans lien");
        assertNull(vus.get(0).get("link"));
        assertEquals("https://meet.google.com/abc-defg-hij", vus.get(1).get("meetLink"), "en cours : lien visible");
    }
}
