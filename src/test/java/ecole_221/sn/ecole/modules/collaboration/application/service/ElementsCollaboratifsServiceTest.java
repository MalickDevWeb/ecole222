package ecole_221.sn.ecole.modules.collaboration.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.ElementCollaboratifJpaEntity;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.ElementCollaboratifJpaRepository;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.WorkgroupJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class ElementsCollaboratifsServiceTest {

    private final ElementCollaboratifJpaRepository depot = Mockito.mock(ElementCollaboratifJpaRepository.class);
    private final WorkgroupJpaRepository groupes = Mockito.mock(WorkgroupJpaRepository.class);
    private final ElementsCollaboratifsService service = new ElementsCollaboratifsService(depot, groupes);
    private final List<ElementCollaboratifJpaEntity> base = new ArrayList<>();

    @BeforeEach
    void preparer() {
        connecter("eleve-1", "Awa", "Diop", "ROLE_ETUDIANT");
        Mockito.when(depot.save(any())).thenAnswer(i -> {
            ElementCollaboratifJpaEntity e = i.getArgument(0);
            base.removeIf(x -> x.getId().equals(e.getId()));
            base.add(e);
            return e;
        });
        Mockito.when(depot.findByGenreAndCle(any(), any())).thenAnswer(i -> base.stream()
                .filter(e -> e.getGenre().equals(i.getArgument(0)) && e.getCle().equals(i.getArgument(1))).findFirst());
        Mockito.when(depot.findByGenreOrderByCreeLeAsc(any())).thenAnswer(i -> base.stream()
                .filter(e -> e.getGenre().equals(i.getArgument(0))).toList());
        Mockito.when(groupes.existsById("grp-1")).thenReturn(true);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String id, String prenom, String nom, String role) {
        User u = Mockito.mock(User.class);
        Mockito.when(u.getId()).thenReturn(id);
        Mockito.when(u.getPrenom()).thenReturn(prenom);
        Mockito.when(u.getNom()).thenReturn(nom);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority(role))));
    }

    private static Map<String, Object> corps(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void seulsLesChampsConnusSontGardesEtLAuteurEstLeCompteConnecte() {
        Map<String, Object> page = service.enregistrer(ElementsCollaboratifsService.PAGE,
                corps("id", "p1", "title", " Exposé ", "content", "Plan", "author", "Directeur", "__proto__", Map.of("admin", true)), null);

        assertThat(page).containsEntry("title", "Exposé").containsEntry("author", "Awa Diop").doesNotContainKey("__proto__");
    }

    @Test
    void valeursAbsurdesRefusees() {
        assertThatThrownBy(() -> service.enregistrer(ElementsCollaboratifsService.PAGE, corps("title", 42), null))
                .hasMessageContaining("doit être un texte");
        assertThatThrownBy(() -> service.enregistrer(ElementsCollaboratifsService.PAGE, corps("content", "x"), null))
                .hasMessageContaining("titre de la page est obligatoire");
        assertThatThrownBy(() -> service.enregistrer(ElementsCollaboratifsService.PRESENCE,
                corps("meetId", "m1", "durationMinutes", -999999999999L), null)).hasMessageContaining("entre 0 et 1440");
        assertThatThrownBy(() -> service.enregistrer(ElementsCollaboratifsService.NOTE, corps("title", "n", "isPrivate", "oui"), null))
                .hasMessageContaining("true / false");
        assertThatThrownBy(() -> service.enregistrer(ElementsCollaboratifsService.DEVOIR,
                corps("title", "Exposé", "deadline", "vendredi", "attachmentUrl", "javascript:alert(1)"), "grp-1"))
                .hasMessageContaining("http");
        assertThatThrownBy(() -> service.enregistrer(ElementsCollaboratifsService.DEVOIR,
                corps("title", "Exposé", "deadline", "vendredi"), "groupe-inconnu")).hasMessageContaining("introuvable");
        assertThatThrownBy(() -> service.enregistrer(ElementsCollaboratifsService.JOURNAL, Map.of(), null))
                .hasMessageContaining("Aucune donnée");
        assertThat(base).isEmpty();
    }

    @Test
    void noteRriveeLueSeulementParSonAuteurEtModifieeSeulementParLui() {
        service.enregistrer(ElementsCollaboratifsService.NOTE, corps("id", "n1", "title", "Brouillon", "isPrivate", true), null);
        service.enregistrer(ElementsCollaboratifsService.NOTE, corps("id", "n2", "title", "Partagée"), null);

        connecter("eleve-2", "Moussa", "Ba", "ROLE_ETUDIANT");
        assertThat(service.lister(ElementsCollaboratifsService.NOTE, null)).extracting(m -> m.get("title")).containsExactly("Partagée");
        assertThatThrownBy(() -> service.enregistrer(ElementsCollaboratifsService.NOTE, corps("id", "n2", "title", "Volée"), null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("auteur");
    }

    @Test
    void presencesDUnEleveInvisiblesPourUnAutreEleveMaisVuesParLEncadrement() {
        service.enregistrer(ElementsCollaboratifsService.PRESENCE, corps("meetId", "m1", "emailUsed", "awa@ecole.sn"), null);

        connecter("eleve-2", "Moussa", "Ba", "ROLE_ETUDIANT");
        assertThat(service.lister(ElementsCollaboratifsService.PRESENCE, null)).isEmpty();
        connecter("prof-1", "Fatou", "Sow", "ROLE_PROFESSEUR");
        assertThat(service.lister(ElementsCollaboratifsService.PRESENCE, null)).hasSize(1);
    }

    @Test
    void doubleEnregistrementDuMemeIdNeCreePasDeDoublon() {
        service.enregistrer(ElementsCollaboratifsService.JOURNAL, corps("id", "a1", "action", "Page créée"), null);
        service.enregistrer(ElementsCollaboratifsService.JOURNAL, corps("id", "a1", "action", "Page créée"), null);

        assertThat(base).hasSize(1);
        assertThat(Optional.of(base.get(0)).map(ElementCollaboratifJpaEntity::getAuteurId)).contains("eleve-1");
    }
}
