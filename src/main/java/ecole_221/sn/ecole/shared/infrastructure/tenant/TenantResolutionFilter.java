package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
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
            logger.debug("Processing request for host: {}, URI: {}", host, httpRequest.getRequestURI());
            
            // 1. Extraire l'identifiant tenant (Header prioritaire, puis Referer, puis Host)
            String headerTenant = httpRequest.getHeader("X-Tenant-Slug");
            if (headerTenant == null || headerTenant.isBlank()) {
                headerTenant = httpRequest.getHeader("X-Tenant-ID");
            }
            if (headerTenant == null || headerTenant.isBlank()) {
                String referer = httpRequest.getHeader("Referer");
                if (referer != null) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("/t/([^/?#]+)").matcher(referer);
                    if (m.find()) {
                        headerTenant = m.group(1);
                    }
                }
            }
            String tenantIdentifier = (headerTenant != null && !headerTenant.isBlank())
                    ? headerTenant.trim()
                    : extractTenantIdentifier(host);
            logger.debug("Extracted tenant identifier: {}", tenantIdentifier);
            
            // 2. Vérifier si c'est une requête plateforme
            if (isPlatformRequest(httpRequest)) {
                logger.debug("Platform mode request detected");
                TenantContext.setPlatformMode(true);
                chain.doFilter(request, response);
                return;
            }
            
            // 3. Résoudre l'organization
            Organization organization = resolveOrganization(tenantIdentifier, host);
            
            if (organization == null) {
                logger.warn("Organization not found for tenant: {}", tenantIdentifier);
                throw new TenantNotFoundException("Tenant not found: " + tenantIdentifier);
            }
            
            // 4. Valider le statut de l'organization
            validateOrganizationStatus(organization, httpRequest);
            
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
     * Résout l'organization à partir de l'identifiant tenant avec isolation stricte.
     */
    private Organization resolveOrganization(String identifier, String host) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        // Cas spécial école 221 (alias ecole221 / ecole-221)
        if ("ecole221".equalsIgnoreCase(identifier) || "ecole-221".equalsIgnoreCase(identifier)) {
            return organizationRepository.findBySlug("ecole-221")
                    .or(() -> organizationRepository.findById("default-tenant-id"))
                    .orElse(null);
        }

        // 1. Essayer par slug exact
        Organization org = organizationRepository.findBySlug(identifier.toLowerCase().trim()).orElse(null);
        if (org != null) {
            return org;
        }

        // 2. Essayer par ID direct (si UUID passé via X-Tenant-ID)
        org = organizationRepository.findById(identifier).orElse(null);
        if (org != null) {
            return org;
        }

        // 3. Essayer par subdomain
        org = organizationRepository.findBySubdomain(identifier).orElse(null);
        if (org != null) {
            return org;
        }
        
        // 4. Essayer par custom domain
        org = organizationRepository.findByCustomDomain(identifier).orElse(null);
        if (org != null) {
            return org;
        }
        
        // 5. Essayer par email (fallback)
        org = organizationRepository.findByEmail(identifier + "@" + PLATFORM_DOMAIN).orElse(null);
        if (org != null) {
            return org;
        }

        // Fallback développement local : si host = localhost ou 127.0.0.1, résoudre vers ecole-221
        if ("localhost".equalsIgnoreCase(identifier) || "127.0.0.1".equalsIgnoreCase(identifier)
                || "localhost".equalsIgnoreCase(host) || "127.0.0.1".equalsIgnoreCase(host)) {
            logger.info("Local development detected for tenant '{}': defaulting to ecole-221", identifier);
            return organizationRepository.findBySlug("ecole-221")
                    .or(() -> organizationRepository.findById("default-tenant-id"))
                    .orElse(null);
        }
        
        return null;
    }
    
    /**
     * Valide le statut de l'organization.
     * Les requêtes authentifiées (Super Admin / Administrateur) peuvent accéder aux écoles en cours de configuration.
     */
    private void validateOrganizationStatus(Organization organization, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return;
        }

        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return;
                }
            }
        }

        if (organization.isSuspended()) {
            throw new TenantSuspendedException("Tenant is suspended: " + organization.getName());
        }
        
        if (!organization.isActive() && !organization.getStatus().equals(OrganizationStatus.PENDING)) {
            throw new TenantExpiredException("Tenant is not active: " + organization.getName() + 
                " (Status: " + organization.getStatus().displayName() + ")");
        }
    }
    
    /**
     * Vérifie si la requête est une requête plateforme (Super Admin) ou publique.
     * Ces requêtes ne nécessitent pas de résolution de tenant.
     */
    private boolean isPlatformRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/super-admin/")   // Dashboard super-admin
            || path.startsWith("/api/super-admin/")      // Super-admin sans préfixe v1
            || path.startsWith("/api/v1/platform/")       // Routes plateforme
            || path.startsWith("/api/platform/")         // Platform sans préfixe v1
            || path.startsWith("/api/auth/")              // Authentification
            || path.startsWith("/api/register-school")    // Inscription école
            || path.startsWith("/api/sync")               // Synchronisation
            || path.startsWith("/api/candidatures/settings") // Paramètres publics
            || path.startsWith("/api/public/")            // Routes publiques
            || path.startsWith("/swagger-ui")             // Documentation
            || path.startsWith("/v3/api-docs");           // OpenAPI
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
