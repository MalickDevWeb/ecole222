package ecole_221.sn.ecole.modules.security.infrastructure.persistence;

import ecole_221.sn.ecole.modules.security.domain.model.VigilProfile;
import ecole_221.sn.ecole.modules.security.domain.model.VigilProfileFactory;
import ecole_221.sn.ecole.modules.security.domain.port.repository.SecurityRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Security.
 * Implémente SecurityRepositoryPort en utilisant les repositories JPA existants.
 */
@Component
public class SecurityRepositoryAdapter implements SecurityRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityRepositoryAdapter.class);
    
    private final VigilProfileJpaRepository vigilProfileJpaRepository;
    
    public SecurityRepositoryAdapter(VigilProfileJpaRepository vigilProfileJpaRepository) {
        this.vigilProfileJpaRepository = vigilProfileJpaRepository;
    }
    
    @Override
    public List<VigilProfile> findAllVigilProfiles() {
        logger.debug("Récupération de tous les profils de vigilance depuis JPA");
        return vigilProfileJpaRepository.findAll().stream()
            .map(this::toVigilProfile)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<VigilProfile> findVigilProfileById(String id) {
        logger.debug("Récupération du profil de vigilance: {}", id);
        return vigilProfileJpaRepository.findById(id)
            .map(this::toVigilProfile);
    }
    
    @Override
    public VigilProfile saveVigilProfile(VigilProfile vigilProfile) {
        logger.debug("Sauvegarde du profil de vigilance: {}", vigilProfile.getId());
        VigilProfileJpaEntity entity = toVigilProfileEntity(vigilProfile);
        VigilProfileJpaEntity savedEntity = vigilProfileJpaRepository.save(entity);
        return toVigilProfile(savedEntity);
    }
    
    @Override
    public void deleteVigilProfile(String id) {
        logger.debug("Suppression du profil de vigilance: {}", id);
        vigilProfileJpaRepository.deleteById(id);
    }
    
    @Override
    public List<VigilProfile> findVigilProfilesByRole(String role) {
        logger.debug("Récupération des profils de vigilance par rôle: {}", role);
        return vigilProfileJpaRepository.findByRole(role).stream()
            .map(this::toVigilProfile)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<VigilProfile> findVigilProfilesByUserId(String userId) {
        logger.debug("Récupération des profils de vigilance par utilisateur: {}", userId);
        // Since VigilProfileJpaEntity doesn't have userId field, we return empty list
        // This method is kept for interface compatibility but not used
        return List.of();
    }
    
    // ==================== MAPPERS ENTITIES VERS DOMAIN ====================

    private VigilProfile toVigilProfile(VigilProfileJpaEntity entity) {
        String nom = entity.getNom() != null ? entity.getNom() : "";
        String prenom = entity.getPrenom() != null ? entity.getPrenom() : "";
        String fullName = (nom + " " + prenom).trim();
        if (fullName.isEmpty()) {
            fullName = "Inconnu";
        }
        return VigilProfileFactory.reconstitute(
            entity.getId(),
            fullName,
            entity.getEquipe(),
            entity.getBadgeId(),
            entity.getStatut(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }
    
    private VigilProfileJpaEntity toVigilProfileEntity(VigilProfile vigilProfile) {
        VigilProfileJpaEntity entity = new VigilProfileJpaEntity();
        entity.setId(vigilProfile.getId());
        String name = vigilProfile.getName() != null ? vigilProfile.getName() : " ";
        String[] nameParts = name.split(" ", 2);
        entity.setNom(nameParts.length > 0 ? nameParts[0] : "");
        entity.setPrenom(nameParts.length > 1 ? nameParts[1] : "");
        entity.setBadgeId(vigilProfile.getUserId());
        entity.setEquipe(vigilProfile.getRole());
        entity.setStatut(vigilProfile.getStatus());
        if (vigilProfile.getCreatedAt() != null) {
            entity.setCreatedAt(java.time.LocalDateTime.parse(vigilProfile.getCreatedAt()));
        }
        return entity;
    }
}