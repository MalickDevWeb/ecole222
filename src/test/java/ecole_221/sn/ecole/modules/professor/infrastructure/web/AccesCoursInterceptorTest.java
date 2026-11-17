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

/** Test du parcours professeur : notes et devoirs posés dans le cours d'un collègue / d'une autre école. */
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

    /** I3 : le contrôle exige une école de requête ; ces cours (sans école enregistrée) y sont. */
    @org.junit.jupiter.api.BeforeEach
    void ecoleDeLaRequete() {
        ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.setCurrentOrganization(
                ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory.create("École A",
                        ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType.ECOLE, "a@ex.sn"));
    }

    @AfterEach
    void fin() {
        SecurityContextHolder.clearContext();
        ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.clear();
    }

    @Test
    void sansEcoleDeRequeteLeCoursEstIntrouvable() {
        ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.clear();
        connecter("prof-a", RoleUtilisateur.PROFESSEUR);
        Mockito.when(cours.findById("c-a")).thenReturn(Optional.of(new CourseJpaEntity("c-a", "Réseaux", "L3", 30, "prof-a")));
        var e = assertThrows(ResponseStatusException.class,
                () -> intercepteur.preHandle(requete(Map.of("courseId", "c-a")), new MockHttpServletResponse(), null));
        assertEquals(404, e.getStatusCode().value());
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

    // APP-TL-05 : liste des élèves d'une classe côté professeur
    private ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository classesAvec(String... ids) {
        var repo = Mockito.mock(ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository.class);
        for (String id : ids) {
            Mockito.when(repo.findById(id)).thenReturn(Optional.of(new ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity()));
        }
        Mockito.when(repo.findById("cls-autre-ecole")).thenReturn(Optional.empty());   // filtrée par @TenantId
        return repo;
    }

    @Test
    void uneClasseHorsEcoleEstIntrouvableCommeUneClasseInexistante() {
        connecter("prof-a", RoleUtilisateur.PROFESSEUR);
        intercepteur.setClasses(classesAvec("cls-l1"));
        var e = assertThrows(ResponseStatusException.class, () -> intercepteur.verifierClasse("cls-autre-ecole"));
        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void uneClasseOuLeProfesseurNEnseignePasEstInterdite() {
        connecter("prof-a", RoleUtilisateur.PROFESSEUR);
        intercepteur.setClasses(classesAvec("cls-l1"));
        Mockito.when(cours.findByClasse("cls-l1")).thenReturn(java.util.List.of(new CourseJpaEntity("c-b", "Réseaux", "cls-l1", 30, "prof-b")));
        var e = assertThrows(ResponseStatusException.class, () -> intercepteur.verifierClasse("cls-l1"));
        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    void sesClassesEtCellesDeLAdministrationSontAccessibles() {
        intercepteur.setClasses(classesAvec("cls-l1"));
        Mockito.when(cours.findByClasse("cls-l1")).thenReturn(java.util.List.of(new CourseJpaEntity("c-a", "Algo", "cls-l1", 30, "prof-a")));
        connecter("prof-a", RoleUtilisateur.PROFESSEUR);
        intercepteur.verifierClasse("cls-l1");
        connecter("adm", RoleUtilisateur.ADMIN);
        intercepteur.verifierClasse("cls-l1");
    }
}
