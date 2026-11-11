package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.parent.application.port.dto.ParentProfileDto;
import ecole_221.sn.ecole.modules.parent.domain.model.Parent;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Profil du parent : les coordonnées sont réellement enregistrées (avant : faux succès). */
class ParentServiceProfilTest {

    private ParentRepositoryPort parents;
    private UserRepositoryPort comptes;
    private ParentService service;

    @BeforeEach
    void preparer() {
        parents = Mockito.mock(ParentRepositoryPort.class);
        comptes = Mockito.mock(UserRepositoryPort.class);
        User compte = Mockito.mock(User.class);
        when(compte.getEmail()).thenReturn("fatou.ndiaye@exemple.sn");
        when(compte.getNom()).thenReturn("Ndiaye");
        when(compte.getPrenom()).thenReturn("Fatou");
        when(comptes.findById("u-parent")).thenReturn(Optional.of(compte));
        when(parents.saveParent(any())).thenAnswer(i -> i.getArgument(0));
        service = new ParentService(parents, Mockito.mock(AppointmentRepositoryPort.class), comptes);
    }

    @Test
    void premiereModificationCreeLaFicheAvecLEmailDuCompte() {
        when(parents.findParentByUserId("u-parent")).thenReturn(Optional.empty());

        ParentProfileDto r = service.updateParentProfile("u-parent",
                new ParentProfileDto("autre@pirate.sn", " +221 77 123 45 67 ", "Sicap Liberté 6, Dakar", "Comptable"));

        ArgumentCaptor<Parent> fiche = ArgumentCaptor.forClass(Parent.class);
        verify(parents).saveParent(fiche.capture());
        assertThat(fiche.getValue().getUserId()).isEqualTo("u-parent");
        assertThat(fiche.getValue().getNom()).isEqualTo("Ndiaye");
        assertThat(r.email()).isEqualTo("fatou.ndiaye@exemple.sn");   // l'email de connexion ne change pas ici
        assertThat(r.telephone()).isEqualTo("+221 77 123 45 67");
        assertThat(r.adresse()).isEqualTo("Sicap Liberté 6, Dakar");
        assertThat(r.profession()).isEqualTo("Comptable");
    }

    @Test
    void modificationSuivanteGardeLaMemeFiche() {
        LocalDateTime cree = LocalDateTime.of(2026, 10, 1, 9, 0);
        when(parents.findParentByUserId("u-parent")).thenReturn(Optional.of(Parent.reconstitute("fiche-1", "u-parent",
                "Ndiaye", "Fatou", "fatou.ndiaye@exemple.sn", "770000000", "Dakar", "Comptable", cree, cree)));

        service.updateParentProfile("u-parent", new ParentProfileDto(null, "781112233", "  ", "Enseignante"));

        ArgumentCaptor<Parent> fiche = ArgumentCaptor.forClass(Parent.class);
        verify(parents).saveParent(fiche.capture());
        assertThat(fiche.getValue().getId()).isEqualTo("fiche-1");
        assertThat(fiche.getValue().getCreatedAt()).isEqualTo(cree);
        assertThat(fiche.getValue().getTelephone()).isEqualTo("781112233");
        assertThat(fiche.getValue().getAdresse()).isNull();   // champ vidé = effacé
        assertThat(fiche.getValue().getProfession()).isEqualTo("Enseignante");
    }

    @Test
    void telephoneInvalideRefuseSansRienEnregistrer() {
        assertThatThrownBy(() -> service.updateParentProfile("u-parent",
                new ParentProfileDto(null, "appelez-moi", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appelez-moi").hasMessageContaining("pas valide");
        verify(parents, never()).saveParent(any());
    }

    @Test
    void profilSansFicheNInventeRien() {
        when(parents.findParentByUserId("u-parent")).thenReturn(Optional.empty());
        ParentProfileDto p = service.getParentProfile("u-parent");
        assertThat(p.email()).isEqualTo("fatou.ndiaye@exemple.sn");
        assertThat(p.telephone()).isNull();
        assertThat(p.adresse()).isNull();
        assertThat(p.profession()).isNull();
    }
}
