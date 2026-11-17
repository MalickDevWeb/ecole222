package ecole_221.sn.ecole.modules.security.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.TerminalAccesJpaEntity;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.TerminalAccesJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class TerminauxAccesServiceTest {

    private final TerminalAccesJpaRepository depot = Mockito.mock(TerminalAccesJpaRepository.class);
    private final ComptesEcoleService comptes = Mockito.mock(ComptesEcoleService.class);
    private final TerminauxAccesService service = new TerminauxAccesService(depot, comptes);
    private final List<TerminalAccesJpaEntity> base = new ArrayList<>();

    @BeforeEach
    void preparer() {
        Mockito.when(depot.save(any())).thenAnswer(i -> {
            TerminalAccesJpaEntity t = i.getArgument(0);
            base.removeIf(x -> x.getId().equals(t.getId()));
            base.add(t);
            return t;
        });
        Mockito.when(depot.findById(anyString())).thenAnswer(i -> base.stream().filter(t -> t.getId().equals(i.getArgument(0))).findFirst());
        Mockito.when(depot.findByVigileId(anyString())).thenAnswer(i -> base.stream().filter(t -> i.getArgument(0).equals(t.getVigileId())).toList());
        Mockito.when(depot.existsByNomIgnoreCase(anyString())).thenAnswer(i -> base.stream().anyMatch(t -> t.getNom().equalsIgnoreCase(i.getArgument(0))));
        UserJpaEntity vigile = Mockito.mock(UserJpaEntity.class);
        Mockito.when(vigile.getPrenom()).thenReturn("Modou");
        Mockito.when(vigile.getNom()).thenReturn("Faye");
        Mockito.when(comptes.membre(eq("vigile-1"), eq("VIGIL"))).thenReturn(vigile);
        Mockito.when(comptes.membre(eq("eleve-1"), eq("VIGIL")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable dans cet établissement."));
    }

    private String creer(String nom) {
        return (String) service.creer(Map.of("name", nom, "location", "Hall central")).get("id");
    }

    @Test
    void creationValideeEtNomUnique() {
        String id = creer("Portail Principal");
        assertThat(base).singleElement().satisfies(t -> assertThat(t.getStatut()).isEqualTo("Actif"));
        assertThatThrownBy(() -> creer("portail principal")).hasMessageContaining("porte déjà le nom");
        assertThatThrownBy(() -> service.creer(Map.of("name", 42))).hasMessageContaining("doit être un texte");
        assertThatThrownBy(() -> service.creer(Map.of())).hasMessageContaining("nom du terminal est obligatoire");
        assertThat(id).startsWith("term-");
    }

    @Test
    void scanDuVigileRefuseQuandSonTerminalEstDesactiveOuRevoque() {
        String id = creer("Portail Principal");
        service.affecterVigile(id, Map.of("vigilId", "vigile-1"));
        assertThat(service.exigerTerminalUtilisable("vigile-1")).contains("Portail Principal");

        service.changerStatut(id, Map.of("status", "Inactif"));
        assertThatThrownBy(() -> service.exigerTerminalUtilisable("vigile-1")).hasMessageContaining("désactivé");

        service.revoquer(id);
        assertThatThrownBy(() -> service.exigerTerminalUtilisable("vigile-1")).hasMessageContaining("révoqué");
        assertThatThrownBy(() -> service.changerStatut(id, Map.of("status", "Actif"))).hasMessageContaining("ne peut plus être modifié");
        assertThatThrownBy(() -> service.revoquer(id)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.exigerTerminalUtilisable("sans-terminal")).isEmpty();
    }

    @Test
    void seulUnVigileDeLEcolePeutEtreAffecteEtIlNAQuUnTerminal() {
        String a = creer("Portail A");
        String b = creer("Portail B");
        assertThatThrownBy(() -> service.affecterVigile(a, Map.of("vigilId", "eleve-1"))).hasMessageContaining("introuvable");
        service.affecterVigile(a, Map.of("vigilId", "vigile-1"));
        Map<String, Object> vueB = service.affecterVigile(b, Map.of("vigilId", "vigile-1"));

        assertThat(vueB).containsEntry("vigilName", "Modou Faye");
        assertThat(base.stream().filter(t -> "vigile-1".equals(t.getVigileId()))).hasSize(1);
        assertThatThrownBy(() -> service.changerStatut(a, Map.of("status", "Supprimé"))).hasMessageContaining("Actif");
        assertThatThrownBy(() -> service.changerStatut("inconnu", Map.of("status", "Actif"))).hasMessageContaining("introuvable");
        assertThat(Optional.ofNullable(service.lister())).isPresent();
    }
}
