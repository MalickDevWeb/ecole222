package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Contexte Thread-Safe pour la gestion multi-tenant.
 * Utilise ThreadLocal pour stocker le contexte tenant par thread.
 */
public class TenantContext {
    
    private static final ThreadLocal<Organization> currentOrganization = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> platformMode = ThreadLocal.withInitial(() -> false);
    
    /**
     * Définit l'organization actuelle pour le thread courant.
     */
    public static void setCurrentOrganization(Organization organization) {
        currentOrganization.set(organization);
    }
    
    /**
     * Récupère l'organization actuelle pour le thread courant.
     */
    public static Optional<Organization> getCurrentOrganization() {
        return Optional.ofNullable(currentOrganization.get());
    }
    
    /**
     * Récupère l'ID de l'organization actuelle.
     */
    public static Optional<String> getCurrentOrganizationId() {
        return getCurrentOrganization().map(Organization::getId);
    }
    
    /**
     * Définit le mode plateforme (Super Admin).
     */
    public static void setPlatformMode(boolean isPlatform) {
        platformMode.set(isPlatform);
    }
    
    /**
     * Vérifie si on est en mode plateforme.
     */
    public static boolean isPlatformMode() {
        return platformMode.get();
    }
    
    /**
     * Nettoie le contexte tenant pour le thread courant.
     * IMPORTANT: Appeler cette méthode dans un finally block.
     */
    public static void clear() {
        currentOrganization.remove();
        platformMode.remove();
    }
    
    /**
     * Exécute une action avec un contexte tenant spécifique.
     * Utile pour l'exécution asynchrone (tasks, threads).
     */
    public static <T> CompletableFuture<T> withTenant(Organization organization, java.util.function.Supplier<T> action) {
        Boolean currentPlatformMode = platformMode.get();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                setCurrentOrganization(organization);
                setPlatformMode(currentPlatformMode);
                return action.get();
            } finally {
                clear();
            }
        });
    }
    
    /**
     * Exécute une action en mode plateforme.
     */
    public static <T> CompletableFuture<T> inPlatformMode(java.util.function.Supplier<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                setPlatformMode(true);
                return action.get();
            } finally {
                clear();
            }
        });
    }
    
    /**
     * Vérifie si un tenant est actuellement défini.
     */
    public static boolean hasTenant() {
        return currentOrganization.get() != null;
    }
    
    /**
     * Vérifie si l'utilisateur a accès au tenant actuel.
     */
    public static boolean requiresTenant() {
        return !isPlatformMode() && !hasTenant();
    }
}
