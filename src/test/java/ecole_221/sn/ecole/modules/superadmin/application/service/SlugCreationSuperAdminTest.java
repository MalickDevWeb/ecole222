package ecole_221.sn.ecole.modules.superadmin.application.service;

import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.superadmin.application.port.command.CreateTenantCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** Défaut F2 (15/11) : slug fabriqué par le super-administrateur — accents retirés, suffixe -2, -3… prévisible. */
class SlugCreationSuperAdminTest {

    private String slugCree(String nom, Set<String> pris) {
        OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
        UserRepositoryPort comptes = Mockito.mock(UserRepositoryPort.class);
        PasswordEncoder encodeur = Mockito.mock(PasswordEncoder.class);
        Mockito.when(encodeur.encode(any())).thenReturn("$2a$12$empreinte");
        Mockito.when(comptes.findByEmail(anyString())).thenReturn(Optional.empty());
        Mockito.when(ecoles.existsBySlug(anyString())).thenAnswer(i -> pris.contains(i.<String>getArgument(0)));
        Mockito.when(ecoles.save(any())).thenAnswer(i -> i.getArgument(0));
        new TenantService(ecoles, comptes, encodeur, Mockito.mock(OrganizationUserRepositoryPort.class))
                .createTenant(new CreateTenantCommand(nom, "contact@lycee-thies.sn", "SN", "Thiès", null, null, null, null,
                        "Ndiaye", "Awa", "awa@lycee-thies.sn", null));
        ArgumentCaptor<Organization> capture = ArgumentCaptor.forClass(Organization.class);
        Mockito.verify(ecoles).save(capture.capture());
        return capture.getValue().getSlug();
    }

    @Test
    void accentsRetiresEtCollisionSuffixePrevisible() {
        assertThat(slugCree("Lycée de Thiès", Set.of())).isEqualTo("lycee-de-thies");
        assertThat(slugCree("Lycée de Thiès", Set.of("lycee-de-thies"))).isEqualTo("lycee-de-thies-2");
        assertThat(slugCree("Lycée de Thiès", Set.of("lycee-de-thies", "lycee-de-thies-2"))).isEqualTo("lycee-de-thies-3");
    }

    @Test
    void nomDonnantUnIdentifiantReserveRefuse400() {
        for (String nom : new String[]{"Admin", "École 221", "Ecole 221 Dakar", "École221 Paiements"}) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> slugCree(nom, Set.of())).as(nom)
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .satisfies(e -> assertThat(((org.springframework.web.server.ResponseStatusException) e).getStatusCode().value()).isEqualTo(400))
                    .hasMessageContaining("réservé");
        }
    }
}
