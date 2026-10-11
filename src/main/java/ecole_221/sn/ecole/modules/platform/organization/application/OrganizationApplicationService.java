package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.platform.organization.application.dto.SchoolRegistrationRequest;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrganizationApplicationService {

    private final OrganizationRepositoryPort organizationRepositoryPort;

    public OrganizationApplicationService(OrganizationRepositoryPort organizationRepositoryPort) {
        this.organizationRepositoryPort = organizationRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<Organization> getAllOrganizations() {
        return organizationRepositoryPort.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Organization> getOrganizationById(String id) {
        return organizationRepositoryPort.findById(id);
    }

    @Transactional
    public Organization suspendOrganization(String id) {
        Organization org = organizationRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        org.suspend();
        return organizationRepositoryPort.save(org);
    }

    @Transactional
    public Organization activateOrganization(String id) {
        Organization org = organizationRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        org.reactivate();
        return organizationRepositoryPort.save(org);
    }

    @Transactional
    public Organization registerSchool(SchoolRegistrationRequest request) {
        // Construire les settings avec le primaryColor si fourni
        Map<String, Object> settings = new java.util.HashMap<>();
        if (request.getPrimaryColor() != null && !request.getPrimaryColor().isBlank()) {
            settings.put("primaryColor", request.getPrimaryColor());
        }

        // Calculer le slug s'il n'est pas fourni
        String slug = request.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            if (request.getSchoolName() != null && !request.getSchoolName().trim().isEmpty()) {
                slug = request.getSchoolName().toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");
            }
            if (slug == null || slug.trim().isEmpty()) {
                slug = "ecole-" + System.currentTimeMillis();
            }
        }
        slug = slug.trim().toLowerCase();

        Organization org = OrganizationFactory.create(
                request.getSchoolName(),
                OrganizationType.ECOLE,
                OrganizationStatus.PENDING,
                slug,
                null,                  // country
                null,                  // city
                null,                  // address
                null,                  // phone
                request.getAdminEmail(), // email de l'admin → email de l'org
                null,                  // website
                null,                  // logo
                null,                  // subscriptionId
                null,                  // primaryDomainId
                settings.isEmpty() ? null : settings
        );

        // Sauvegarder l'organization (en statut PENDING par défaut)
        return organizationRepositoryPort.save(org);
    }

    
    @Transactional
    public Organization approveSchool(String id) {
        Organization org = organizationRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        org.validate(); // Fait passer de PENDING à ACTIVE
        return organizationRepositoryPort.save(org);
    }
}