package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Accès aux présences (table attendances, filtrée par école via @TenantId) pour la feuille d'appel par séance.
 * Dépôt distinct d'AttendanceJpaRepository pour ne pas modifier l'interface partagée de l'espace élève.
 */
@Repository
public interface PresenceSeanceJpaRepository extends JpaRepository<AttendanceJpaEntity, String> {
    List<AttendanceJpaEntity> findByCourseIdAndDate(String courseId, String date);
    List<AttendanceJpaEntity> findByCourseId(String courseId);
}
