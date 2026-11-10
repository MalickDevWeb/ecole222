package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.DemandeReportJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.DemandeReportJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Lot E : le professeur demande un report, l'administration accepte (séance modifiée) ou refuse avec motif. */
class DemandeReportServiceTest {

    private DemandeReportJpaRepository demandes;
    private ScheduleSessionJpaRepository seances;
    private DemandeReportService service;
    private ScheduleSessionJpaEntity seance;
    private final Map<String, DemandeReportJpaEntity> base = new HashMap<>();

    @BeforeEach
    void setUp() {
        demandes = mock(DemandeReportJpaRepository.class);
        seances = mock(ScheduleSessionJpaRepository.class);
        service = new DemandeReportService(demandes, seances, mock(UserJpaRepository.class));
        seance = new ScheduleSessionJpaEntity("ses-1", "Algèbre", "Lundi", "08:00-10:00", "S1", "c-1", "CM", null, "cl-1", "prof-1");
        when(seances.findById("ses-1")).thenReturn(Optional.of(seance));
        when(demandes.save(any())).thenAnswer(i -> { DemandeReportJpaEntity d = i.getArgument(0); base.put(d.getId(), d); return d; });
        when(demandes.findById(any())).thenAnswer(i -> Optional.ofNullable(base.get((String) i.getArgument(0))));
    }

    private static Map<String, Object> corps() {
        return Map.of("jour", "Mardi", "horaire", "10:00-12:00", "salle", "S2", "motif", "Conférence");
    }

    @Test
    void la_demande_est_en_attente_et_ne_modifie_pas_la_seance() {
        Map<String, Object> d = service.demander("prof-1", "ses-1", corps());
        assertThat(d.get("statut")).isEqualTo("EN_ATTENTE");
        assertThat(d.get("jourActuel")).isEqualTo("Lundi");
        assertThat(seance.getDay()).isEqualTo("Lundi");
        verify(seances, never()).save(any());
    }

    @Test
    void un_professeur_ne_demande_pas_pour_la_seance_d_un_collegue() {
        assertStatut(() -> service.demander("prof-2", "ses-1", corps()), HttpStatus.FORBIDDEN);
    }

    @Test
    void champs_obligatoires() {
        assertStatut(() -> service.demander("prof-1", "ses-1", Map.of("jour", "Mardi", "horaire", "10:00-12:00", "salle", "S2")), HttpStatus.BAD_REQUEST);
        assertStatut(() -> service.demander("prof-1", "ses-1", Map.of("motif", "x")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void une_seule_demande_en_attente_par_seance() {
        when(demandes.existsBySessionIdAndStatut("ses-1", "EN_ATTENTE")).thenReturn(true);
        assertStatut(() -> service.demander("prof-1", "ses-1", corps()), HttpStatus.CONFLICT);
    }

    @Test
    void accepter_applique_le_nouveau_creneau() {
        String id = (String) service.demander("prof-1", "ses-1", corps()).get("id");
        Map<String, Object> d = service.accepter(id, "admin-1");
        assertThat(d.get("statut")).isEqualTo("ACCEPTEE");
        assertThat(seance.getDay()).isEqualTo("Mardi");
        assertThat(seance.getTime()).isEqualTo("10:00-12:00");
        assertThat(seance.getRoom()).isEqualTo("S2");
        assertThat(seance.getStatus()).isEqualTo("reporte");
        verify(seances).save(seance);
        assertStatut(() -> service.accepter(id, "admin-1"), HttpStatus.CONFLICT);
    }

    @Test
    void refuser_exige_un_motif_et_ne_touche_pas_la_seance() {
        String id = (String) service.demander("prof-1", "ses-1", corps()).get("id");
        assertStatut(() -> service.refuser(id, "admin-1", " "), HttpStatus.BAD_REQUEST);
        Map<String, Object> d = service.refuser(id, "admin-1", "Salle indisponible");
        assertThat(d.get("statut")).isEqualTo("REFUSEE");
        assertThat(d.get("motifRefus")).isEqualTo("Salle indisponible");
        assertThat(seance.getDay()).isEqualTo("Lundi");
        verify(seances, never()).save(any());
    }

    @Test
    void demande_inconnue_404() {
        assertStatut(() -> service.accepter("drp-inconnue", "admin-1"), HttpStatus.NOT_FOUND);
    }

    private static void assertStatut(Runnable r, HttpStatus attendu) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(attendu));
    }
}
