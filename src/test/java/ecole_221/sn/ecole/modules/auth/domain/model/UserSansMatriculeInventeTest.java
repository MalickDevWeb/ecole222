package ecole_221.sn.ecole.modules.auth.domain.model;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** APP-ARC-07 (A6) : un compte sans matricule n'en reçoit pas un inventé (« 2026-EC-001 » pour tous). */
class UserSansMatriculeInventeTest {

    @Test
    void compteSansMatriculeResteSansMatricule() {
        User u = UserFactory.create("Diop", "Awa", "awa@exemple.sn", "x", RoleUtilisateur.PARENT);
        assertThat(u.getMatricule()).isNull();
    }
}
