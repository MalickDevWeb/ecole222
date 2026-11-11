package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService.CompteCree;
import ecole_221.sn.ecole.modules.admin.application.service.ImportElevesService.LigneImport;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Import groupé : vérification ligne par ligne, tout ou rien, aucune donnée inventée. */
class ImportElevesServiceTest {

    private ComptesEcoleService comptesEcole;
    private InscriptionClasseJpaRepository inscriptions;
    private ImportElevesService service;

    @BeforeEach
    void preparer() {
        comptesEcole = mock(ComptesEcoleService.class);
        when(comptesEcole.creer(any(), any(), anyString(), any(), any(), eq("ETUDIANT"), isNull())).thenAnswer(i -> {
            UserJpaEntity u = mock(UserJpaEntity.class);
            when(u.getId()).thenReturn("usr-" + i.getArgument(2));
            when(u.getMatricule()).thenReturn("ETU-" + i.getArgument(2));
            return new CompteCree(u, "Provisoire-123");
        });
        UserJpaRepository comptes = mock(UserJpaRepository.class);
        when(comptes.existsByEmail("deja@exemple.sn")).thenReturn(true);
        when(comptes.findFirstByMatricule(anyString())).thenReturn(Optional.empty());
        ClasseJpaRepository classes = mock(ClasseJpaRepository.class);
        ClasseJpaEntity l1 = mock(ClasseJpaEntity.class);
        when(l1.getId()).thenReturn("cls-1");
        when(l1.getName()).thenReturn("L1 Informatique");
        when(classes.findAll()).thenReturn(List.of(l1));
        inscriptions = mock(InscriptionClasseJpaRepository.class);
        service = new ImportElevesService(comptesEcole, comptes, classes, inscriptions);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lignes(Map<String, Object> r) {
        return (List<Map<String, Object>>) r.get("lignes");
    }

    @Test
    void importValideCreeLesComptesEtAffecteLaClasse() {
        Map<String, Object> r = service.importer(List.of(
                new LigneImport(2, "Awa Diop", "Awa.Diop@Exemple.sn", null, null, "  l1   informatique "),
                new LigneImport(3, "Modou Fall", "modou@exemple.sn", "2026-INF-002", "770001122", null)), false);

        assertThat(r.get("valide")).isEqualTo(true);
        assertThat(r.get("crees")).isEqualTo(2);
        assertThat(lignes(r).get(0)).containsEntry("statut", "CREE").containsEntry("classe", "L1 Informatique")
                .containsEntry("motDePasseProvisoire", "Provisoire-123");
        verify(comptesEcole).creer("Awa", "Diop", "Awa.Diop@Exemple.sn", null, null, "ETUDIANT", null);
        verify(inscriptions, times(1)).save(any());   // seule la 1re ligne a une classe
    }

    @Test
    void uneLigneEnErreurEtRienNestCree() {
        Map<String, Object> r = service.importer(List.of(
                new LigneImport(2, "Awa Diop", "awa@exemple.sn", null, null, null),
                new LigneImport(3, "Awa Bis", "AWA@exemple.sn", null, null, null),
                new LigneImport(4, " ", "deja@exemple.sn", null, null, "Terminale S"),
                new LigneImport(5, "Sans Email", null, null, null, null),
                new LigneImport(6, "Mal Ecrit", "pas-un-email", null, null, null)), false);

        assertThat(r.get("valide")).isEqualTo(false);
        assertThat(r.get("crees")).isEqualTo(0);
        assertThat(r.get("erreurs")).isEqualTo(4L);
        List<Map<String, Object>> l = lignes(r);
        assertThat(l.get(0).get("statut")).isEqualTo("OK");
        assertThat((List<String>) l.get(1).get("erreurs")).containsExactly("email « awa@exemple.sn » en double dans le fichier");
        assertThat((List<String>) l.get(2).get("erreurs")).containsExactly("nom manquant",
                "email « deja@exemple.sn » déjà utilisé par un compte", "classe « Terminale S » inconnue dans l'école");
        assertThat((List<String>) l.get(3).get("erreurs")).containsExactly("email manquant (il sert d'identifiant de connexion)");
        assertThat((List<String>) l.get(4).get("erreurs")).containsExactly("email « pas-un-email » invalide");
        verifyNoInteractions(inscriptions);
        verify(comptesEcole, never()).creer(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void simulationNEcritJamaisRien() {
        Map<String, Object> r = service.importer(List.of(new LigneImport(2, "Awa Diop", "awa@exemple.sn", null, null, null)), true);
        assertThat(r.get("valide")).isEqualTo(true);
        assertThat(r.get("crees")).isEqualTo(0);
        assertThat(lignes(r).get(0).get("statut")).isEqualTo("OK");
        verify(comptesEcole, never()).creer(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void fichierVideOuTropGrosRefuse() {
        assertThatThrownBy(() -> service.importer(List.of(), true)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("aucun élève");
        List<LigneImport> trop = new ArrayList<>();
        for (int i = 0; i < 501; i++) trop.add(new LigneImport(i, "E " + i, "e" + i + "@x.sn", null, null, null));
        assertThatThrownBy(() -> service.importer(trop, true)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("501 élèves ; 500 au plus");
    }
}
