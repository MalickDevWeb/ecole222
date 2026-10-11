package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import ecole_221.sn.ecole.modules.auth.domain.exception.UserNotFoundException;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour l'agrégat User.
 * Implémente UserRepositoryPort en utilisant UserJpaRepository.
 */
@Component
public class UserRepositoryAdapter implements UserRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryAdapter.class);
    
    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        logger.debug("Sauvegarde de l'utilisateur: {}", user.getEmail());
        UserJpaEntity entity = toUserEntity(user);
        UserJpaEntity savedEntity = jpaRepository.save(entity);
        return toUser(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        logger.debug("Récupération de l'utilisateur par email: {}", email);
        return jpaRepository.findByEmail(email)
                .map(this::toUser);
    }

    @Override
    public Optional<User> findById(String id) {
        logger.debug("Récupération de l'utilisateur par ID: {}", id);
        return jpaRepository.findById(id)
                .map(this::toUser);
    }
    
    @Override
    public List<User> findAll() {
        logger.debug("Récupération de tous les utilisateurs");
        return jpaRepository.findAll().stream()
            .map(this::toUser)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<User> findByRole(RoleUtilisateur role) {
        logger.debug("Récupération des utilisateurs par rôle: {}", role);
        return jpaRepository.findByRole(role.value()).stream()
            .map(this::toUser)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        logger.debug("Suppression de l'utilisateur: {}", id);
        if (!jpaRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        jpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        logger.debug("Vérification de l'existence de l'utilisateur par email: {}", email);
        return jpaRepository.existsByEmail(email);
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private User toUser(UserJpaEntity entity) {
        return UserFactory.reconstruct(
            entity.getId(),
            entity.getNom(),
            entity.getPrenom(),
            entity.getEmail(),
            entity.getPassword(),
            entity.getMatricule(),
            new RoleUtilisateur(entity.getRole()),
            new java.util.ArrayList<>(),
            entity.getPhotoUrl(),
            entity.getTelephone(),
            entity.getEmailVerified(),
            entity.getPhoneVerified(),
            entity.getLastLoginAt(),
            entity.getLanguage(),
            entity.getTimezone()
        );
    }
    
    private UserJpaEntity toUserEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity(
            user.getId(),
            user.getNom(),
            user.getPrenom(),
            user.getEmail(),
            user.getPassword(),
            user.getRole().value(),
            user.getMatricule(),
            user.getPhotoUrl()
        );
        
        // Set the new fields
        entity.setTelephone(user.getTelephone());
        entity.setEmailVerified(user.getEmailVerified());
        entity.setPhoneVerified(user.getPhoneVerified());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setLanguage(user.getLanguage());
        entity.setTimezone(user.getTimezone());
        
        return entity;
    }
}
