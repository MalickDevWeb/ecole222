package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.RegistreDomaines;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Registre des domaines déclarés lu dans la table domaines_ecoles (V17). */
@Component
public class RegistreDomainesJpa implements RegistreDomaines {

    private final DomaineEcoleJpaRepository domaines;

    public RegistreDomainesJpa(DomaineEcoleJpaRepository domaines) {
        this.domaines = domaines;
    }

    @Override
    public Optional<DomaineDeclare> parHote(String hostnameNormalise) {
        if (hostnameNormalise == null || hostnameNormalise.isBlank()) {
            return Optional.empty();
        }
        return domaines.findByHostname(hostnameNormalise)
                .map(d -> new DomaineDeclare(d.getHostname(), d.getOrganizationId(), d.getStatut()));
    }
}
