package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.auth.application.service.ReinitialisationMotDePasseService;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.ReinitialisationMotDePasseJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.ReinitialisationMotDePasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.email.domain.port.EmailServicePort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

/** Liens d'accès émis par l'école (15/11) : jamais de mot de passe dans un message, accord d'un second administrateur. */
class LienAccesServiceTest {

    private final List<ReinitialisationMotDePasseJpaEntity> stockes = new ArrayList<>();
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
    private final UserJpaEntity parent = compte("u-parent", "Fatou", "parent-221771234567@sans-email.invalid", "+221771234567");
    private final UserJpaEntity directeur = compte("u-dir", "Moussa", "moussa@exemple.sn", "+221781234567");
    private LienAccesService service;
    private ReinitialisationMotDePasseService reinit;

    private static UserJpaEntity compte(String id, String prenom, String email, String tel) {
        UserJpaEntity u = new UserJpaEntity();
        u.setId(id);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setTelephone(tel);
        u.setRole("PARENT");
        return u;
    }

    private static OrganizationUserJpaEntity membre(String userId, String role) {
        OrganizationUserJpaEntity m = new OrganizationUserJpaEntity();
        m.setUserId(userId);
        m.setOrganizationId("org-1");
        m.setRole(role);
        m.setStatus("ACTIVE");
        return m;
    }

