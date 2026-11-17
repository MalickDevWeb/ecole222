package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
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
    private UserJpaRepository comptesMock;

    @BeforeEach
    void preparer() {
        UserJpaRepository comptes = Mockito.mock(UserJpaRepository.class);
        comptesMock = comptes;
        awa = new UserJpaEntity();
        awa.setId("u-awa");
        awa.setEmail("awa.diop@exemple.sn");
        awa.setPrenom("Awa");
        awa.setPassword(bcrypt.encode("Ancien-2026"));
        awa.setDoitChangerMotDePasse(true);
        Mockito.when(comptes.findByEmail("awa.diop@exemple.sn")).thenReturn(Optional.of(awa));
        Mockito.when(comptes.findByEmail("personne@exemple.sn")).thenReturn(Optional.empty());
        UserJpaEntity eleveSansEmail = new UserJpaEntity();   // APP-TL-23 : compte existant, identifiant technique
        eleveSansEmail.setId("u-eleve");
        eleveSansEmail.setEmail("etu-2026-abcdef@sans-email.invalid");
        Mockito.when(comptes.findByEmail("etu-2026-abcdef@sans-email.invalid")).thenReturn(Optional.of(eleveSansEmail));
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

        // L'école du lien est l'école d'APPARTENANCE du compte (Awa : membre de ecole-221)
        OrganizationUserRepositoryPort appartenances = Mockito.mock(OrganizationUserRepositoryPort.class);
        OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
        Organization ecole221 = OrganizationFactory.create("École 221", OrganizationType.ECOLE, OrganizationStatus.ACTIVE, "ecole-221",
                "SN", "Dakar", null, null, "contact@ecole221.sn", null, null, null, null, null);
        Mockito.when(appartenances.findByUserId(anyString())).thenReturn(List.of());
        Mockito.when(appartenances.findByUserId("u-awa")).thenReturn(List.of(OrganizationUserFactory.create("u-awa", ecole221.getId(), "ETUDIANT")));
        Mockito.when(ecoles.findById(anyString())).thenReturn(Optional.empty());
        Mockito.when(ecoles.findById(ecole221.getId())).thenReturn(Optional.of(ecole221));
        service.setAppartenances(appartenances, ecoles);
    }

    /**
     * Revue DevSecOps : Host, X-Forwarded-Host, Origin pointant vers evil.example et X-Tenant-Slug d'une autre
     * école → lien sur l'adresse CONFIGURÉE, avec l'école d'appartenance du compte.
     */
    @Test
    void lienJamaisConstruitSurLaRequete() throws Exception {
        org.springframework.test.web.servlet.MockMvc mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new ecole_221.sn.ecole.modules.auth.infrastructure.web.ReinitialisationMotDePasseController(service)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/mot-de-passe-oublie")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"email\":\"awa.diop@exemple.sn\"}")
                        .header("Host", "evil.example").header("X-Forwarded-Host", "evil.example")
                        .header("Origin", "https://evil.example").header("Referer", "https://evil.example/t/ecole-b/login")
                        .header("X-Tenant-Slug", "ecole-b"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted());
        assertThat(emails).hasSize(1);
        assertThat(emails.get(0)).contains("https://ecole221.sn/t/ecole-221/reinitialiser-mot-de-passe?jeton=")
                .doesNotContain("evil").doesNotContain("ecole-b");
    }

    @Test
    void compteSansAppartenanceLienSansEcole() {
        UserJpaEntity sansEcole = new UserJpaEntity();
        sansEcole.setId("u-seul");
        sansEcole.setEmail("seul@exemple.sn");
        Mockito.when(comptesMock.findByEmail("seul@exemple.sn")).thenReturn(Optional.of(sansEcole));
        service.demander("seul@exemple.sn");
        assertThat(emails.get(0)).contains("https://ecole221.sn/reinitialiser-mot-de-passe?jeton=");
    }

    private String jetonDuDernierEmail() {
        Matcher m = Pattern.compile("jeton=([A-Za-z0-9_-]+)").matcher(emails.get(emails.size() - 1));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void lienEnvoyeJetonStockeHacheEtUtilisableUneSeuleFois() {
        service.demander(" Awa.Diop@Exemple.sn ");
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
    void eleveSansEmailAucunLienNiEnvoi() {
        // APP-TL-23 : identifiant technique sur le domaine réservé .invalid — rien n'est créé ni envoyé
        service.demander("ETU-2026-ABCDEF@sans-email.invalid");
        assertThat(emails).isEmpty();
        assertThat(stockees).isEmpty();
    }

    @Test
    void adresseInconnueMemeComportementSansEmail() {
        service.demander("personne@exemple.sn");   // aucune erreur : pas de fuite
        assertThat(emails).isEmpty();
        assertThat(stockees).isEmpty();
    }

    @Test
    void lienExpireApres30MinutesEtMotDePasseFaibleRefuseSansConsommerLeLien() {
        service.demander("awa.diop@exemple.sn");
        String jeton = jetonDuDernierEmail();
        // L'école du lien est celle du compte, sans en-tête
        assertThat(emails.get(0)).contains("https://ecole221.sn/t/ecole-221/reinitialiser-mot-de-passe?jeton=");
        refus(() -> service.reinitialiser(jeton, "court1"), HttpStatus.BAD_REQUEST, "caractères");   // Longueur selon le rôle
        horloge.t = horloge.t.plusSeconds(30 * 60);
        refus(() -> service.reinitialiser(jeton, "Nouveau-2026x"), HttpStatus.BAD_REQUEST, "plus valide");
        assertThat(bcrypt.matches("Ancien-2026", awa.getPassword())).isTrue();
    }

    @Test
    void nouvelleDemandeAnnuleLeLienPrecedent() {
        service.demander("awa.diop@exemple.sn");
        String premier = jetonDuDernierEmail();
        service.demander("awa.diop@exemple.sn");
        String second = jetonDuDernierEmail();
        refus(() -> service.reinitialiser(premier, "Nouveau-2026x"), HttpStatus.BAD_REQUEST, "plus valide");
        service.reinitialiser(second, "Nouveau-2026x");
        assertThat(bcrypt.matches("Nouveau-2026x", awa.getPassword())).isTrue();
    }

    @Test
    void limiteDeTroisDemandesParQuartDHeureMemePourUneAdresseInconnue() {
        for (int i = 0; i < 3; i++) service.demander("personne@exemple.sn");
        refus(() -> service.demander("personne@exemple.sn"), HttpStatus.TOO_MANY_REQUESTS, "Réessayez dans 15 minutes");
        horloge.t = horloge.t.plusSeconds(15 * 60 + 1);
        service.demander("personne@exemple.sn");
    }

    @Test
    void jetonInventeOuVideRefuse() {
        refus(() -> service.reinitialiser("n-importe-quoi", "Nouveau-2026x"), HttpStatus.BAD_REQUEST, "plus valide");
        refus(() -> service.reinitialiser("", "Nouveau-2026x"), HttpStatus.BAD_REQUEST, "plus valide");
        refus(() -> service.demander("awa@"), HttpStatus.BAD_REQUEST, "invalide");
    }

    private static void refus(Runnable r, HttpStatus statut, String extrait) {
        assertThatThrownBy(r::run).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(statut);
                    assertThat(((ResponseStatusException) e).getReason()).contains(extrait);
                });
    }
}
