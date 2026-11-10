package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ModuleJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test du parcours professeur du 2026-11-10 : notes et devoirs posés dans le cours d'un collègue / d'une autre école. */
class AccesCoursInterceptorTest {

    private final CourseJpaRepository cours = Mockito.mock(CourseJpaRepository.class);
    private final ComptesEcoleService comptes = Mockito.mock(ComptesEcoleService.class);
    private final AccesCoursInterceptor intercepteur =
            new AccesCoursInterceptor(cours, Mockito.mock(ModuleJpaRepository.class), comptes);

    private static void connecter(String id, RoleUtilisateur role) {
        User u = UserFactory.reconstructSimple(id, "N", "P", id + "@ex.sn", null, "M", role, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    private static MockHttpServletRequest requete(Map<String, String> variables) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, variables);
        return r;
    }

    @AfterEach
    void fin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leCoursDuCollegueEstInterdit() {
        connecter("prof-a", RoleUtilisateur.PROFESSEUR);
        Mockito.when(cours.findById("c-b")).thenReturn(Optional.of(new CourseJpaEntity("c-b", "Réseaux", "L3", 30, "prof-b")));
        var e = assertThrows(ResponseStatusException.class,
                () -> intercepteur.preHandle(requete(Map.of("courseId", "c-b")), new MockHttpServletResponse(), null));
        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    void unCoursHorsEcoleEstIntrouvable() {
        connecter("prof-a", RoleUtilisateur.PROFESSEUR);
        Mockito.when(cours.findById("c-x")).thenReturn(Optional.empty());   // filtré par @TenantId
        var e = assertThrows(ResponseStatusException.class,
                () -> intercepteur.preHandle(requete(Map.of("courseId", "c-x")), new MockHttpServletResponse(), null));
        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void noterUnEleveInexistantEstRefuse() {
        connecter("prof-a", RoleUtilisateur.PROFESSEUR);
        Mockito.when(cours.findById("c-a")).thenReturn(Optional.of(new CourseJpaEntity("c-a", "Algo", "L3", 30, "prof-a")));
        Mockito.when(comptes.appartenance("fantome", "ETUDIANT")).thenReturn(Optional.empty());
        var e = assertThrows(ResponseStatusException.class, () -> intercepteur.preHandle(
                requete(Map.of("courseId", "c-a", "studentId", "fantome")), new MockHttpServletResponse(), null));
        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void sonPropreCoursEtSonEleveSontAutorises() throws Exception {
        connecter("prof-a", RoleUtilisateur.PROFESSEUR);
        Mockito.when(cours.findById("c-a")).thenReturn(Optional.of(new CourseJpaEntity("c-a", "Algo", "L3", 30, "prof-a")));
        Mockito.when(comptes.appartenance("e1", "ETUDIANT")).thenReturn(Optional.of(new OrganizationUserJpaEntity()));
        assertTrue(intercepteur.preHandle(requete(Map.of("courseId", "c-a", "studentId", "e1")), new MockHttpServletResponse(), null));
    }
}
