package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import ecole_221.sn.ecole.modules.student.domain.exception.AttendanceNotFoundException;
import ecole_221.sn.ecole.modules.student.domain.model.Attendance;
import ecole_221.sn.ecole.modules.student.domain.model.AttendanceFactory;
import ecole_221.sn.ecole.modules.student.domain.port.repository.AttendanceRepositoryPort;
import ecole_221.sn.ecole.modules.student.domain.valueobject.AttendanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour l'agrégat Attendance.
 * Implémente AttendanceRepositoryPort en utilisant AttendanceJpaRepository.
 */
@Component
public class AttendanceRepositoryAdapter implements AttendanceRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(AttendanceRepositoryAdapter.class);
    
    private final AttendanceJpaRepository attendanceJpaRepository;
    
    public AttendanceRepositoryAdapter(AttendanceJpaRepository attendanceJpaRepository) {
        this.attendanceJpaRepository = attendanceJpaRepository;
    }
    
    @Override
    public Attendance save(Attendance attendance) {
        logger.debug("Sauvegarde de la présence: {}", attendance.getId());
        AttendanceJpaEntity entity = toAttendanceEntity(attendance);
        AttendanceJpaEntity savedEntity = attendanceJpaRepository.save(entity);
        return toAttendance(savedEntity);
    }
    
    @Override
    public Optional<Attendance> findById(String id) {
        logger.debug("Récupération de la présence: {}", id);
        return attendanceJpaRepository.findById(id)
            .map(this::toAttendance);
    }
    
    @Override
    public List<Attendance> findAll() {
        logger.debug("Récupération de toutes les présences");
        return attendanceJpaRepository.findAll().stream()
            .map(this::toAttendance)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        logger.debug("Suppression de la présence: {}", id);
        if (!attendanceJpaRepository.existsById(id)) {
            throw new AttendanceNotFoundException(id);
        }
        attendanceJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Attendance> findByStudentId(String studentId) {
        logger.debug("Récupération des présences par étudiant: {}", studentId);
        return attendanceJpaRepository.findByStudentId(studentId).stream()
            .map(this::toAttendance)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Attendance> findByCourseId(String courseId) {
        logger.debug("Récupération des présences par cours: {}", courseId);
        return attendanceJpaRepository.findByCourseId(courseId).stream()
            .map(this::toAttendance)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Attendance> findByDate(LocalDate date) {
        logger.debug("Récupération des présences par date: {}", date);
        return attendanceJpaRepository.findByDate(date.toString()).stream()
            .map(this::toAttendance)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Attendance> findByStatus(AttendanceStatus status) {
        logger.debug("Récupération des présences par statut: {}", status);
        return attendanceJpaRepository.findByStatus(status.value()).stream()
            .map(this::toAttendance)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Attendance> findByStudentIdAndCourseId(String studentId, String courseId) {
        logger.debug("Récupération des présences par étudiant et cours: {} - {}", studentId, courseId);
        return attendanceJpaRepository.findByStudentIdAndCourseId(studentId, courseId).stream()
            .map(this::toAttendance)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Attendance toAttendance(AttendanceJpaEntity entity) {
        return AttendanceFactory.reconstruct(
            entity.getId(),
            LocalDate.parse(entity.getDate()),
            entity.getStudentId(),
            entity.getStudentName(),
            new AttendanceStatus(entity.getStatus()),
            entity.getCourseId()
        );
    }
    
    private AttendanceJpaEntity toAttendanceEntity(Attendance attendance) {
        AttendanceJpaEntity entity = new AttendanceJpaEntity();
        entity.setId(attendance.getId());
        entity.setDate(attendance.getDate().toString());
        entity.setStudentId(attendance.getStudentId());
        entity.setStudentName(attendance.getStudentName());
        entity.setStatus(attendance.getStatus().value());
        entity.setCourseId(attendance.getCourseId());
        return entity;
    }
}