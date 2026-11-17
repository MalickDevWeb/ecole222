package ecole_221.sn.ecole.modules.admin.rbac;

import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Application de la matrice des droits aux rôles du personnel.
 *
 * <p>Un Directeur, une Secrétaire, un Comptable, un Surveillant ou un RH n'atteint une route que si la permission du
 * module correspondant (lecture ou écriture) lui est accordée dans l'école de la requête. Une route qui n'appartient
 * à aucun module est refusée (refus par défaut). Les autres rôles ne sont pas concernés.</p>
 */
@Component
public class PermissionsInterceptor implements HandlerInterceptor {

    private final PermissionsEcoleService droits;

    public PermissionsInterceptor(PermissionsEcoleService droits) {
        this.droits = droits;
    }

    static String role() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5))
                .filter(CataloguePermissions.ROLES_PERSONNEL::contains).findFirst().orElse(null);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = role();
        if (role == null) return true;   // pas un rôle du personnel : règles habituelles
        String chemin = request.getServletPath() == null || request.getServletPath().isEmpty()
                ? request.getRequestURI().substring(request.getContextPath().length()) : request.getServletPath();
        String methode = request.getMethod();
        if ("OPTIONS".equals(methode) || CataloguePermissions.routeCommune(methode, chemin)) return true;
        String permission = CataloguePermissions.permissionRequise(methode, chemin);
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        if (permission == null || !droits.autorise(ecole, role, permission)) {
            String libelle = CataloguePermissions.LIBELLES_ROLES.getOrDefault(role, role);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, permission == null
                    ? "Cette fonction n'est pas ouverte au rôle " + libelle + "."
                    : "Le rôle " + libelle + " n'a pas la permission « " + permission + " » dans cette école. "
                      + "L'administrateur peut l'accorder dans « Droits d'accès ».");
        }
        return true;
    }
}
