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

class CoordonneesEcoleServiceTest {

    private final OrganizationRepositoryPort ecoles = Mockito.mock(OrganizationRepositoryPort.class);
    private final Organization ecole = Mockito.mock(Organization.class);
    private final CoordonneesEcoleService service = new CoordonneesEcoleService(ecoles);
    private final Map<String, String> fiche = new HashMap<>(Map.of("name", "École 221", "email", "contact@ecole221.sn"));

    @BeforeEach
    void preparer() {
        Mockito.when(ecole.getId()).thenReturn("org-1");
        Mockito.when(ecole.getName()).thenAnswer(i -> fiche.get("name"));
        Mockito.when(ecole.getEmail()).thenAnswer(i -> fiche.get("email"));
        Mockito.when(ecole.getPhone()).thenAnswer(i -> fiche.get("phone"));
        Mockito.when(ecole.getAddress()).thenAnswer(i -> fiche.get("address"));
        Mockito.doAnswer(i -> { fiche.put("name", i.getArgument(0)); return null; }).when(ecole).setName(Mockito.any());
        Mockito.doAnswer(i -> { fiche.put("email", i.getArgument(0)); return null; }).when(ecole).setEmail(Mockito.any());
        Mockito.doAnswer(i -> { fiche.put("phone", i.getArgument(0)); return null; }).when(ecole).setPhone(Mockito.any());
        Mockito.doAnswer(i -> { fiche.put("address", i.getArgument(0)); return null; }).when(ecole).setAddress(Mockito.any());
        Mockito.when(ecoles.findById("org-1")).thenReturn(Optional.of(ecole));
        Mockito.when(ecoles.save(ecole)).thenReturn(ecole);
        TenantContext.setCurrentOrganization(ecole);
    }

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
    }

    @Test
    void seulsLesChampsEnvoyesChangentEtLeVideEfface() {
        Map<String, Object> r = service.enregistrer(Map.of("phone", " +221 33 800 00 00 ", "address", "Rue 10, Dakar"));
        assertThat(r).containsEntry("phone", "+221 33 800 00 00").containsEntry("address", "Rue 10, Dakar")
                .containsEntry("name", "École 221").containsEntry("email", "contact@ecole221.sn");
        service.enregistrer(Map.of("address", "  "));
        assertThat(fiche.get("address")).isNull();
    }

    @Test
    void erreursHumainesEtValeursAbsurdesRefusees() {
        assertThatThrownBy(() -> service.enregistrer(Map.of())).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucune donnée");
        assertThatThrownBy(() -> service.enregistrer(Map.of("slogan", "x"))).hasMessageContaining("Aucun champ reconnu");
        assertThatThrownBy(() -> service.enregistrer(Map.of("name", ""))).hasMessageContaining("ne peut pas être effacé");
        assertThatThrownBy(() -> service.enregistrer(Map.of("name", "12345"))).hasMessageContaining("des lettres");
        assertThatThrownBy(() -> service.enregistrer(Map.of("name", 42))).hasMessageContaining("doit être un texte");
        assertThatThrownBy(() -> service.enregistrer(Map.of("email", "pas-un-email"))).hasMessageContaining("n'est pas valide");
        assertThatThrownBy(() -> service.enregistrer(Map.of("phone", "abc"))).hasMessageContaining("téléphone n'est pas valide");
        assertThatThrownBy(() -> service.enregistrer(Map.of("website", "javascript:alert(1)"))).hasMessageContaining("http");
        assertThatThrownBy(() -> service.enregistrer(Map.of("address", "x".repeat(301)))).hasMessageContaining("300");
        assertThat(fiche).containsEntry("name", "École 221").containsEntry("email", "contact@ecole221.sn");
    }

    @Test
    void emailDejaPrisParUneAutreEcoleRefuse() {
        Mockito.when(ecoles.existsByEmail("autre@ecole.sn")).thenReturn(true);
        assertThatThrownBy(() -> service.enregistrer(Map.of("email", "Autre@Ecole.sn")))
                .hasMessageContaining("déjà utilisé par un autre établissement");
        Mockito.when(ecoles.existsByEmail("contact@ecole221.sn")).thenReturn(true);
        assertThat(service.enregistrer(Map.of("email", "CONTACT@ecole221.sn"))).containsEntry("email", "contact@ecole221.sn");
    }
}
