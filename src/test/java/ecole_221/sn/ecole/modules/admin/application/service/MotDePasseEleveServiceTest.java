package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** APP-TL-23 (2026-11-14) : l'école remet un mot de passe provisoire à un élève, notamment sans e-mail. */
class MotDePasseEleveServiceTest {

    private final ComptesEcoleService comptes = mock(ComptesEcoleService.class);
    private final OrganizationUserJpaRepository appartenances = mock(OrganizationUserJpaRepository.class);
    private final UserJpaRepository utilisateurs = mock(UserJpaRepository.class);
    private final PasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private final MotDePasseEleveService service = new MotDePasseEleveService(comptes, appartenances, utilisateurs, encodeur);

    private UserJpaEntity eleve;

    private static OrganizationUserJpaEntity lien(String ecole, String statut) {
        OrganizationUserJpaEntity l = new OrganizationUserJpaEntity();
        l.setUserId("e1");
        l.setOrganizationId(ecole);
        l.setRole("ETUDIANT");
        l.setStatus(statut);
        return l;
    }

    @BeforeEach
    void bases() {
        eleve = new UserJpaEntity();
        eleve.setId("e1");
        eleve.setPrenom("Awa");
        eleve.setNom("Diop");
        eleve.setMatricule("ETU-2026-ABC123");
        eleve.setEmail("etu-2026-abc123@" + InscriptionAdmisService.DOMAINE_SANS_EMAIL);
        eleve.setPassword(encodeur.encode("ancien-mot-de-passe"));
        eleve.setRole("ETUDIANT");
        when(comptes.ecoleCourante()).thenReturn("org-a");
        when(comptes.membre("e1", "ETUDIANT")).thenReturn(eleve);
        when(comptes.appartenance("e1", "ETUDIANT")).thenReturn(Optional.of(lien("org-a", "ACTIVE")));
        when(comptes.membre("e-intrus", "ETUDIANT")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "introuvable"));
        when(appartenances.findByUserId("e1")).thenReturn(List.of(lien("org-a", "ACTIVE")));
        when(utilisateurs.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void eleveSansEmailRecoitUnMotDePasseProvisoireIdentifiantMatricule() {
        Map<String, Object> r = service.nouveauMotDePasseProvisoire("e1");
        String provisoire = (String) r.get("motDePasseProvisoire");
        assertThat(provisoire).hasSize(12);
        assertThat(r).containsEntry("identifiant", "ETU-2026-ABC123").containsEntry("sansEmail", true)
                .containsEntry("doitChangerMotDePasse", true).containsEntry("nom", "Awa Diop");
        // le nouveau mot de passe est enregistré haché, l'ancien ne fonctionne plus, le changement est imposé
        assertThat(encodeur.matches(provisoire, eleve.getPassword())).isTrue();
        assertThat(encodeur.matches("ancien-mot-de-passe", eleve.getPassword())).isFalse();
        assertThat(eleve.getPassword()).doesNotContain(provisoire);
        assertThat(eleve.getDoitChangerMotDePasse()).isTrue();
        verify(utilisateurs).save(eleve);
    }

    @Test
    void eleveAvecEmailIdentifiantEmail() {
        eleve.setEmail("awa.diop@exemple.sn");
        Map<String, Object> r = service.nouveauMotDePasseProvisoire("e1");
        assertThat(r).containsEntry("identifiant", "awa.diop@exemple.sn").containsEntry("sansEmail", false);
    }

    @Test
    void eleveDUneAutreEcoleIntrouvable() {
        assertThatThrownBy(() -> service.nouveauMotDePasseProvisoire("e-intrus"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(utilisateurs, never()).save(any());
    }

    @Test
    void compteActifDansUneAutreEcoleNonModifie() {
        when(appartenances.findByUserId("e1")).thenReturn(List.of(lien("org-a", "ACTIVE"), lien("org-b", "ACTIVE")));
        String avant = eleve.getPassword();
        assertThatThrownBy(() -> service.nouveauMotDePasseProvisoire("e1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(eleve.getPassword()).isEqualTo(avant);
        verify(utilisateurs, never()).save(any());
    }

    @Test
    void eleveTransfereNonModifie() {
        when(comptes.appartenance("e1", "ETUDIANT")).thenReturn(Optional.of(lien("org-a", "TRANSFERRED")));
        assertThatThrownBy(() -> service.nouveauMotDePasseProvisoire("e1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(utilisateurs, never()).save(any());
    }
}
