package ecole_221.sn.ecole.modules.admissions.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AdmissionSettingsJpaRepository extends JpaRepository<AdmissionSettingsJpaEntity, String> {
    Optional<AdmissionSettingsJpaEntity> findByAcademicYear(String academicYear);
    
    @Query("SELECT a FROM AdmissionSettingsJpaEntity a WHERE a.isOpen = true ORDER BY a.openingDate DESC")
    Optional<AdmissionSettingsJpaEntity> findActiveAdmissionSettings();
    
    @Query("SELECT a FROM AdmissionSettingsJpaEntity a WHERE a.academicYear = :year")
    Optional<AdmissionSettingsJpaEntity> findByYear(@Param("year") String year);
}