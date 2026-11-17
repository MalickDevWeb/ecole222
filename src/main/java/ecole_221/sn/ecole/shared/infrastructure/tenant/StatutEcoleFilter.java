package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Statut de l'école de la requête — défaut F1, corrigé selon la revue DevSecOps du 15/11.
 *
 * <p>Avant : TenantResolutionFilter sautait le contrôle dès qu'un en-tête « Bearer … » ou un cookie auth_token était
 * présent — sans savoir si le jeton était valide (il s'exécute AVANT Spring Security) : une école suspendue restait
 * utilisable, même avec un faux jeton. Désormais ce filtre s'exécute APRÈS Spring Security (ordre 0, la chaîne de
 * sécurité étant à -100) : seules les écoles ACTIVE et PENDING sont servies ; SUSPENDED, ARCHIVED, REJECTED, EXPIRED
 * et CANCELLED sont refusées en 403, compte connecté ou non. Seule exception : un SUPER_ADMIN / PLATFORM_ADMIN dont
 * le jeton a été VALIDÉ (authentification présente dans le contexte de sécurité).</p>
 */
@Component
@Order(0)
public class StatutEcoleFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(StatutEcoleFilter.class);
    static final Set<String> ROLES_PLATEFORME = Set.of("ROLE_SUPER_ADMIN", "ROLE_PLATFORM_ADMIN");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Organization ecole = TenantContext.getCurrentOrganization().orElse(null);
        if (ecole == null || TenantContext.isPlatformMode() || ouverte(ecole) || comptePlateformeAuthentifie()) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        log.warn("Accès refusé à l'école « {} » (statut {}) : {} {}", ecole.getSlug(), ecole.getStatus().value(),
                req.getMethod(), req.getRequestURI());
        if (ecole.isSuspended()) {
            ReponseErreurTenant.ecrire(response, 403, "TENANT_SUSPENDED", "Tenant suspended",
                    "L'établissement « " + ecole.getName() + " » est suspendu : son espace n'est plus accessible, même aux "
                            + "comptes déjà connectés. Contactez l'administration de la plateforme.");
        } else {
            ReponseErreurTenant.ecrire(response, 403, "TENANT_NOT_ACTIVE", "Tenant not active",
                    "L'établissement « " + ecole.getName() + " » n'est plus actif (statut : " + ecole.getStatus().displayName()
                            + ") : son espace n'est plus accessible.");
        }
    }

    /** École servie : ACTIVE, ou PENDING (en cours de configuration, accès inchangé). */
    public static boolean ouverte(Organization ecole) {
        return ecole.isActive() || OrganizationStatus.PENDING.equals(ecole.getStatus());
    }

    private static boolean comptePlateformeAuthentifie() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ROLES_PLATEFORME::contains);
    }
}
