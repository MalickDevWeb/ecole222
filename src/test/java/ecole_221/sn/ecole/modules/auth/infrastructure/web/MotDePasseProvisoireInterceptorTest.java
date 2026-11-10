package ecole_221.sn.ecole.modules.auth.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot E : tant que le mot de passe provisoire n'est pas changé, seules quelques routes restent ouvertes. */
class MotDePasseProvisoireInterceptorTest {

    private final MotDePasseProvisoireInterceptor interceptor = new MotDePasseProvisoireInterceptor();

    @AfterEach
    void nettoyer() { SecurityContextHolder.clearContext(); }

    private void connecter(boolean provisoire) {
        User u = UserFactory.create("Diop", "Awa", "awa@test.sn", "x", RoleUtilisateur.fromString("PROFESSEUR"));
        u.setDoitChangerMotDePasse(provisoire);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    private MockHttpServletRequest requete(String methode, String chemin) {
        MockHttpServletRequest r = new MockHttpServletRequest(methode, "/api" + chemin);
        r.setContextPath("/api");
        return r;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/professor/courses", "/admin/students", "/student/grades", "/auth/register"})
    void les_routes_metier_sont_refusees_en_403(String chemin) throws Exception {
        connecter(true);
        MockHttpServletResponse rep = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(requete("GET", chemin), rep, new Object())).isFalse();
        assertThat(rep.getStatus()).isEqualTo(403);
        assertThat(rep.getContentAsString()).contains("Changez votre mot de passe provisoire").contains("MOT_DE_PASSE_PROVISOIRE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/changer-mot-de-passe", "/auth/logout", "/auth/me", "/auth/me/"})
    void changement_deconnexion_et_profil_restent_permis(String chemin) throws Exception {
        connecter(true);
        assertThat(interceptor.preHandle(requete("POST", chemin), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void un_compte_sans_drapeau_n_est_pas_bloque() throws Exception {
        connecter(false);
        assertThat(interceptor.preHandle(requete("GET", "/professor/courses"), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void une_requete_anonyme_passe_aux_regles_de_securite() throws Exception {
        assertThat(interceptor.preHandle(requete("GET", "/professor/courses"), new MockHttpServletResponse(), new Object())).isTrue();
    }
}
