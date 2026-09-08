package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtre Servlet pour la résolution multi-tenant.
 * Extrait le tenant à partir du domaine (subdomain ou custom domain)
 * et définit le contexte tenant pour la requête.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantResolutionFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantResolutionFilter.class);
    
    private static final String PLATFORM_DOMAIN = "platform.com";
    
    private final OrganizationRepositoryPort organizationRepository;
    
    public TenantResolutionFilter(OrganizationRepositoryPort organizationRepository) {
        this.organizationRepository = organizationRepository;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String host = httpRequest.getServerName();
        
        try {
            logger.debug("Processing request for host: {}", host);
            
            // 1. Extraire l'identifiant tenant
            String tenantIdentifier = extractTenantIdentifier(host);
            logger.debug("Extracted tenant identifier: {}", tenantIdentifier);
            
            // 2. Vérifier si c'est une requête plateforme
            if (isPlatformRequest(httpRequest)) {
                logger.debug("Platform mode request detected");
                TenantContext.setPlatformMode(true);
                chain.doFilter(request, response);
                return;
            }
            
            // 3. Résoudre l'organization
            Organization organization = resolveOrganization(tenantIdentifier);
            
            if (organization == null) {
                logger.warn("Organization not found for tenant: {}", tenantIdentifier);
                throw new TenantNotFoundException("Tenant not found: " + tenantIdentifier);
            }
            
            // 4. Valider le statut de l'organization
            validateOrganizationStatus(organization);
            
            // 5. Définir le contexte tenant
            TenantContext.setCurrentOrganization(organization);
            TenantContext.setPlatformMode(false);
            
            logger.debug("Tenant context set for organization: {} ({})", organization.getName(), organization.getId());
            
            chain.doFilter(request, response);
            
        } catch (TenantNotFoundException e) {
            logger.error("Tenant not found: {}", e.getMessage());
            handleTenantNotFound(response, e);
        } catch (TenantSuspendedException e) {
            logger.error("Tenant suspended: {}", e.getMessage());
            handleTenantSuspended(response, e);
        } catch (TenantExpiredException e) {
            logger.error("Tenant expired: {}", e.getMessage());
            handleTenantExpired(response, e);
        } finally {
            // Nettoyer le contexte
            TenantContext.clear();
            logger.debug("Tenant context cleared");
        }
    }
    
    /**
     * Extrait l'identifiant tenant à partir du host.
     * ecole-a.platform.com → ecole-a
     * www.ecole-a.sn → www.ecole-a.sn (custom domain)
     */
    private String extractTenantIdentifier(String host) {
        if (host.endsWith("." + PLATFORM_DOMAIN)) {
            // Subdomain format: ecole-a.platform.com
            return host.substring(0, host.indexOf("." + PLATFORM_DOMAIN));
        }
        // Custom domain format: www.ecole-a.sn
        return host;
    }
    
    /**
     * Résout l'organization à partir de l'identifiant tenant.
     */
    private Organization resolveOrganization(String identifier) {
        // Essayer par subdomain d'abord
        Organization org = organizationRepository.findBySubdomain(identifier).orElse(null);
        if (org != null) {
            return org;
        }
        
        // Essayer par custom domain
        org = organizationRepository.findByCustomDomain(identifier).orElse(null);
        if (org != null) {
            return org;
        }
        
        // Essayer par email (fallback)
        org = organizationRepository.findByEmail(identifier + "@" + PLATFORM_DOMAIN).orElse(null);
        
        return org;
    }
    
    /**
     * Valide le statut de l'organization.
     */
    private void validateOrganizationStatus(Organization organization) {
        if (organization.isSuspended()) {
            throw new TenantSuspendedException("Tenant is suspended: " + organization.getName());
        }
        
        if (!organization.isActive()) {
            throw new TenantExpiredException("Tenant is not active: " + organization.getName() + 
                " (Status: " + organization.getStatus().displayName() + ")");
        }
    }
    
    /**
     * Vérifie si la requête est une requête plateforme (Super Admin).
     */
    private boolean isPlatformRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/platform/") || 
               path.startsWith("/admin/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
    
    /**
     * Gère les erreurs de tenant non trouvé.
     */
    private void handleTenantNotFound(ServletResponse response, TenantNotFoundException e) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType("application/json");
        httpResponse.setStatus(404);
        String errorResponse = "{\"error\":\"Tenant not found\",\"message\":\"" + e.getMessage() + "\"}";
        httpResponse.getWriter().write(errorResponse);
    }
    
    /**
     * Gère les erreurs de tenant suspendu.
     */
    private void handleTenantSuspended(ServletResponse response, TenantSuspendedException e) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType("application/json");
        httpResponse.setStatus(403);
        String errorResponse = "{\"error\":\"Tenant suspended\",\"message\":\"" + e.getMessage() + "\"}";
        httpResponse.getWriter().write(errorResponse);
    }
    
    /**
     * Gère les erreurs de tenant expiré.
     */
    private void handleTenantExpired(ServletResponse response, TenantExpiredException e) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType("application/json");
        httpResponse.setStatus(403);
        String errorResponse = "{\"error\":\"Tenant not active\",\"message\":\"" + e.getMessage() + "\"}";
        httpResponse.getWriter().write(errorResponse);
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("TenantResolutionFilter initialized");
    }
    
    @Override
    public void destroy() {
        logger.info("TenantResolutionFilter destroyed");
    }
}
