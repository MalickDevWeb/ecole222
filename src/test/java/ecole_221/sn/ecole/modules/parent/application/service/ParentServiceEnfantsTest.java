package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.parent.application.port.dto.ChildDto;
import ecole_221.sn.ecole.modules.parent.domain.model.ParentEnfant;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** APP-BE-19 (sécurité) : un parent ne voit que ses enfants réels — plus d'enfant « usr-001 » par défaut. */
class ParentServiceEnfantsTest {

    private final ParentRepositoryPort parents = Mockito.mock(ParentRepositoryPort.class);
    private final UserRepositoryPort comptes = Mockito.mock(UserRepositoryPort.class);
    private final ParentService service =
            new ParentService(parents, Mockito.mock(AppointmentRepositoryPort.class), comptes);

    @Test
    void parentSansEnfantNeRecoitAucunEnfant() {
        Mockito.when(parents.findParentEnfantsByParentId("p1")).thenReturn(List.of());
        assertTrue(service.getEnfantsByParentId("p1").isEmpty());
    }

    @Test
    void enfantReelAvecSonVraiNom() {
        Mockito.when(parents.findParentEnfantsByParentId("p1"))
                .thenReturn(List.of(ParentEnfant.create("p1", "e42", "MERE")));
        User eleve = Mockito.mock(User.class);
        Mockito.when(eleve.getNom()).thenReturn("Ba");
        Mockito.when(eleve.getPrenom()).thenReturn("Fatou");
        Mockito.when(eleve.getMatricule()).thenReturn("ETU-42");
        Mockito.when(comptes.findById("e42")).thenReturn(Optional.of(eleve));

        List<ChildDto> enfants = service.getEnfantsByParentId("p1");
        assertEquals(1, enfants.size());
        assertEquals("e42", enfants.get(0).id());
        assertEquals("Ba", enfants.get(0).nom());
        assertEquals("Fatou", enfants.get(0).prenom());
        assertEquals("ETU-42", enfants.get(0).matricule());
        assertTrue(enfants.stream().noneMatch(e -> "usr-001".equals(e.id()) || "Aminata".equals(e.prenom())));
    }
}
