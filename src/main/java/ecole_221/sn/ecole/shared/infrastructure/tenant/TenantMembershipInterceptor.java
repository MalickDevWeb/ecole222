package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Set;

/**
 * SEC-003 — Étanchéité entre écoles : l'école d'une requête est choisie par le client
 * (X-Tenant-Slug, X-Tenant-ID, Referer, host) ; on vérifie ici que l'utilisateur connecté
 * en est MEMBRE avant d'atteindre le contrôleur. Sinon : 403.
 *
 * <p><b>Phase de transition</b> (décision du 2026-11-07) : organization_users est encore
 * incomplète (élèves, professeurs… créés sans appartenance). Un utilisateur SANS AUCUNE
 * appartenance garde temporairement l'ancien comportement, journalisé « SEC-003 TRANSITION ».
 * Après la reprise des données ({@code ./orchestrator.sh backfill-memberships --apply}), activer
 * {@code ecole.security.tenant-membership.strict=true} : l'exception est alors supprimée (403).</p>
 */
@Component
public class TenantMembershipInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantMembershipInterceptor.class);
    private static final Set<String> PLATFORM_ROLES = Set.of("ROLE_SUPER_ADMIN", "ROLE_PLATFORM_ADMIN");

    private final OrganizationUserRepositoryPort memberships;
    private final boolean strict;

    public TenantMembershipInterceptor(OrganizationUserRepositoryPort memberships,
                                       @Value("${ecole.security.tenant-membership.strict:false}") boolean strict) {
        this.memberships = memberships;
        this.strict = strict;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String organizationId = TenantContext.getCurrentOrganizationId().orElse(null);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (organizationId == null || TenantContext.isPlatformMode() || !isAuthenticatedUser(auth)) {
            return true;   // requête publique ou plateforme : les règles de SecurityConfig s'appliquent
        }
        if (auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(PLATFORM_ROLES::contains)) {
            return true;   // SUPER_ADMIN / PLATFORM_ADMIN administrent toutes les écoles
        }
        String userId = ((User) auth.getPrincipal()).getId();
        List<OrganizationUser> userMemberships = memberships.findByUserId(userId);
        if (userMemberships == null || userMemberships.isEmpty()) {
            return legacyAllowed(userId, organizationId, request);
        }
        boolean member = userMemberships.stream()
                .anyMatch(m -> organizationId.equals(m.getOrganizationId()) && m.isActive() && !m.isExpired());
        if (!member) {
            log.warn("SEC-003 REFUS : utilisateur {} non membre de l'école {} ({} {})",
                    userId, organizationId, request.getMethod(), request.getRequestURI());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous n'êtes pas membre de cet établissement.");
        }
        return true;
    }

    /** Transition : utilisateur sans appartenance enregistrée (données historiques incomplètes). */
    private boolean legacyAllowed(String userId, String organizationId, HttpServletRequest request) {
        if (strict) {
            log.warn("SEC-003 REFUS (strict) : utilisateur {} sans appartenance, école {} ({} {})",
                    userId, organizationId, request.getMethod(), request.getRequestURI());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous n'êtes pas membre de cet établissement.");
        }
        log.warn("SEC-003 TRANSITION : utilisateur {} sans appartenance, accès à l'école {} non vérifié ({} {})",
                userId, organizationId, request.getMethod(), request.getRequestURI());
        return true;
    }

    private boolean isAuthenticatedUser(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getPrincipal() instanceof User;
    }
}
