package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Lot E : seul un mot de passe GÉNÉRÉ impose le changement à la première connexion ; un mot de passe choisi par
 * l'administration ne l'impose pas. La réponse de connexion porte le drapeau.
 */
class MotDePasseProvisoireALaCreationTest {

    private final BCryptPasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private UserJpaRepository comptes;
    private ComptesEcoleService service;

    @BeforeEach
    void setUp() {
        comptes = mock(UserJpaRepository.class);
        when(comptes.findFirstByMatricule(any())).thenReturn(Optional.empty());
        service = new ComptesEcoleService(comptes, mock(OrganizationUserJpaRepository.class), encodeur);
        TenantContext.setCurrentOrganization(OrganizationFactory.create("Lycée A", OrganizationType.ECOLE, "a@exemple.sn"));
    }

    @AfterEach
    void nettoyer() { TenantContext.clear(); }

    @Test
    void un_mot_de_passe_genere_impose_le_changement() {
        var cree = service.creer("Awa", "Diop", "awa.diop@exemple.sn", null, null, "PROFESSEUR", null);
        assertThat(cree.motDePasseProvisoire()).isNotBlank();
        assertThat(cree.compte().getDoitChangerMotDePasse()).isTrue();
    }

    @Test
    void un_mot_de_passe_choisi_par_l_administration_ne_l_impose_pas() {
        var cree = service.creer("Awa", "Diop", "awa.diop@exemple.sn", null, null, "PROFESSEUR", "ChoisiParAdmin1");
        assertThat(cree.motDePasseProvisoire()).isNull();
        assertThat(cree.compte().getDoitChangerMotDePasse()).isNull();
    }

    @Test
    void la_reponse_de_connexion_indique_le_drapeau() {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        JwtService jwt = mock(JwtService.class);
        OrganizationUserRepositoryPort memberships = mock(OrganizationUserRepositoryPort.class);
        when(memberships.findByUserId(any())).thenReturn(List.of());
        when(jwt.generateToken(any(User.class))).thenReturn("jwt");
        String mdp = "Provisoire" + System.nanoTime();
        User u = UserFactory.create("Diop", "Awa", "awa@ecole221.sn", encodeur.encode(mdp), RoleUtilisateur.fromString("PROFESSEUR"));
        u.setDoitChangerMotDePasse(true);
        when(users.findByEmail("awa@ecole221.sn")).thenReturn(Optional.of(u));
        LoginService login = new LoginService(users, jwt, encodeur, memberships, mock(OrganizationRepositoryPort.class));

        assertThat(login.login(new LoginCommand("awa@ecole221.sn", mdp)).isDoitChangerMotDePasse()).isTrue();
        u.setDoitChangerMotDePasse(null);
        assertThat(login.login(new LoginCommand("awa@ecole221.sn", mdp)).isDoitChangerMotDePasse()).isFalse();
    }

    @Test
    void le_drapeau_survit_a_l_aller_retour_entite_domaine() {
        UserJpaEntity e = new UserJpaEntity("usr-1", "Diop", "Awa", "awa@exemple.sn", "h", "PROFESSEUR", "PRF-1", null);
        e.setDoitChangerMotDePasse(true);
        var adapter = new ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserRepositoryAdapter(comptes);
        when(comptes.findById("usr-1")).thenReturn(Optional.of(e));
        when(comptes.save(any())).thenAnswer(i -> i.getArgument(0));
        User u = adapter.findById("usr-1").orElseThrow();
        assertThat(u.isDoitChangerMotDePasse()).isTrue();
        adapter.save(u);
        verify(comptes).save(argThat(x -> Boolean.TRUE.equals(x.getDoitChangerMotDePasse())));
    }
}
