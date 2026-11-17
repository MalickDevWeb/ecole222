package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.application.dto.SchoolRegistrationRequest;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.shared.error.AppException;
import ecole_221.sn.ecole.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** Défaut F3 (15/11) : slug fourni à l'inscription publique validé côté serveur (400 / 409). */
class SlugInscriptionPubliqueTest {

    private final OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
    private final OrganizationApplicationService service;

    SlugInscriptionPubliqueTest() {
        UserRepositoryPort comptes = Mockito.mock(UserRepositoryPort.class);
        PasswordEncoder encodeur = Mockito.mock(PasswordEncoder.class);
        Mockito.when(encodeur.encode(any())).thenReturn("$2a$12$empreinte");
        Mockito.when(ecoles.existsBySlug(anyString())).thenAnswer(i -> "lycee-pris".equals(i.getArgument(0)));
        Mockito.when(ecoles.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new OrganizationApplicationService(ecoles, comptes, Mockito.mock(OrganizationUserRepositoryPort.class), encodeur);
    }

    private SchoolRegistrationRequest demande(String slug) {
        SchoolRegistrationRequest r = new SchoolRegistrationRequest();
        r.setSchoolName("Lycée de Thiès");
        r.setSlug(slug);
        r.setAdminEmail("direction@lycee-thies.sn");
        r.setAdminPassword("Inscription-2026");
        return r;
    }

    private ErrorCode code(String slug) {
        try {
            service.registerSchool(demande(slug));
            return null;
        } catch (AppException e) {
            return e.getErrorCode();
        }
    }

    @Test
    void slugInvalideOuReserve400() {
        for (String s : new String[]{"lycée-thiès", "ab", "a".repeat(41), "lycee_thies", "lycee thies", "-lycee", "lycee--thies",
                "www", "admin", "ecole-221", "xn--lycee", "ecole-221-dakar", "ecole221-paiements"}) {
            assertThat(code(s)).as(s).isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
        assertThatThrownBy(() -> service.registerSchool(demande("www"))).hasMessageContaining("réservé");
    }

    @Test
    void slugDejaPris409EtSlugValideAccepte() {
        assertThat(code("lycee-pris")).isEqualTo(ErrorCode.TENANT_SLUG_ALREADY_EXISTS);
        assertThat(code("Lycee-De-Thies")).as("majuscules ramenées en minuscules").isNull();
        assertThat(service.registerSchool(demande(null)).getSlug()).as("sans slug : depuis le nom").isEqualTo("lycee-de-thies");
    }

    @Test
    void sansSlugUnNomReserveEstRefuseJamaisSuffixe() {
        SchoolRegistrationRequest r = demande(null);
        r.setSchoolName("École 221");
        assertThatThrownBy(() -> service.registerSchool(r)).isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR))
                .hasMessageContaining("réservé");
    }
}