    @BeforeEach
    void preparer() {
        directeur.setRole("DIRECTEUR");
        ComptesEcoleService comptes = Mockito.mock(ComptesEcoleService.class);
        Mockito.when(comptes.ecoleCourante()).thenReturn("org-1");
        Mockito.when(comptes.appartenance(anyString(), any())).thenReturn(Optional.empty());
        Mockito.when(comptes.appartenance(eq("u-parent"), isNull())).thenReturn(Optional.of(membre("u-parent", "PARENT")));
        Mockito.when(comptes.appartenance(eq("u-dir"), isNull())).thenReturn(Optional.of(membre("u-dir", "DIRECTEUR")));
        Mockito.when(comptes.membre("u-parent", "PARENT")).thenReturn(parent);
        Mockito.when(comptes.membre("u-dir", "DIRECTEUR")).thenReturn(directeur);

        ReinitialisationMotDePasseJpaRepository liens = Mockito.mock(ReinitialisationMotDePasseJpaRepository.class);
        Mockito.when(liens.save(any())).thenAnswer(i -> {
            ReinitialisationMotDePasseJpaEntity l = i.getArgument(0);
            if (!stockes.contains(l)) stockes.add(l);
            return l;
        });
        Mockito.when(liens.findById(anyString())).thenAnswer(i -> stockes.stream().filter(l -> l.getId().equals(i.getArgument(0))).findFirst());
        Mockito.when(liens.findByJetonHache(anyString())).thenAnswer(i ->
                stockes.stream().filter(l -> l.getJetonHache().equals(i.getArgument(0))).findFirst());
        Mockito.when(liens.findByUserIdAndUtiliseLeIsNull(anyString())).thenAnswer(i ->
                stockes.stream().filter(l -> l.getUserId().equals(i.getArgument(0)) && l.getUtiliseLe() == null).toList());

        OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
        Mockito.when(ecoles.findById(anyString())).thenReturn(Optional.empty());
        EmailServicePort email = Mockito.mock(EmailServicePort.class);
        service = new LienAccesService(comptes, liens, ecoles, email, bcrypt, "https://ecole221.sn");

        UserJpaRepository utilisateurs = Mockito.mock(UserJpaRepository.class);
        Mockito.when(utilisateurs.findById("u-parent")).thenReturn(Optional.of(parent));
        Mockito.when(utilisateurs.findById("u-dir")).thenReturn(Optional.of(directeur));
        reinit = new ReinitialisationMotDePasseService(utilisateurs, liens, bcrypt, email, "https://ecole221.sn");
        connecter("u-admin-1");
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of()));
    }

    private static String jeton(Map<String, Object> r) {
        Matcher m = Pattern.compile("jeton=([A-Za-z0-9_-]+)").matcher(URLDecoder.decode(r.get("adresseEnvoi").toString(), StandardCharsets.UTF_8));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void parentSansEmailRecoitUnLienWhatsAppSansMotDePasse() {
        Map<String, Object> r = service.emettre("u-parent", Map.of("canal", "whatsapp"));
        assertThat(r).containsEntry("type", "ACTIVATION").containsEntry("accordRequis", false)
                .containsEntry("destinataire", "+221 77 ••• •• 67").doesNotContainKey("codeOral");
        assertThat(r.get("adresseEnvoi").toString()).startsWith("https://wa.me/221771234567?text=");
        assertThat(r.get("message").toString()).contains("valable 72 h").doesNotContainIgnoringCase("mot de passe :");
        reinit.reinitialiser(jeton(r), "le baobab fleurit en mai");
        assertThat(bcrypt.matches("le baobab fleurit en mai", parent.getPassword())).isTrue();
    }

    @Test
    void unNouveauLienAnnuleLePrecedent() {
        String premier = jeton(service.emettre("u-parent", Map.of("canal", "SMS")));
        String second = jeton(service.emettre("u-parent", Map.of("canal", "SMS")));
        assertThatThrownBy(() -> reinit.reinitialiser(premier, "le baobab fleurit en mai"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getReason()).contains("plus valide"));
        reinit.reinitialiser(second, "le baobab fleurit en mai");
    }

    @Test
    void emailRefusePourUnIdentifiantTechnique() {
        assertThatThrownBy(() -> service.emettre("u-parent", Map.of("canal", "EMAIL")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void numeroModifieRecemmentDemandeUnAccord() {
        parent.setTelephoneModifieLe(LocalDateTime.now().minusHours(2));
        Map<String, Object> r = service.emettre("u-parent", Map.of("canal", "WHATSAPP"));
        assertThat(r).containsEntry("accordRequis", true).doesNotContainKey("adresseEnvoi").doesNotContainKey("message");
    }

    @Test
    void comptePrivilegieAccordDUnSecondAdministrateurEtCodeOral() {
        directeur.setLastLoginAt(LocalDateTime.now().minusDays(3));
        Map<String, Object> demande = service.emettre("u-dir", Map.of("canal", "WHATSAPP"));
        assertThat(demande).containsEntry("accordRequis", true).containsEntry("type", "REINITIALISATION").doesNotContainKey("adresseEnvoi");
        String lienId = demande.get("lienId").toString();

        // le demandeur ne peut pas s'accorder lui-même
        assertThatThrownBy(() -> service.accorder(lienId))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        connecter("u-admin-2");
        Map<String, Object> r = service.accorder(lienId);
        String code = r.get("codeOral").toString();
        assertThat(code).matches("\\d{6}");
        assertThat(r.get("message").toString()).contains("valable 2 h").doesNotContain(code);
        String jeton = jeton(r);
        assertThatThrownBy(() -> service.accorder(lienId)).isInstanceOf(ResponseStatusException.class);   // une seule fois

        assertThatThrownBy(() -> reinit.reinitialiser(jeton, "le baobab fleurit en mai et juin", null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getReason()).contains("code de 6 chiffres"));
        String faux = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> reinit.reinitialiser(jeton, "le baobab fleurit en mai et juin", faux))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getReason()).contains("Code incorrect"));
        }
        reinit.reinitialiser(jeton, "le baobab fleurit en mai et juin", code);
        assertThat(bcrypt.matches("le baobab fleurit en mai et juin", directeur.getPassword())).isTrue();
    }

    @Test
    void cinqCodesErronesAnnulentLeLien() {
        Map<String, Object> demande = service.emettre("u-dir", Map.of("canal", "SMS"));
        connecter("u-admin-2");
        Map<String, Object> r = service.accorder(demande.get("lienId").toString());
        String code = r.get("codeOral").toString();
        String jeton = jeton(r);
        String faux = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> reinit.reinitialiser(jeton, "le baobab fleurit en mai et juin", faux)).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> reinit.reinitialiser(jeton, "le baobab fleurit en mai et juin", faux))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getReason()).contains("annulé"));
        assertThatThrownBy(() -> reinit.reinitialiser(jeton, "le baobab fleurit en mai et juin", code))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getReason()).contains("plus valide"));
    }

    @Test
    void lienEnAttenteDAccordInutilisable() {
        service.emettre("u-dir", Map.of("canal", "SMS"));
        // le jeton n'est connu de personne avant l'accord ; un jeton quelconque est refusé
        assertThatThrownBy(() -> reinit.reinitialiser("jeton-invente", "le baobab fleurit en mai et juin", "123456"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getReason()).contains("plus valide"));
    }

    @Test
    void pasDeLienPourSonPropreCompte() {
        connecter("u-parent");
        assertThatThrownBy(() -> service.emettre("u-parent", Map.of("canal", "SMS")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
