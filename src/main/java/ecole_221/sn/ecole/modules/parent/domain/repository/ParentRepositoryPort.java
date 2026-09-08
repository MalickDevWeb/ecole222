package ecole_221.sn.ecole.modules.parent.domain.repository;

import ecole_221.sn.ecole.modules.parent.domain.model.Parent;
import ecole_221.sn.ecole.modules.parent.domain.model.ParentEnfant;
import java.util.List;
import java.util.Optional;

/**
 * Port de repository pour le module Parent.
 * Interface entre le domaine et l'infrastructure.
 */
public interface ParentRepositoryPort {
    // Parent operations
    Parent saveParent(Parent parent);
    Optional<Parent> findParentById(String id);
    Optional<Parent> findParentByUserId(String userId);
    Optional<Parent> findParentByEmail(String email);
    List<Parent> findAllParents();
    void deleteParent(String id);

    // Parent-Enfant relationship operations
    ParentEnfant saveParentEnfant(ParentEnfant parentEnfant);
    List<ParentEnfant> findParentEnfantsByParentId(String parentId);
    List<ParentEnfant> findParentEnfantsByEnfantId(String enfantId);
    Optional<ParentEnfant> findParentEnfantById(String id);
    Optional<ParentEnfant> findParentEnfantByParentIdAndEnfantId(String parentId, String enfantId);
    void deleteParentEnfant(String parentId, String enfantId);
    boolean existsParentEnfant(String parentId, String enfantId);
}
