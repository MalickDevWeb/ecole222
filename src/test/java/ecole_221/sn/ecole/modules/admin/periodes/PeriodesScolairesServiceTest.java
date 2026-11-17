package ecole_221.sn.ecole.modules.admin.periodes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** Trimestres et clôture des notes (BR-04). */
class PeriodesScolairesServiceTest {

    private final List<PeriodeScolaireJpaEntity> base = new ArrayList<>();
    private PeriodesScolairesService service;

    @BeforeEach
    void preparer() {
        PeriodeScolaireJpaRepository repo = Mockito.mock(PeriodeScolaireJpaRepository.class);
        Mockito.when(repo.findAllByOrderByDateDebutAsc()).thenAnswer(i -> base.stream()
                .sorted(Comparator.comparing(PeriodeScolaireJpaEntity::getDateDebut)).toList());
        Mockito.when(repo.save(any())).thenAnswer(i -> {
            PeriodeScolaireJpaEntity p = i.getArgument(0);
            base.removeIf(x -> x.getId().equals(p.getId()));
            base.add(p);
            return p;
        });
        Mockito.when(repo.findById(anyString())).thenAnswer(i -> base.stream().filter(p -> p.getId().equals(i.getArgument(0))).findFirst());
        service = new PeriodesScolairesService(repo);
        connecter("ADMIN");
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private String creer(String libelle, String debut, String fin) {
        return (String) service.creer(Map.of("libelle", libelle, "dateDebut", debut, "dateFin", fin)).get("id");
    }

    @Test
    void periodesSansChevauchementNiDoublon() {
        creer("Trimestre 1", "2026-10-01", "2026-12-20");
        assertThatThrownBy(() -> creer("Trimestre 2", "2026-12-15", "2027-03-31"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("chevauchent");
        assertThatThrownBy(() -> creer("trimestre 1", "2027-01-05", "2027-03-31"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("déjà le libellé");
        assertThatThrownBy(() -> creer("Trimestre 3", "2027-06-30", "2027-04-01"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("après la date de début");
        assertThatThrownBy(() -> creer("Trimestre 3", "2027-04-01", "31/06/2027"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("invalide");
    }

    @Test
    void clotureFigeLesNotesDeLaPeriodeSeulement() {
        String t1 = creer("Trimestre 1", "2026-10-01", "2026-12-20");
        creer("Trimestre 2", "2027-01-05", "2027-03-31");
        service.exigerOuverte(LocalDate.parse("2026-11-10"), "x");   // ouverte : rien
        service.cloturer(t1);
        assertThatThrownBy(() -> service.exigerOuverte(LocalDate.parse("2026-11-10"), "les notes ne peuvent plus être modifiées"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Trimestre 1").hasMessageContaining("clôturée");
        service.exigerOuverte(LocalDate.parse("2027-02-01"), "x");   // trimestre 2 ouvert
        service.exigerOuverte(LocalDate.parse("2026-12-30"), "x");   // vacances : hors période
        assertThatThrownBy(() -> service.modifier(t1, Map.of("dateFin", "2026-12-21"))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.supprimer(t1)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reouvertureReserveeALAdministration() {
        String t1 = creer("Trimestre 1", "2026-10-01", "2026-12-20");
        service.cloturer(t1);
        connecter("SECRETAIRE");
        assertThatThrownBy(() -> service.rouvrir(t1)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("administration");
        connecter("ADMIN");
        assertThat(service.rouvrir(t1).get("cloturee")).isEqualTo(false);
        service.exigerOuverte(LocalDate.parse("2026-11-10"), "x");
        assertThat(Optional.empty()).isEmpty();
    }
}
