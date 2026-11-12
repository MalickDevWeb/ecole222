package ecole_221.sn.ecole.modules.planning.infrastructure.persistence;

import ecole_221.sn.ecole.modules.planning.domain.model.Filiere;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Une filière enregistrée par l'administration (PUT /admin/planning/filieres) n'a pas forcément de niveau.
 * Avant : sa relecture levait « Le niveau ne peut pas être null » et toute la liste GET /planning/filieres
 * répondait 500 pour l'école.
 */
class FiliereSansNiveauTest {

    @Test
    void filiereSansNiveauRelueAvecUnNiveauVideJamaisInvente() {
        FiliereJpaEntity e = new FiliereJpaEntity();
        e.setId("fil-1");
        e.setName("Informatique");
        e.setCode("INF");
        Filiere f = PlanningRepositoryAdapter.toFiliere(e);
        assertThat(f.getName()).isEqualTo("Informatique");
        assertThat(f.getLevel()).isEmpty();
    }
}
