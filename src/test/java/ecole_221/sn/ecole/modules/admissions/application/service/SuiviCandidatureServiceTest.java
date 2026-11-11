package ecole_221.sn.ecole.modules.admissions.application.service;

import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Suivi de candidature : seul le candidat (email + mot de passe) voit SON dossier ; tentatives freinées. */
class SuiviCandidatureServiceTest {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
    private final List<CandidateJpaEntity> base = new ArrayList<>();
    private final MutableClock horloge = new MutableClock(Instant.parse("2026-11-11T10:00:00Z"));
    private SuiviCandidatureService service;

    static class MutableClock extends Clock {
        Instant t;
        MutableClock(Instant t) { this.t = t; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return t; }
    }

    @BeforeEach
    void preparer() {
        CandidateJpaRepository repo = mock(CandidateJpaRepository.class);
        when(repo.existsByEmailIgnoreCase(anyString())).thenAnswer(i -> base.stream().anyMatch(c -> c.getEmail().equalsIgnoreCase(i.getArgument(0))));
        when(repo.findByEmailIgnoreCase(anyString())).thenAnswer(i -> base.stream().filter(c -> c.getEmail().equalsIgnoreCase(i.getArgument(0))).toList());
        service = new SuiviCandidatureService(repo, bcrypt, horloge);
        CandidateJpaEntity c = new CandidateJpaEntity("CAND-2026-abcd1234", "Awa", "Diop", "awa.diop@exemple.sn", "770000000", "Informatique");
        c.setMotDePasseSuivi(bcrypt.encode("MonSuivi-2026"));
        c.setAdmissionStatus("REJECTED");
        c.setRejectionReason("Dossier incomplet : relevés de notes absents.");
        c.setSubmittedAt(LocalDateTime.of(2026, 11, 1, 9, 0));
        c.setIdCardUploaded(true);
        c.setRegistrationFeeReference("OM-778899");
        base.add(c);
        CandidateJpaEntity parParent = new CandidateJpaEntity("CAND-2026-par", "Modou", "Fall", "modou@exemple.sn", null, "Gestion");
        base.add(parParent);   // déposé par un parent : aucun mot de passe de suivi
    }

    private static void refus(Runnable r, HttpStatus code, String extrait) {
        assertThatThrownBy(r::run).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(code))
                .hasMessageContaining(extrait);
    }

    @Test
    void bonEmailEtMotDePasseDonnentSonDossierSansLeMotDePasse() {
        List<Map<String, Object>> d = service.suivre("  AWA.Diop@exemple.sn ", "MonSuivi-2026");
        assertThat(d).hasSize(1);
        assertThat(d.get(0)).containsEntry("id", "CAND-2026-abcd1234").containsEntry("statut", "REFUSE")
                .containsEntry("motifRefus", "Dossier incomplet : relevés de notes absents.")
                .containsEntry("pieceIdentiteDeclaree", true).containsEntry("diplomeDeclare", false)
                .containsEntry("frais", "DECLARES")
                .doesNotContainKey("motDePasseSuivi");
        assertThat(d.get(0).values()).noneMatch(v -> String.valueOf(v).startsWith("$2"));
    }

    @Test
    void echecGeneriqueQuelleQueSoitLaCause() {
        refus(() -> service.suivre("awa.diop@exemple.sn", "mauvais-mdp"), HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        refus(() -> service.suivre("inconnu@exemple.sn", "MonSuivi-2026"), HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        refus(() -> service.suivre("modou@exemple.sn", "nimporte"), HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        refus(() -> service.suivre("", "x"), HttpStatus.BAD_REQUEST, "Saisissez");
    }

    @Test
    void cinqEchecsBloquentQuinzeMinutesPuisLeBonMotDePasseRemarche() {
        for (int i = 0; i < 5; i++) {
            refus(() -> service.suivre("awa.diop@exemple.sn", "essai"), HttpStatus.UNAUTHORIZED, "incorrect");
        }
        refus(() -> service.suivre("awa.diop@exemple.sn", "MonSuivi-2026"), HttpStatus.TOO_MANY_REQUESTS, "Réessayez dans 15 minutes");
        horloge.t = horloge.t.plusSeconds(15 * 60 + 1);
        assertThat(service.suivre("awa.diop@exemple.sn", "MonSuivi-2026")).hasSize(1);
    }

    @Test
    void depotVerifieAvantEcriture() {
        Map<String, Object> ok = new HashMap<>(Map.of("prenom", "Fatou", "nom", "Sy", "email", "Fatou.Sy@Exemple.sn",
                "filiere", "Informatique", "motDePasse", "Suivi-Fatou-1"));
        SuiviCandidatureService.Depot d = service.verifierDepot(ok);
        assertThat(d.email()).isEqualTo("fatou.sy@exemple.sn");
        assertThat(bcrypt.matches("Suivi-Fatou-1", d.motDePasseHache())).isTrue();

        Map<String, Object> court = new HashMap<>(ok); court.put("motDePasse", "1234567");
        refus(() -> service.verifierDepot(court), HttpStatus.BAD_REQUEST, "au moins 8 caractères");
        Map<String, Object> sansEmail = new HashMap<>(ok); sansEmail.remove("email");
        refus(() -> service.verifierDepot(sansEmail), HttpStatus.BAD_REQUEST, "e-mail est obligatoire");
        Map<String, Object> mauvaisEmail = new HashMap<>(ok); mauvaisEmail.put("email", "fatou");
        refus(() -> service.verifierDepot(mauvaisEmail), HttpStatus.BAD_REQUEST, "« fatou » n'est pas valide");
        Map<String, Object> sansFiliere = new HashMap<>(ok); sansFiliere.remove("filiere");
        refus(() -> service.verifierDepot(sansFiliere), HttpStatus.BAD_REQUEST, "formation");
        Map<String, Object> doublon = new HashMap<>(ok); doublon.put("email", "AWA.DIOP@exemple.sn");
        refus(() -> service.verifierDepot(doublon), HttpStatus.CONFLICT, "existe déjà");
    }
}
