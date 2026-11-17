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
        // I3 (revue DevSecOps) : sans école, refus (par exemple /api/sync traité sans école).
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        if (ecole == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement.");
        }
        CourseJpaEntity c = cours.findById(courseId)
                .filter(x -> x.getOrganizationId() == null || ecole.equals(x.getOrganizationId()))
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

    /** Classes de l'école (facultatif : absent des tests unitaires construits sans Spring). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository classes;

    void setClasses(ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository c) {
        this.classes = c;
    }

    /**
     * APP-TL-05 : la classe existe dans l'école de la requête (sinon 404, comme une classe inexistante :
     * on ne révèle pas les classes d'une autre école) et le professeur connecté y enseigne au moins un cours
     * (sinon 403) ; l'administration de l'école y a accès.
     */
    public void verifierClasse(String classeId) {
        if (classes == null || classeId == null || classes.findById(classeId).isEmpty()) {   // @TenantId : école courante
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable dans cet établissement.");
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth != null && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ROLES_ADMIN::contains);
        if (admin) return;
        String moi = auth != null && auth.getPrincipal() instanceof User u ? u.getId() : null;
        if (moi == null || cours.findByClasse(classeId).stream().noneMatch(c -> moi.equals(c.getProfId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'enseignez dans aucun cours de cette classe.");
        }
    }

    /** L'élève est membre (élève) de l'école de la requête. */
    public void verifierEleve(String studentId) {
        if (comptesEcole.appartenance(studentId, "ETUDIANT").isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable dans cet établissement.");
        }
    }
}
