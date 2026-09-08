package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
    Optional<UserJpaEntity> findByEmail(String email);
    List<UserJpaEntity> findByRole(String role);
    boolean existsByEmail(String email);
    
    // Nouvelles méthodes pour les champs ajoutés
    Optional<UserJpaEntity> findByTelephone(String telephone);
    List<UserJpaEntity> findByEmailVerified(Boolean emailVerified);
    List<UserJpaEntity> findByLanguage(String language);
    
    @Query("SELECT u FROM UserJpaEntity u WHERE u.email = :email AND u.emailVerified = true")
    Optional<UserJpaEntity> findVerifiedByEmail(@Param("email") String email);
}
