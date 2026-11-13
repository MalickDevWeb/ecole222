package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentiteVisuelleServiceTest {

    private final OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
    private final Organization ecole = Mockito.mock(Organization.class);
    private final IdentiteVisuelleService service = new IdentiteVisuelleService(ecoles);
    private Map<String, Object> reglages = new HashMap<>();
    private String logo;

    @BeforeEach
    void preparer() {
        Mockito.when(ecole.getId()).thenReturn("org-1");
        Mockito.when(ecole.getSettings()).thenAnswer(i -> reglages);
        Mockito.doAnswer(i -> { reglages = i.getArgument(0); return null; }).when(ecole).setSettings(Mockito.any());
        Mockito.when(ecole.getLogo()).thenAnswer(i -> logo);
        Mockito.doAnswer(i -> { logo = i.getArgument(0); return null; }).when(ecole).setLogo(Mockito.any());
        Mockito.when(ecoles.findById("org-1")).thenReturn(Optional.of(ecole));
        Mockito.when(ecoles.save(ecole)).thenReturn(ecole);
        Mockito.when(ecole.getName()).thenReturn("École");
        TenantContext.setCurrentOrganization(ecole);
    }

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
    }

    @Test
    void logoCouleurEtFondEnregistresPourLEcoleCourante() {
        Map<String, Object> r = service.enregistrer(Map.of("logoUrl", " https://res.cloudinary.com/e/logo.png ",
                "primaryColor", "#b3181c", "backgroundUrl", "https://res.cloudinary.com/e/fond.jpg"));
        assertThat(r).containsEntry("logoUrl", "https://res.cloudinary.com/e/logo.png").containsEntry("primaryColor", "#B3181C")
                .containsEntry("backgroundUrl", "https://res.cloudinary.com/e/fond.jpg");
        service.enregistrer(Map.of("backgroundUrl", ""));
        assertThat(reglages).doesNotContainKey("backgroundUrl").containsEntry("primaryColor", "#B3181C");
    }

    @Test
    void valeursInvalidesRefuseesAvecMessage() {
        assertThatThrownBy(() -> service.enregistrer(Map.of("logoUrl", "data:image/png;base64,AAAA")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Lien du logo invalide");
        assertThatThrownBy(() -> service.enregistrer(Map.of("logoUrl", "javascript:alert(1)")))
                .hasMessageContaining("invalide");
        assertThatThrownBy(() -> service.enregistrer(Map.of("primaryColor", "rouge")))
                .hasMessageContaining("#RRGGBB");
        assertThatThrownBy(() -> service.enregistrer(Map.of())).hasMessageContaining("Aucune modification");
        assertThat(logo).isNull();
    }
}
