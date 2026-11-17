package ecole_221.sn.ecole.modules.collaboration.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MembreGroupeJpaEntity;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MembreGroupeJpaRepository;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.WorkgroupJpaEntity;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.WorkgroupJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
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

class MembresGroupeServiceTest {

    private final MembreGroupeJpaRepository membres = Mockito.mock(MembreGroupeJpaRepository.class);
    private final WorkgroupJpaRepository groupes = Mockito.mock(WorkgroupJpaRepository.class);
    private final OrganizationUserRepositoryPort appartenances = Mockito.mock(OrganizationUserRepositoryPort.class);
    private final UserRepositoryPort comptes = Mockito.mock(UserRepositoryPort.class);
    private final ClasseJpaRepository classes = Mockito.mock(ClasseJpaRepository.class);
    private final InscriptionClasseJpaRepository inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
    private final MembresGroupeService service = new MembresGroupeService(membres, groupes, appartenances, comptes, classes, inscriptions);
    private final List<MembreGroupeJpaEntity> enBase = new ArrayList<>();
    private final User prof = utilisateur("prof-1", "Awa", "Ndiaye");
    private final User eleveA = utilisateur("el-a", "Fatou", "Sow");
    private final User eleveB = utilisateur("el-b", "Moussa", "Diop");

    @BeforeEach
    void preparer() {
        Organization ecole = Mockito.mock(Organization.class);
        Mockito.when(ecole.getId()).thenReturn("org-1");
        TenantContext.setCurrentOrganization(ecole);
        eleve("el-a", true); eleve("el-b", true); eleve("el-inactif", false);
        OrganizationUser p = membre("PROFESSEUR", true);
        Mockito.when(appartenances.findByUserIdAndOrganizationId("prof-1", "org-1")).thenReturn(Optional.of(p));
        Mockito.when(classes.existsById("cls-1")).thenReturn(true);
        Mockito.when(inscriptions.findByClasseId("cls-1")).thenReturn(List.of(new InscriptionClasseJpaEntity("i1", "el-a", "cls-1")));
        Mockito.when(membres.save(Mockito.any())).thenAnswer(i -> { if (!enBase.contains(i.getArgument(0))) enBase.add(i.getArgument(0)); return i.getArgument(0); });
        Mockito.when(membres.findByGroupeIdOrderByAjouteLeAsc("g-1")).thenAnswer(i -> enBase);
        WorkgroupJpaEntity g = new WorkgroupJpaEntity("g-1", "Exposé", null, "prof-1", 1);
        Mockito.when(groupes.findById("g-1")).thenReturn(Optional.of(g));
    }

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void enseignantComposeLeGroupeEtLeChefEstUnMembre() {
        connecter(prof, "ROLE_PROFESSEUR");
        var c = service.preparer(new HashMap<>(Map.of("memberIds", List.of("el-a", "el-b", "el-a"), "leaderId", "el-b")), "Exposé", prof);
        assertThat(c.membres()).containsExactly("el-a", "el-b");   // doublon coché deux fois : compté une fois
        assertThat(c.chefId()).isEqualTo("el-b");
        service.enregistrer("g-1", c);
        assertThat(enBase).extracting(MembreGroupeJpaEntity::getUtilisateurId).containsExactly("el-a", "el-b");
        assertThat(groupes.findById("g-1").get().getMembersCount()).isEqualTo(2);
        // sans chef désigné : le premier membre
        assertThat(service.preparer(new HashMap<>(Map.of("memberIds", List.of("el-a"))), "Autre", prof).chefId()).isEqualTo("el-a");
    }

