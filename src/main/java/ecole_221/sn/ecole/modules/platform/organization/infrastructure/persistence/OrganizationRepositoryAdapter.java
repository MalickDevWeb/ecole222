package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter JPA pour le repository Organization.
 * Convertit entre les entités JPA et les agrégats du domaine.
 */
@Repository
public class OrganizationRepositoryAdapter implements OrganizationRepositoryPort {
    
    private final OrganizationJpaRepository jpaRepository;
    
    public OrganizationRepositoryAdapter(OrganizationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Organization save(Organization organization) {
        OrganizationJpaEntity entity = toEntity(organization);
        OrganizationJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<Organization> findById(String id) {
        return jpaRepository.findById(id)
            .map(this::toDomain);
    }
    
    @Override
    public Optional<Organization> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
            .map(this::toDomain);
    }
    
    @Override
    public Optional<Organization> findBySubdomain(String subdomain) {
        // Pour l'instant, on utilise une logique simple
        // À améliorer avec la table domains
        return jpaRepository.findAll().stream()
            .filter(org -> org.getName().toLowerCase().replace(" ", "-").equals(subdomain))
            .findFirst()
            .map(this::toDomain);
    }
    
    @Override
    public Optional<Organization> findByCustomDomain(String customDomain) {
        // return jpaRepository.findByDomainName(customDomain).map(this::toDomain);
        return Optional.empty(); // À implémenter
    }
    
    @Override
    public List<Organization> findByStatus(OrganizationStatus status) {
        OrganizationStatusJpa statusJpa = OrganizationStatusJpa.valueOf(status.value());
        return jpaRepository.findByStatus(statusJpa).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Organization> findByType(OrganizationType type) {
        OrganizationTypeJpa typeJpa = OrganizationTypeJpa.valueOf(type.value());
        return jpaRepository.findByType(typeJpa).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Organization> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public long countByStatus(OrganizationStatus status) {
        OrganizationStatusJpa statusJpa = OrganizationStatusJpa.valueOf(status.value());
        return jpaRepository.countByStatus(statusJpa);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
    
    // Méthodes de conversion
    private OrganizationJpaEntity toEntity(Organization domain) {
        OrganizationJpaEntity entity = new OrganizationJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setType(OrganizationTypeJpa.valueOf(domain.getType().value()));
        entity.setStatus(OrganizationStatusJpa.valueOf(domain.getStatus().value()));
        entity.setCountry(domain.getCountry());
        entity.setCity(domain.getCity());
        entity.setAddress(domain.getAddress());
        entity.setPhone(domain.getPhone());
        entity.setEmail(domain.getEmail());
        entity.setWebsite(domain.getWebsite());
        entity.setLogo(domain.getLogo());
        entity.setSubscriptionId(domain.getSubscriptionId());
        entity.setPrimaryDomainId(domain.getPrimaryDomainId());
        entity.setSettings(domain.getSettings());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setValidatedAt(domain.getValidatedAt());
        entity.setSuspendedAt(domain.getSuspendedAt());
        return entity;
    }
    
    private Organization toDomain(OrganizationJpaEntity entity) {
        return OrganizationFactory.reconstruct(
            entity.getId(),
            entity.getName(),
            entity.getType().name(),
            entity.getStatus().name(),
            entity.getCountry(),
            entity.getCity(),
            entity.getAddress(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getWebsite(),
            entity.getLogo(),
            entity.getSubscriptionId(),
            entity.getPrimaryDomainId(),
            entity.getSettings(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getValidatedAt(),
            entity.getSuspendedAt()
        );
    }
}
