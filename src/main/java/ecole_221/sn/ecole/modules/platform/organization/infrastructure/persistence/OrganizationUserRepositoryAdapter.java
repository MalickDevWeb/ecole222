package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.MembershipStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter JPA pour le repository OrganizationUser.
 * Convertit entre les entités JPA et les agrégats du domaine.
 */
@Repository
public class OrganizationUserRepositoryAdapter implements OrganizationUserRepositoryPort {
    
    private final OrganizationUserJpaRepository jpaRepository;
    
    public OrganizationUserRepositoryAdapter(OrganizationUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public OrganizationUser save(OrganizationUser organizationUser) {
        OrganizationUserJpaEntity entity = toEntity(organizationUser);
        OrganizationUserJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<OrganizationUser> findById(String id) {
        return jpaRepository.findById(id)
            .map(this::toDomain);
    }
    
    @Override
    public Optional<OrganizationUser> findByUserIdAndOrganizationId(String userId, String organizationId) {
        return jpaRepository.findByUserIdAndOrganizationId(userId, organizationId)
            .map(this::toDomain);
    }
    
    @Override
    public List<OrganizationUser> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationUser> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationUser> findByOrganizationIdAndStatus(String organizationId, MembershipStatus status) {
        return jpaRepository.findByOrganizationIdAndStatus(organizationId, status.value()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationUser> findByOrganizationIdAndRole(String organizationId, String role) {
        return jpaRepository.findByOrganizationIdAndRole(organizationId, role).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public long countByOrganizationId(String organizationId) {
        return jpaRepository.countByOrganizationId(organizationId);
    }
    
    @Override
    public boolean existsByUserIdAndOrganizationId(String userId, String organizationId) {
        return jpaRepository.existsByUserIdAndOrganizationId(userId, organizationId);
    }
    
    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public void deleteByUserId(String userId) {
        List<OrganizationUserJpaEntity> entities = jpaRepository.findByUserId(userId);
        jpaRepository.deleteAll(entities);
    }
    
    @Override
    public void deleteByOrganizationId(String organizationId) {
        List<OrganizationUserJpaEntity> entities = jpaRepository.findByOrganizationId(organizationId);
        jpaRepository.deleteAll(entities);
    }
    
    // Méthodes de conversion
    private OrganizationUserJpaEntity toEntity(OrganizationUser domain) {
        OrganizationUserJpaEntity entity = new OrganizationUserJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setOrganizationId(domain.getOrganizationId());
        entity.setRole(domain.getRole());
        entity.setPermissions(domain.getPermissions());
        entity.setScopeType(domain.getScopeType() != null ? domain.getScopeType().value() : null);
        entity.setScopeId(domain.getScopeId());
        entity.setStatus(domain.getStatus().value());
        entity.setJoinedAt(domain.getJoinedAt());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setIsDelegated(domain.getIsDelegated());
        entity.setDelegationId(domain.getDelegationId());
        return entity;
    }
    
    private OrganizationUser toDomain(OrganizationUserJpaEntity entity) {
        return OrganizationUserFactory.reconstruct(
            entity.getId(),
            entity.getUserId(),
            entity.getOrganizationId(),
            entity.getRole(),
            entity.getPermissions() != null ? entity.getPermissions().toArray(new String[0]) : null,
            entity.getScopeType(),
            entity.getScopeId(),
            entity.getStatus(),
            entity.getJoinedAt(),
            entity.getExpiresAt(),
            entity.getIsDelegated(),
            entity.getDelegationId()
        );
    }
}
