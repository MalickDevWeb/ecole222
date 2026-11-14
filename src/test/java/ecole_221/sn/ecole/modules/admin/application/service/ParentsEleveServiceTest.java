package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** APP-BE-27 (2026-11-14) : l'administration crée ou rattache le compte d'un parent et le lie à l'élève. */
class ParentsEleveServiceTest {

    private final ComptesEcoleService comptes = mock(ComptesEcoleService.class);
    private final UserJpaRepository utilisateurs = mock(UserJpaRepository.class);
    private final ParentEnfantJpaRepository liens = mock(ParentEnfantJpaRepository.class);
    private final ParentsEleveService service = new ParentsEleveService(comptes, utilisateurs, liens);

    private static UserJpaEntity compte(String id, String role) {
        UserJpaEntity u = new UserJpaEntity();
        u.setId(id);
        u.setPrenom("Fatou");
        u.setNom("Sow");
        u.setEmail(id + "@exemple.sn");
        u.setRole(role);
        return u;
    }

    private static ParentEnfantJpaEntity lien(String parent, String enfant) {
        ParentEnfantJpaEntity l = new ParentEnfantJpaEntity();
        l.setParentId(parent);
        l.setEnfantId(enfant);
        l.setRelation("MERE");
        return l;
    }

    @BeforeEach
    void bases() {
        when(comptes.membre("e-intrus", "ETUDIANT")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "introuvable"));
        when(liens.findByEnfantId("e1")).thenReturn(new ArrayList<>());
        when(liens.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void parentInconnuCompteCreeAvecMotDePasseProvisoireEtLie() {
        when(utilisateurs.findByEmail("fatou.sow@exemple.sn")).thenReturn(Optional.empty());
        UserJpaEntity cree = compte("p-new", "PARENT");
        when(comptes.creer(eq("Fatou"), eq("Sow"), eq("fatou.sow@exemple.sn"), eq("+221 77 000 00 00"), isNull(), eq("PARENT"), isNull()))
                .thenReturn(new ComptesEcoleService.CompteCree(cree, "Prov-1234"));
        Map<String, Object> r = service.lier("e1", Map.of("email", "Fatou.Sow@exemple.sn", "nom", "Fatou Sow",
                "telephone", "+221 77 000 00 00", "relation", "MERE"));
        assertThat(r).containsEntry("parentId", "p-new").containsEntry("compteCree", true)
                .containsEntry("motDePasseProvisoire", "Prov-1234").containsEntry("relationLibelle", "Mère");
        ArgumentCaptor<ParentEnfantJpaEntity> l = ArgumentCaptor.forClass(ParentEnfantJpaEntity.class);
        verify(liens).save(l.capture());
        assertThat(l.getValue().getParentId()).isEqualTo("p-new");
        assertThat(l.getValue().getEnfantId()).isEqualTo("e1");
        assertThat(l.getValue().getRelation()).isEqualTo("MERE");
    }

    @Test
    void parentExistantRattacheSansNouveauCompteNiMotDePasse() {
        UserJpaEntity p = compte("p-autre-ecole", "PARENT");
        when(utilisateurs.findByEmail("p-autre-ecole@exemple.sn")).thenReturn(Optional.of(p));
        Map<String, Object> r = service.lier("e1", Map.of("email", "p-autre-ecole@exemple.sn", "relation", "PERE"));
        verify(comptes).rattacher(p, "PARENT");
        verify(comptes, never()).creer(any(), any(), any(), any(), any(), any(), any());
        assertThat(r).containsEntry("compteCree", false).doesNotContainKey("motDePasseProvisoire");
    }

    @Test
    void adresseDUnCompteNonParentRefuseeSansRevelerSonRole() {
        when(utilisateurs.findByEmail("prof@exemple.sn")).thenReturn(Optional.of(compte("prof", "PROFESSEUR")));
        assertThatThrownBy(() -> service.lier("e1", Map.of("email", "prof@exemple.sn", "relation", "PERE")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409")
                .hasMessageContaining("pas un compte parent").satisfies(e -> assertThat(e.getMessage()).doesNotContain("PROFESSEUR"));
        verify(liens, never()).save(any());
        verify(comptes, never()).rattacher(any(), any());
    }

    @Test
    void dejaLieOuQuatreParentsDejaLiesRefuse() {
        UserJpaEntity p = compte("p1", "PARENT");
        when(utilisateurs.findByEmail("p1@exemple.sn")).thenReturn(Optional.of(p));
        when(liens.findByEnfantId("e1")).thenReturn(List.of(lien("p1", "e1")));
        assertThatThrownBy(() -> service.lier("e1", Map.of("email", "p1@exemple.sn", "relation", "MERE")))
                .hasMessageContaining("déjà lié");
        when(liens.findByEnfantId("e1")).thenReturn(List.of(lien("a", "e1"), lien("b", "e1"), lien("c", "e1"), lien("d", "e1")));
        assertThatThrownBy(() -> service.lier("e1", Map.of("email", "p1@exemple.sn", "relation", "MERE")))
                .hasMessageContaining("409");
        verify(liens, never()).save(any());
    }

    @Test
    void champsInvalidesEtEleveDUneAutreEcoleRefuses() {
        assertThatThrownBy(() -> service.lier("e1", Map.of("email", "x@exemple.sn", "relation", "COUSIN")))
                .hasMessageContaining("400");
        assertThatThrownBy(() -> service.lier("e1", Map.of("relation", "PERE"))).hasMessageContaining("400");
        when(utilisateurs.findByEmail("nouveau@exemple.sn")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lier("e1", Map.of("email", "nouveau@exemple.sn", "relation", "PERE")))
                .hasMessageContaining("nom");   // compte à créer : le nom est obligatoire
        assertThatThrownBy(() -> service.lier("e-intrus", Map.of("email", "x@exemple.sn", "relation", "PERE")))
                .hasMessageContaining("404");
        assertThatThrownBy(() -> service.parents("e-intrus")).hasMessageContaining("404");
        verify(liens, never()).save(any());
    }

    @Test
    void delierRetireLeLienJamaisLeCompte() {
        ParentEnfantJpaEntity l = lien("p1", "e1");
        when(liens.findByParentIdAndEnfantId("p1", "e1")).thenReturn(Optional.of(l));
        service.delier("e1", "p1");
        verify(liens).delete(l);
        verify(utilisateurs, never()).delete(any());
        when(liens.findByParentIdAndEnfantId("p9", "e1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delier("e1", "p9")).hasMessageContaining("404");
    }
}
