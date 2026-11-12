package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.ReinitialisationMotDePasseJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.ReinitialisationMotDePasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.email.domain.port.EmailServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class ReinitialisationMotDePasseServiceTest {

    static class HorlogeReglable extends Clock {
        Instant t = Instant.parse("2026-11-12T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return t; }
    }

    private final List<ReinitialisationMotDePasseJpaEntity> stockees = new ArrayList<>();
    private final List<String> emails = new ArrayList<>();
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
    private final HorlogeReglable horloge = new HorlogeReglable();
    private UserJpaEntity awa;
    private ReinitialisationMotDePasseService service;

    @BeforeEach
    void preparer() {
        UserJpaRepository comptes = Mockito.mock(UserJpaRepository.class);
        awa = new UserJpaEntity();
        awa.setId("u-awa");
        awa.setEmail("awa.diop@exemple.sn");
        awa.setPrenom("Awa");
        awa.setPassword(bcrypt.encode("Ancien-2026"));
        awa.setDoitChangerMotDePasse(true);
        Mockito.when(comptes.findByEmail("awa.diop@exemple.sn")).thenReturn(Optional.of(awa));
        Mockito.when(comptes.findByEmail("personne@exemple.sn")).thenReturn(Optional.empty());
        Mockito.when(comptes.findById("u-awa")).thenReturn(Optional.of(awa));

        ReinitialisationMotDePasseJpaRepository demandes = Mockito.mock(ReinitialisationMotDePasseJpaRepository.class);
        Mockito.when(demandes.save(any())).thenAnswer(i -> {
            ReinitialisationMotDePasseJpaEntity d = i.getArgument(0);
            if (!stockees.contains(d)) stockees.add(d);
            return d;
        });
        Mockito.when(demandes.findByJetonHache(anyString())).thenAnswer(i ->
                stockees.stream().filter(d -> d.getJetonHache().equals(i.getArgument(0))).findFirst());
        Mockito.when(demandes.findByUserIdAndUtiliseLeIsNull(anyString())).thenAnswer(i ->
                stockees.stream().filter(d -> d.getUserId().equals(i.getArgument(0)) && d.getUtiliseLe() == null).toList());

        EmailServicePort email = Mockito.mock(EmailServicePort.class);
        Mockito.when(email.sendEmail(anyString(), anyString(), anyString())).thenAnswer(i -> emails.add(i.getArgument(2)));
        service = new ReinitialisationMotDePasseService(comptes, demandes, bcrypt, email, "https://ecole221.sn/");
        service.horloge = horloge;
        service.envoi = Runnable::run;   // envoi synchrone pour le test
    }

    private String jetonDuDernierEmail() {
        Matcher m = Pattern.compile("jeton=([A-Za-z0-9_-]+)").matcher(emails.get(emails.size() - 1));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void lienEnvoyeJetonStockeHacheEtUtilisableUneSeuleFois() {
        service.demander(" Awa.Diop@Exemple.sn ", "ecole-221");
        assertThat(emails).hasSize(1);
        assertThat(emails.get(0)).contains("Bonjour Awa").contains("https://ecole221.sn/t/ecole-221/reinitialiser-mot-de-passe?jeton=")
                .contains("30 minutes");
        String jeton = jetonDuDernierEmail();
        assertThat(stockees.get(0).getJetonHache()).hasSize(64).isNotEqualTo(jeton).doesNotContain(jeton);

        service.reinitialiser(jeton, "Nouveau-2026x");
        assertThat(bcrypt.matches("Nouveau-2026x", awa.getPassword())).isTrue();
        assertThat(awa.getDoitChangerMotDePasse()).isNull();
        refus(() -> service.reinitialiser(jeton, "Encore-2026xx"), HttpStatus.BAD_REQUEST, "plus valide");
    }

    @Test
    void adresseInconnueMemeComportementSansEmail() {
        service.demander("personne@exemple.sn", "ecole-221");   // aucune erreur : pas de fuite
        assertThat(emails).isEmpty();
        assertThat(stockees).isEmpty();
    }

    @Test
    void lienExpireApres30MinutesEtMotDePasseFaibleRefuseSansConsommerLeLien() {
        service.demander("awa.diop@exemple.sn", null);
        String jeton = jetonDuDernierEmail();
        assertThat(emails.get(0)).contains("https://ecole221.sn/reinitialiser-mot-de-passe?jeton=");
        refus(() -> service.reinitialiser(jeton, "court1"), HttpStatus.BAD_REQUEST, "au moins 10 caractères");
        horloge.t = horloge.t.plusSeconds(30 * 60);
        refus(() -> service.reinitialiser(jeton, "Nouveau-2026x"), HttpStatus.BAD_REQUEST, "plus valide");
        assertThat(bcrypt.matches("Ancien-2026", awa.getPassword())).isTrue();
    }

    @Test
    void nouvelleDemandeAnnuleLeLienPrecedent() {
        service.demander("awa.diop@exemple.sn", null);
        String premier = jetonDuDernierEmail();
        service.demander("awa.diop@exemple.sn", null);
        String second = jetonDuDernierEmail();
        refus(() -> service.reinitialiser(premier, "Nouveau-2026x"), HttpStatus.BAD_REQUEST, "plus valide");
        service.reinitialiser(second, "Nouveau-2026x");
        assertThat(bcrypt.matches("Nouveau-2026x", awa.getPassword())).isTrue();
    }

    @Test
    void limiteDeTroisDemandesParQuartDHeureMemePourUneAdresseInconnue() {
        for (int i = 0; i < 3; i++) service.demander("personne@exemple.sn", null);
        refus(() -> service.demander("personne@exemple.sn", null), HttpStatus.TOO_MANY_REQUESTS, "Réessayez dans 15 minutes");
        horloge.t = horloge.t.plusSeconds(15 * 60 + 1);
        service.demander("personne@exemple.sn", null);
    }

    @Test
    void jetonInventeOuVideRefuse() {
        refus(() -> service.reinitialiser("n-importe-quoi", "Nouveau-2026x"), HttpStatus.BAD_REQUEST, "plus valide");
        refus(() -> service.reinitialiser("", "Nouveau-2026x"), HttpStatus.BAD_REQUEST, "plus valide");
        refus(() -> service.demander("awa@", null), HttpStatus.BAD_REQUEST, "invalide");
    }

    private static void refus(Runnable r, HttpStatus statut, String extrait) {
        assertThatThrownBy(r::run).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(statut);
                    assertThat(((ResponseStatusException) e).getReason()).contains(extrait);
                });
    }
}