    @Test
    void compositionsAbsurdesRefuseesAvantToutecreation() {
        connecter(prof, "ROLE_PROFESSEUR");
        Mockito.when(groupes.existsByNameIgnoreCase("Exposé")).thenReturn(true);
        assertThatThrownBy(() -> service.preparer(new HashMap<>(), "Exposé", prof)).hasMessageContaining("porte déjà le nom");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("memberIds", "el-a")), "G", prof)).hasMessageContaining("liste");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("memberIds", List.of(42))), "G", prof)).hasMessageContaining("invalide");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("memberIds", List.of("prof-1"))), "G", prof)).hasMessageContaining("pas un élève actif");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("memberIds", List.of("el-inactif"))), "G", prof)).hasMessageContaining("pas un élève actif");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("memberIds", List.of("inconnu"))), "G", prof)).hasMessageContaining("pas un élève actif");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("memberIds", List.of("el-a"), "leaderId", "el-b")), "G", prof))
                .hasMessageContaining("l'un des membres");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("classId", "cls-x")), "G", prof)).hasMessageContaining("Classe inconnue");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("classId", "cls-1", "memberIds", List.of("el-b"))), "G", prof))
                .hasMessageContaining("Moussa Diop n'est pas inscrit(e) dans la classe");
        List<String> trop = new ArrayList<>();
        for (int i = 0; i < 61; i++) trop.add("x" + i);
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("memberIds", trop)), "G", prof)).hasMessageContaining("60");
    }

    @Test
    void eleveCreateurSeulMembreEtChefSansPouvoirChoisirLesAutres() {
        connecter(eleveA, "ROLE_ETUDIANT");
        var c = service.preparer(new HashMap<>(), "Mon groupe", eleveA);
        assertThat(c.membres()).containsExactly("el-a");
        assertThat(c.chefId()).isEqualTo("el-a");
        assertThatThrownBy(() -> service.preparer(new HashMap<>(Map.of("memberIds", List.of("el-b"))), "G", eleveA))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Seul un enseignant");
    }

    @Test
    void changementDeChefReserveAuChefActuelEtALEncadrement() {
        enBase.add(new MembreGroupeJpaEntity("m1", "g-1", "el-a", true));
        enBase.add(new MembreGroupeJpaEntity("m2", "g-1", "el-b", false));
        connecter(eleveB, "ROLE_ETUDIANT");
        assertThatThrownBy(() -> service.changerChef("g-1", Map.of("leaderId", "el-b"), eleveB)).hasMessageContaining("chef actuel");
        connecter(eleveA, "ROLE_ETUDIANT");
        assertThatThrownBy(() -> service.changerChef("g-1", Map.of("leaderId", "prof-1"), eleveA)).hasMessageContaining("membre du groupe");
        Mockito.when(comptes.findById("el-b")).thenReturn(Optional.of(eleveB));
        Mockito.when(comptes.findById("el-a")).thenReturn(Optional.of(eleveA));
        Map<String, Object> r = service.changerChef("g-1", Map.of("leaderId", "el-b"), eleveA);
        assertThat(r).containsEntry("leaderId", "el-b").containsEntry("leaderName", "Moussa Diop");
        assertThat(enBase).filteredOn(MembreGroupeJpaEntity::isChef).extracting(MembreGroupeJpaEntity::getUtilisateurId).containsExactly("el-b");
        assertThatThrownBy(() -> service.changerChef("g-inconnu", Map.of("leaderId", "el-b"), eleveA)).hasMessageContaining("introuvable");
    }

    private void eleve(String id, boolean actif) {
        OrganizationUser m = membre("ETUDIANT", actif);
        Mockito.when(appartenances.findByUserIdAndOrganizationId(id, "org-1")).thenReturn(Optional.of(m));
        Mockito.when(comptes.findById(id)).thenAnswer(i -> Optional.ofNullable(switch (id) {
            case "el-a" -> eleveA; case "el-b" -> eleveB; default -> null; }));
    }

    private static OrganizationUser membre(String role, boolean actif) {
        OrganizationUser m = Mockito.mock(OrganizationUser.class);
        Mockito.when(m.getRole()).thenReturn(role);
        Mockito.when(m.isActive()).thenReturn(actif);
        return m;
    }

    private static User utilisateur(String id, String prenom, String nom) {
        User u = Mockito.mock(User.class);
        Mockito.when(u.getId()).thenReturn(id);
        Mockito.when(u.getPrenom()).thenReturn(prenom);
        Mockito.when(u.getNom()).thenReturn(nom);
        return u;
    }

    private static void connecter(User u, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority(role))));
    }
}
