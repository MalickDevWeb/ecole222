package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ObservationEleveJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ObservationEleveJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Observations d'un élève : réellement enregistrées, uniquement pour un élève de l'école. */
class ObservationsElevesServiceTest {

    private final List<ObservationEleveJpaEntity> base = new ArrayList<>();
    private ObservationsElevesService service;

    @BeforeEach
    void preparer() {
        ObservationEleveJpaRepository repo = mock(ObservationEleveJpaRepository.class);
        when(repo.save(any())).thenAnswer(i -> { base.add(0, i.getArgument(0)); return i.getArgument(0); });
        when(repo.findByEleveIdOrderByCreeLeDesc(anyString())).thenAnswer(i ->
                base.stream().filter(o -> o.getEleveId().equals(i.getArgument(0))).toList());
        ComptesEcoleService comptesEcole = mock(ComptesEcoleService.class);
        when(comptesEcole.membre("eleve-1", "ETUDIANT")).thenReturn(mock(UserJpaEntity.class));
        when(comptesEcole.membre("eleve-autre-ecole", "ETUDIANT"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable dans cet établissement."));
        UserJpaRepository comptes = mock(UserJpaRepository.class);
        UserJpaEntity admin = mock(UserJpaEntity.class);
        when(admin.getPrenom()).thenReturn("Ibrahima");
        when(admin.getNom()).thenReturn("Diop");
        when(comptes.findById("admin-1")).thenReturn(Optional.of(admin));
        service = new ObservationsElevesService(repo, comptesEcole, comptes,
                Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneId.of("Africa/Dakar")));
    }

    private static void refus(Runnable r, HttpStatus code, String extrait) {
        assertThatThrownBy(r::run).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(code))
                .hasMessageContaining(extrait);
    }

    @Test
    void observationEnregistreeEtRelueAvecSonAuteur() {
        Map<String, Object> o = service.ajouter("eleve-1", "admin-1", "comportement", "  Bavardages répétés en cours de maths. ");
        assertThat(o.get("type")).isEqualTo("Comportement");
        assertThat(o.get("text")).isEqualTo("Bavardages répétés en cours de maths.");
        assertThat(o.get("auteur")).isEqualTo("Ibrahima Diop");
        assertThat(o.get("date")).isEqualTo("2026-11-11");
        assertThat(service.liste("eleve-1")).hasSize(1);
    }

    @Test
    void saisiesInvalidesRefuseesSansRienEnregistrer() {
        refus(() -> service.ajouter("eleve-1", "admin-1", "Humeur", "x"), HttpStatus.BAD_REQUEST, "Catégorie inconnue : « Humeur »");
        refus(() -> service.ajouter("eleve-1", "admin-1", null, "x"), HttpStatus.BAD_REQUEST, "Choisissez une catégorie");
        refus(() -> service.ajouter("eleve-1", "admin-1", "Notes", "   "), HttpStatus.BAD_REQUEST, "vide");
        refus(() -> service.ajouter("eleve-1", "admin-1", "Notes", "x".repeat(1001)), HttpStatus.BAD_REQUEST, "1001 caractères");
        assertThat(base).isEmpty();
    }

    @Test
    void eleveHorsEcoleIntrouvable() {
        refus(() -> service.ajouter("eleve-autre-ecole", "admin-1", "Notes", "x"), HttpStatus.NOT_FOUND, "introuvable");
        refus(() -> service.liste("eleve-autre-ecole"), HttpStatus.NOT_FOUND, "introuvable");
        assertThat(base).isEmpty();
    }
}
