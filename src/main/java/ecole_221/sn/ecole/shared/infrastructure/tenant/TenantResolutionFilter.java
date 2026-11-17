package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolutionHoteEcole;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filtre Servlet de résolution de l'école de la requête (domaines des écoles, revue DevSecOps).
 *
 * <ol>
 *   <li>Adresse demandée : Host, ou X-Forwarded-Host SEULEMENT si la connexion vient d'un relais de confiance
 *       déclaré ({@code ecole.relais.confiance}, vide par défaut) — sinon n'importe quel client choisirait l'adresse
 *       lue ;</li>
 *   <li>HÔTE D'ÉCOLE ({@code <slug>.<domaine-plateforme>} ou domaine propre déclaré) : l'école est celle de l'hôte,
 *       servie seulement si elle existe et n'est pas en attente ; un X-Tenant-Slug / X-Tenant-ID désignant une autre
 *       école → 403 journalisé ;</li>
 *   <li>hôte NEUTRE (hôtes de la plateforme, localhost, adresse IP, hôte du serveur) : école de l'en-tête
 *       X-Tenant-Slug, puis X-Tenant-ID ; sans en-tête, repli de développement local (localhost / 127.0.0.1) sur
 *       ecole-221.</li>
 * </ol>
 * <p>Le Referer n'est plus jamais une source d'école.</p>
 *
 * <p>Ce filtre s'exécute AVANT Spring Security et ne sait pas si un jeton est valide : il ne contrôle plus le statut
 * de l'école (voir StatutEcoleFilter, placé après l'authentification). L'appartenance du compte à l'école est
 * vérifiée par TenantMembershipInterceptor.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantResolutionFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TenantResolutionFilter.class);

    static final String MESSAGE_AUCUNE_ECOLE_A_CETTE_ADRESSE = "Aucun établissement n'est rattaché à cette adresse.";
    static final String MESSAGE_INCOHERENCE = "Adresse et établissement incohérents.";
    /**
     * Aucun relais de confiance par défaut (revue DevSecOps phase 2, B2) : derrière nginx ou le proxy Vite, la
     * connexion arrive de 127.0.0.1 ; faire confiance à cette adresse laisserait le client choisir l'hôte lu par
     * X-Forwarded-Host. Déclarer explicitement l'IP du relais qui ÉCRASE cet en-tête (ECOLE_RELAIS_CONFIANCE).
     */
    static final String RELAIS_PAR_DEFAUT = "";

    private final OrganizationRepositoryPort organizationRepository;
    private final ResolutionHoteEcole resolution;
    private final Set<String> relaisDeConfiance;

    public TenantResolutionFilter(OrganizationRepositoryPort organizationRepository, ResolutionHoteEcole resolution) {
        this(organizationRepository, resolution, RELAIS_PAR_DEFAUT);
    }

    @Autowired
    public TenantResolutionFilter(OrganizationRepositoryPort organizationRepository, ResolutionHoteEcole resolution,
                                  @Value("${ecole.relais.confiance:}") String relais) {
        this.organizationRepository = organizationRepository;
        this.resolution = resolution;
        this.relaisDeConfiance = relais == null ? Set.of() : Arrays.stream(relais.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String host = httpRequest.getServerName();

        try {
            logger.debug("Processing request for host: {}, URI: {}", host, httpRequest.getRequestURI());

            // 0. Pré-vérification CORS (OPTIONS + Origin + Access-Control-Request-Method) : le navigateur n'y joint
            // aucun en-tête personnalisé ; Spring Security y répond lui-même, sans atteindre de contrôleur.
            if (estPreverificationCors(httpRequest)) {
                chain.doFilter(request, response);
                return;
            }

            // 1. Requête plateforme (super-administrateur, authentification, routes publiques)
            if (isPlatformRequest(httpRequest)) {
                logger.debug("Platform mode request detected");
                TenantContext.setPlatformMode(true);
                chain.doFilter(request, response);
                return;
            }

            String enTete = premierNonVide(httpRequest.getHeader("X-Tenant-Slug"), httpRequest.getHeader("X-Tenant-ID"));
            String hote = hoteDemande(httpRequest);
            Organization organization;

            if (resolution.estHoteDEcole(hote)) {
                // 2. Hôte d'école : l'école est celle de l'hôte
                organization = resolution.ecoleDepuisHote(hote).orElse(null);
                if (organization == null || OrganizationStatus.PENDING.equals(organization.getStatus())) {
                    logger.warn("Adresse {} : aucune école servie (inconnue, non pointée ou en attente)", hote);
                    ReponseErreurTenant.ecrire(response, 404, "TENANT_NOT_FOUND", "Tenant not found", MESSAGE_AUCUNE_ECOLE_A_CETTE_ADRESSE);
                    return;
                }
                if (enTete != null) {
                    Organization demandee = resolveOrganization(enTete, null);
                    if (demandee == null || !organization.getId().equals(demandee.getId())) {
                        logger.warn("[SÉCURITÉ] Adresse et établissement incohérents : hôte {} (école « {} ») mais en-tête "
                                + "désignant « {} » ({} {}, IP {})", hote, organization.getSlug(), enTete,
                                httpRequest.getMethod(), httpRequest.getRequestURI(), httpRequest.getRemoteAddr());
                        ReponseErreurTenant.ecrire(response, 403, "TENANT_ADDRESS_MISMATCH", "Tenant address mismatch", MESSAGE_INCOHERENCE);
                        return;
                    }
                }
            } else if (enTete != null) {
                // 3. Hôte neutre : école de l'en-tête
                organization = resolveOrganization(enTete, host);
                if (organization == null) {
                    logger.warn("Organization not found for tenant: {}", enTete);
                    ReponseErreurTenant.ecrire(response, 404, "TENANT_NOT_FOUND", "Tenant not found",
                            "Établissement introuvable : « " + enTete + " ».");
                    return;
                }
            } else {
                // 4. Hôte neutre sans en-tête : repli de développement local uniquement
                organization = resolveOrganization(host, host);
                if (organization == null) {
                    logger.warn("Organization not found for host: {}", hote);
                    ReponseErreurTenant.ecrire(response, 404, "TENANT_NOT_FOUND", "Tenant not found", MESSAGE_AUCUNE_ECOLE_A_CETTE_ADRESSE);
                    return;
                }
            }

            // 5. Définir le contexte tenant (le statut de l'école est contrôlé après l'authentification : StatutEcoleFilter)
            TenantContext.setCurrentOrganization(organization);
            TenantContext.setPlatformMode(false);

            logger.debug("Tenant context set for organization: {} ({})", organization.getName(), organization.getId());

            chain.doFilter(request, response);

        } finally {
            // Nettoyer le contexte
            TenantContext.clear();
            logger.debug("Tenant context cleared");
        }
    }

    static boolean estPreverificationCors(HttpServletRequest req) {
        return "OPTIONS".equalsIgnoreCase(req.getMethod()) && req.getHeader("Origin") != null
                && req.getHeader("Access-Control-Request-Method") != null;
    }

    private static String premierNonVide(String... valeurs) {
        for (String v : valeurs) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /**
     * Adresse demandée : X-Forwarded-Host (première valeur) seulement si la connexion vient d'un relais de confiance ;
     * sinon Host. Le relais doit ÉCRASER X-Forwarded-Host (et non l'ajouter à la valeur reçue du client).
     */
    String hoteDemande(HttpServletRequest req) {
        String transmis = req.getHeader("X-Forwarded-Host");
        if (transmis != null && !transmis.isBlank() && relaisDeConfiance.contains(req.getRemoteAddr())) {
            return transmis.split(",")[0].trim();
        }
        return req.getServerName();
    }

    /**
     * Résout l'organization à partir de l'identifiant tenant (slug, alias ou identifiant).
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

        // (Recherches factices par sous-domaine, domaine personnalisé et e-mail « @platform.com » retirées ;
        // l'adresse est désormais résolue par ResolutionHoteEcole.)

        // Fallback développement local : UNIQUEMENT quand aucune école n'a été demandée explicitement
        // (l'identifiant est l'hôte lui-même). Sinon, derrière le proxy Vite (hôte 127.0.0.1), toute école
        // inconnue demandée par X-Tenant-Slug (ex. « 192 ») serait servie comme ecole-221.
        boolean identifiantEstLHote = identifier.equalsIgnoreCase(host);
        if (identifiantEstLHote && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equalsIgnoreCase(host))) {
            logger.info("Local development detected for tenant '{}': defaulting to ecole-221", identifier);
            return organizationRepository.findBySlug("ecole-221")
                    .or(() -> organizationRepository.findById("default-tenant-id"))
                    .orElse(null);
        }

        return null;
    }

    /**
     * Vérifie si la requête est une requête plateforme (Super Admin) ou publique.
     * Ces requêtes ne nécessitent pas de résolution de tenant.
     *
     * <p>I3 (revue DevSecOps) : /api/sync n'en fait plus partie (un préfixe {@code startsWith("/api/sync")}, qui
     * couvrirait aussi /api/sync*, ferait traiter les actions hors ligne sans école ni contrôle d'appartenance).
     * Seule la vérification de version GET /api/sync/check, sans aucune donnée, reste hors école.</p>
     */
    private boolean isPlatformRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/super-admin/")   // Dashboard super-admin
            || path.startsWith("/api/super-admin/")      // Super-admin sans préfixe v1
            || path.startsWith("/api/v1/platform/")       // Routes plateforme
            || path.startsWith("/api/platform/")         // Platform sans préfixe v1
            || path.startsWith("/api/auth/")              // Authentification
            || path.startsWith("/api/register-school")    // Inscription école
            || path.equals("/api/sync/check")             // Version de la synchronisation (aucune donnée)
            || path.startsWith("/api/public/")            // Routes publiques
            || path.startsWith("/swagger-ui")             // Documentation
            || path.startsWith("/v3/api-docs");           // OpenAPI
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
