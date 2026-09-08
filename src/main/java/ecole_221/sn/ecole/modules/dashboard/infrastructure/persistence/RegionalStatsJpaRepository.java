package ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RegionalStatsJpaRepository extends JpaRepository<RegionalStatsJpaEntity, String> {
    List<RegionalStatsJpaEntity> findByAcademicYear(String academicYear);
    Optional<RegionalStatsJpaEntity> findByRegionNameAndAcademicYear(String regionName, String academicYear);
    
    @Query("SELECT r FROM RegionalStatsJpaEntity r WHERE r.academicYear = :year ORDER BY r.studentCount DESC")
    List<RegionalStatsJpaEntity> findByAcademicYearOrderByStudentCountDesc(@Param("year") String year);
    
    @Query("SELECT SUM(r.studentCount) FROM RegionalStatsJpaEntity r WHERE r.academicYear = :year")
    Long totalStudentsByAcademicYear(@Param("year") String year);
}