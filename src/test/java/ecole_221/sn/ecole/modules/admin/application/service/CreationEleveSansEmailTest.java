package ecole_221.sn.ecole.modules.admin.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Inscription directe d'un élève par l'administration (2026-11-15) : e-mail OU « sans e-mail » (connexion par
 * matricule, APP-TL-23), même règle que l'inscription des admis.
 */
class CreationEleveSansEmailTest {

    private UserJpaRepository comptes;
    private ComptesEcoleService service;

    @BeforeEach
    void setUp() {
        comptes = mock(UserJpaRepository.class);
        when(comptes.findFirstByMatricule(any())).thenReturn(Optional.empty());
        service = new ComptesEcoleService(comptes, mock(OrganizationUserJpaRepository.class), new BCryptPasswordEncoder(4));
        TenantContext.setCurrentOrganization(OrganizationFactory.create("Lycée A", OrganizationType.ECOLE, "a@exemple.sn"));
    }

    @AfterEach
    void nettoyer() { TenantContext.clear(); }

    private static HttpStatus statut(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    @Test
    void sans_email_connexion_par_matricule_avec_identifiant_technique_reserve() {
        var cree = service.creerEleve("Moussa", "Ba", null, true, null, null, null);
        UserJpaEntity u = cree.compte();
        assertThat(u.getMatricule()).startsWith("ETU-");
        assertThat(u.getEmail()).isEqualTo(u.getMatricule().toLowerCase() + "@sans-email.invalid");
        assertThat(InscriptionAdmisService.sansEmail(u.getEmail())).isTrue();
        assertThat(u.getRole()).isEqualTo("ETUDIANT");
        assertThat(cree.motDePasseProvisoire()).isNotBlank();
        assertThat(u.getDoitChangerMotDePasse()).isTrue();
    }

    @Test
    void sans_email_avec_matricule_saisi() {
        var cree = service.creerEleve("Moussa", "Ba", "  ", true, null, "MAT-2026-007", null);
        assertThat(cree.compte().getMatricule()).isEqualTo("MAT-2026-007");
        assertThat(cree.compte().getEmail()).isEqualTo("mat-2026-007@sans-email.invalid");
    }

    @Test
    void avec_email_comme_avant() {
        var cree = service.creerEleve("Awa", "Diop", "Awa.Diop@Exemple.sn", false, null, null, null);
        assertThat(cree.compte().getEmail()).isEqualTo("awa.diop@exemple.sn");
        assertThat(InscriptionAdmisService.sansEmail(cree.compte().getEmail())).isFalse();
    }

    @Test
    void ni_email_ni_sans_email_400() {
        assertThatThrownBy(() -> service.creerEleve("Awa", "Diop", null, false, null, null, null))
                .satisfies(t -> assertThat(statut(t)).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("sans e-mail");
        verify(comptes, never()).save(any());
    }

    @Test
    void email_et_sans_email_a_la_fois_400() {
        assertThatThrownBy(() -> service.creerEleve("Awa", "Diop", "awa@exemple.sn", true, null, null, null))
                .satisfies(t -> assertThat(statut(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(comptes, never()).save(any());
    }

    @Test
    void identifiant_technique_saisi_comme_email_400() {
        assertThatThrownBy(() -> service.creerEleve("Awa", "Diop", "x@sans-email.invalid", false, null, null, null))
                .satisfies(t -> assertThat(statut(t)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void matricule_pris_409_avec_le_bon_message() {
        when(comptes.findFirstByMatricule("MAT-1")).thenReturn(Optional.of(new UserJpaEntity()));
        assertThatThrownBy(() -> service.creerEleve("Moussa", "Ba", null, true, null, "MAT-1", null))
                .satisfies(t -> assertThat(statut(t)).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("matricule");
        verify(comptes, never()).save(any());
    }

    @Test
    void email_pris_409() {
        when(comptes.existsByEmail("awa@exemple.sn")).thenReturn(true);
        assertThatThrownBy(() -> service.creerEleve("Awa", "Diop", "awa@exemple.sn", false, null, null, null))
                .satisfies(t -> assertThat(statut(t)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void le_booleen_json_sans_email_arrive_en_texte_dans_le_corps_de_la_route() throws Exception {
        // POST /admin/students lit un Map<String, String> : `"sansEmail": true` doit y arriver en « true »
        Map<String, String> corps = new ObjectMapper().readValue("{\"name\":\"Moussa Ba\",\"sansEmail\":true}",
                new TypeReference<Map<String, String>>() { });
        assertThat(corps.get("sansEmail")).isEqualTo("true");
    }
}
