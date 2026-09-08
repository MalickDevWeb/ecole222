package ecole_221.sn.ecole.shared.infrastructure.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class HibernateTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        if (TenantContext.isPlatformMode()) {
            // Dans le cas de la plateforme globale (Super Admin), on ne filtre pas (ou on filtre différemment).
            // Mais avec @TenantId, Hibernate exige une valeur pour enregistrer/lire.
            // Pour l'instant, s'il n'y a pas de tenant défini, on peut retourner une chaîne vide ou le Default Tenant.
            // On pourrait lever une exception si le contexte exige un tenant.
            return TenantContext.getCurrentOrganizationId().orElse("");
        }
        return TenantContext.getCurrentOrganizationId().orElse("");
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}

