package ecole_221.sn.ecole.modules.collaboration.domain.port.repository;

import ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AccessControlRepositoryPort {
    AccessControl save(AccessControl accessControl);
    Optional<AccessControl> findById(String id);
    Optional<AccessControl> findByUserAndResource(String userId, String resourceId, String resourceType);
    List<AccessControl> findByUserId(String userId);
    List<AccessControl> findByResourceId(String resourceId, String resourceType);
    List<AccessControl> findActiveByUserId(String userId);
    List<AccessControl> findActiveByResourceId(String resourceId, String resourceType);
    List<AccessControl> findByUserIdAndPermission(String userId, Permission permission);
    void deleteById(String id);
    void revokeByUserAndResource(String userId, String resourceId, String resourceType);
}