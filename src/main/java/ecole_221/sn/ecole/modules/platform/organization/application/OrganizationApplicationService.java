package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.platform.organization.application.dto.SchoolRegistrationRequest;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        // Validation basique (il faudrait vérifier l'unicité du subdomain en base)
        Organization org = OrganizationFactory.create(
                request.getSchoolName(),
                OrganizationType.ECOLE,
                request.getSubdomain()
        );
        // Sauvegarder l'organization (en statut PENDING par défaut)
        Organization savedOrg = organizationRepositoryPort.save(org);
        
        // TODO: Créer l'utilisateur admin initial de l'école (via UserRepositoryPort et OrganizationUserRepositoryPort)
        
        return savedOrg;
    }
    
    @Transactional
    public Organization approveSchool(String id) {
        Organization org = organizationRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        org.validate(); // Fait passer de PENDING à ACTIVE
        return organizationRepositoryPort.save(org);
    }
}

