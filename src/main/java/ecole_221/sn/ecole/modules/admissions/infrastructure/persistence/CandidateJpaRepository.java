package ecole_221.sn.ecole.modules.admissions.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CandidateJpaRepository extends JpaRepository<CandidateJpaEntity, String> {
    Optional<CandidateJpaEntity> findByEmail(String email);
    List<CandidateJpaEntity> findByCourseChoice(String courseChoice);
    List<CandidateJpaEntity> findByCurrentStep(String currentStep);
    List<CandidateJpaEntity> findByAdmissionStatus(String admissionStatus);
    
    @Query("SELECT c FROM CandidateJpaEntity c WHERE c.registrationFeePaid = true AND c.admissionStatus = 'pending'")
    List<CandidateJpaEntity> findPaidPendingCandidates();
    
    @Query("SELECT c FROM CandidateJpaEntity c WHERE c.currentStep = :step ORDER BY c.submittedAt DESC")
    List<CandidateJpaEntity> findByCurrentStepOrderBySubmittedAtDesc(@Param("step") String step);
    
    @Query("SELECT COUNT(c) FROM CandidateJpaEntity c WHERE c.admissionStatus = :status")
    Long countByAdmissionStatus(@Param("status") String status);
    
    @Query("SELECT c FROM CandidateJpaEntity c WHERE c.totalScore >= :minScore ORDER BY c.totalScore DESC")
    List<CandidateJpaEntity> findTopCandidatesByScore(@Param("minScore") Double minScore);
}