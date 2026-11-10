package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Lot E : changement du mot de passe provisoire (règles, ancien faux, identique, effacement du drapeau). */
class ChangementMotDePasseServiceTest {

    private static final String PROVISOIRE = "Prov" + System.nanoTime();
    private final BCryptPasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private UserJpaRepository comptes;
    private ChangementMotDePasseService service;
    private UserJpaEntity compte;

    @BeforeEach
    void setUp() {
        comptes = mock(UserJpaRepository.class);
        service = new ChangementMotDePasseService(comptes, encodeur);
        compte = new UserJpaEntity("usr-1", "Diop", "Awa", "awa@test.sn", encodeur.encode(PROVISOIRE), "PROFESSEUR", "PRF-1", null);
        compte.setDoitChangerMotDePasse(true);
        when(comptes.findById("usr-1")).thenReturn(Optional.of(compte));
    }

    @Test
    void un_changement_valide_remplace_le_mot_de_passe_et_efface_le_drapeau() {
        service.changer("usr-1", PROVISOIRE, "NouveauMotDePasse2026");
        verify(comptes).save(compte);
        assertThat(compte.getDoitChangerMotDePasse()).isNull();
        assertThat(encodeur.matches("NouveauMotDePasse2026", compte.getPassword())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"court1", "sanschiffreaucun", "1234567890123", ""})
    void les_mots_de_passe_faibles_sont_refuses_en_400(String nouveau) {
        assertStatut(() -> service.changer("usr-1", PROVISOIRE, nouveau), HttpStatus.BAD_REQUEST);
        verify(comptes, never()).save(any());
    }

    @Test
    void un_ancien_mot_de_passe_faux_est_refuse() {
        assertThatThrownBy(() -> service.changer("usr-1", "mauvais-ancien", "NouveauMotDePasse2026"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ancien mot de passe est incorrect");
        verify(comptes, never()).save(any());
    }

    @Test
    void le_nouveau_doit_differer_de_l_ancien() {
        String ancien = "MotDePasse2026a";
        compte.setPassword(encodeur.encode(ancien));
        assertThatThrownBy(() -> service.changer("usr-1", ancien, ancien))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("différent de l'ancien");
    }

    @Test
    void sans_utilisateur_connecte_401() {
        assertStatut(() -> service.changer(null, PROVISOIRE, "NouveauMotDePasse2026"), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void regles_explicites() {
        assertThat(ChangementMotDePasseService.erreurRegles("abcdefghi1")).isNull();
        assertThat(ChangementMotDePasseService.erreurRegles("abcdefgh1")).contains("10 caractères");
        assertThat(ChangementMotDePasseService.erreurRegles("abcdefghij")).contains("une lettre et un chiffre");
    }

    private static void assertStatut(Runnable r, HttpStatus attendu) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(attendu));
    }
}
