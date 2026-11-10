package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ModuleJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Set;

/**
 * Contrôle d'accès UNIQUE de l'espace professeur, appliqué avant toute route contenant {courseId},
 * {moduleId} ou {studentId}.
 *
 * <p>Test du parcours professeur du 2026-11-10 : un professeur pouvait noter, créer devoirs / leçons /
 * modules / rappels et faire l'appel dans le cours d'un COLLÈGUE, et un professeur d'une AUTRE ÉCOLE
 * pouvait noter un élève de l'école A ; on pouvait aussi noter un élève inexistant.</p>
 * <ul>
 *   <li>le cours (ou le cours du module) doit exister DANS l'école de la requête, sinon 404 ;</li>
 *   <li>un professeur n'agit que sur SES cours (403) ; l'administration de l'école agit sur tous ;</li>
 *   <li>l'élève visé doit être membre de l'école (404).</li>
 * </ul>
 */
@Component
public class AccesCoursInterceptor implements HandlerInterceptor {

    private static final Set<String> ROLES_ADMIN = Set.of("ROLE_ADMIN", "ROLE_ADMINISTRATOR", "ROLE_SUPER_ADMIN");

    private final CourseJpaRepository cours;
    private final ModuleJpaRepository modules;
    private final ComptesEcoleService comptesEcole;

    public AccesCoursInterceptor(CourseJpaRepository cours, ModuleJpaRepository modules, ComptesEcoleService comptesEcole) {
        this.cours = cours;
        this.modules = modules;
        this.comptesEcole = comptesEcole;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        @SuppressWarnings("unchecked")
        Map<String, String> variables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables == null || variables.isEmpty()) return true;

        String courseId = variables.get("courseId");
        String moduleId = variables.get("moduleId");
        if (moduleId != null) {
            courseId = modules.findById(moduleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Module introuvable dans cet établissement."))
                    .getCourseId();
        }
        if (courseId != null) {
            verifierCours(courseId);
        }
        String studentId = variables.get("studentId");
        if (studentId != null && courseId != null) {
            verifierEleve(studentId);
        }
        return true;
    }

    /** Le cours existe dans l'école de la requête et appartient au professeur connecté (sauf administration). */
    public CourseJpaEntity verifierCours(String courseId) {
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        CourseJpaEntity c = cours.findById(courseId)
                .filter(x -> ecole == null || x.getOrganizationId() == null || ecole.equals(x.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement."));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth != null && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ROLES_ADMIN::contains);
        if (!admin) {
            String moi = auth != null && auth.getPrincipal() instanceof User u ? u.getId() : null;
            if (moi == null || !moi.equals(c.getProfId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce cours n'est pas attribué à votre compte.");
            }
        }
        return c;
    }

    /** L'élève est membre (élève) de l'école de la requête. */
    public void verifierEleve(String studentId) {
        if (comptesEcole.appartenance(studentId, "ETUDIANT").isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable dans cet établissement.");
        }
    }
}
