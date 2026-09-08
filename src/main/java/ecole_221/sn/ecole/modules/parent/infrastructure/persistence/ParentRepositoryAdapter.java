package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import ecole_221.sn.ecole.modules.parent.domain.model.Parent;
import ecole_221.sn.ecole.modules.parent.domain.model.ParentEnfant;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Parent.
 * Implémente ParentRepositoryPort en utilisant les repositories JPA.
 */
@Component
public class ParentRepositoryAdapter implements ParentRepositoryPort {

    private static final Logger logger = LoggerFactory.getLogger(ParentRepositoryAdapter.class);

    private final ParentJpaRepository parentJpaRepository;
    private final ParentEnfantJpaRepository parentEnfantJpaRepository;

    public ParentRepositoryAdapter(
            ParentJpaRepository parentJpaRepository,
            ParentEnfantJpaRepository parentEnfantJpaRepository) {
        this.parentJpaRepository = parentJpaRepository;
        this.parentEnfantJpaRepository = parentEnfantJpaRepository;
    }

    // ==================== Parent operations ====================

    @Override
    public Parent saveParent(Parent parent) {
        logger.debug("Sauvegarde du parent: {}", parent.getId());
        ParentJpaEntity entity = toParentJpaEntity(parent);
        ParentJpaEntity savedEntity = parentJpaRepository.save(entity);
        return toParent(savedEntity);
    }

    @Override
    public Optional<Parent> findParentById(String id) {
        logger.debug("Récupération du parent par ID: {}", id);
        return parentJpaRepository.findById(id)
                .map(this::toParent);
    }

    @Override
    public Optional<Parent> findParentByUserId(String userId) {
        logger.debug("Récupération du parent par userId: {}", userId);
        return parentJpaRepository.findByUserId(userId)
                .map(this::toParent);
    }

    @Override
    public Optional<Parent> findParentByEmail(String email) {
        logger.debug("Récupération du parent par email: {}", email);
        return parentJpaRepository.findByEmail(email)
                .map(this::toParent);
    }

    @Override
    public List<Parent> findAllParents() {
        logger.debug("Récupération de tous les parents");
        return parentJpaRepository.findAll().stream()
                .map(this::toParent)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteParent(String id) {
        logger.debug("Suppression du parent: {}", id);
        parentJpaRepository.deleteById(id);
    }

    // ==================== Parent-Enfant operations ====================

    @Override
    public ParentEnfant saveParentEnfant(ParentEnfant parentEnfant) {
        logger.debug("Sauvegarde de la relation parent-enfant: {}", parentEnfant.getId());
        ParentEnfantJpaEntity entity = toParentEnfantJpaEntity(parentEnfant);
        ParentEnfantJpaEntity savedEntity = parentEnfantJpaRepository.save(entity);
        return toParentEnfant(savedEntity);
    }

    @Override
    public List<ParentEnfant> findParentEnfantsByParentId(String parentId) {
        logger.debug("Récupération des relations parent-enfant par parentId: {}", parentId);
        return parentEnfantJpaRepository.findByParentId(parentId).stream()
                .map(this::toParentEnfant)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParentEnfant> findParentEnfantsByEnfantId(String enfantId) {
        logger.debug("Récupération des relations parent-enfant par enfantId: {}", enfantId);
        return parentEnfantJpaRepository.findByEnfantId(enfantId).stream()
                .map(this::toParentEnfant)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ParentEnfant> findParentEnfantById(String id) {
        logger.debug("Récupération de la relation parent-enfant par ID: {}", id);
        return parentEnfantJpaRepository.findById(id)
                .map(this::toParentEnfant);
    }

    @Override
    public Optional<ParentEnfant> findParentEnfantByParentIdAndEnfantId(String parentId, String enfantId) {
        logger.debug("Récupération de la relation parent-enfant par parentId et enfantId");
        return parentEnfantJpaRepository.findByParentIdAndEnfantId(parentId, enfantId)
                .map(this::toParentEnfant);
    }

    @Override
    public void deleteParentEnfant(String parentId, String enfantId) {
        logger.debug("Suppression de la relation parent-enfant");
        parentEnfantJpaRepository.deleteByParentIdAndEnfantId(parentId, enfantId);
    }

    @Override
    public boolean existsParentEnfant(String parentId, String enfantId) {
        return parentEnfantJpaRepository.findByParentIdAndEnfantId(parentId, enfantId)
                .isPresent();
    }

    // ==================== MAPPERS ====================

    private ParentJpaEntity toParentJpaEntity(Parent parent) {
        ParentJpaEntity entity = new ParentJpaEntity();
        entity.setId(parent.getId());
        entity.setUserId(parent.getUserId());
        entity.setNom(parent.getNom());
        entity.setPrenom(parent.getPrenom());
        entity.setEmail(parent.getEmail());
        entity.setTelephone(parent.getTelephone());
        entity.setAdresse(parent.getAdresse());
        entity.setProfession(parent.getProfession());
        if (parent.getCreatedAt() != null) {
            entity.setCreatedAt(parent.getCreatedAt());
        }
        if (parent.getUpdatedAt() != null) {
            entity.setUpdatedAt(parent.getUpdatedAt());
        }
        return entity;
    }

    private Parent toParent(ParentJpaEntity entity) {
        return Parent.reconstitute(
            entity.getId(),
            entity.getUserId(),
            entity.getNom(),
            entity.getPrenom(),
            entity.getEmail(),
            entity.getTelephone(),
            entity.getAdresse(),
            entity.getProfession(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private ParentEnfantJpaEntity toParentEnfantJpaEntity(ParentEnfant parentEnfant) {
        ParentEnfantJpaEntity entity = new ParentEnfantJpaEntity();
        entity.setId(parentEnfant.getId());
        entity.setParentId(parentEnfant.getParentId());
        entity.setEnfantId(parentEnfant.getEnfantId());
        entity.setRelation(parentEnfant.getRelation());
        if (parentEnfant.getCreatedAt() != null) {
            entity.setCreatedAt(parentEnfant.getCreatedAt());
        }
        return entity;
    }

    private ParentEnfant toParentEnfant(ParentEnfantJpaEntity entity) {
        return ParentEnfant.reconstitute(
            entity.getId(),
            entity.getParentId(),
            entity.getEnfantId(),
            entity.getRelation(),
            entity.getCreatedAt()
        );
    }
}
