package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import ecole_221.sn.ecole.modules.parent.domain.model.Appointment;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AppointmentRepositoryAdapter implements AppointmentRepositoryPort {

    private final AppointmentJpaRepository jpaRepository;

    public AppointmentRepositoryAdapter(AppointmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentJpaEntity entity = AppointmentJpaEntity.fromDomain(appointment);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public List<Appointment> findByParentId(String parentId) {
        return jpaRepository.findByParentIdOrderByCreatedAtDesc(parentId).stream()
                .map(AppointmentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findByProfesseurId(String professeurId) {
        return jpaRepository.findByProfesseurIdOrderByCreatedAtDesc(professeurId).stream()
                .map(AppointmentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AppointmentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return jpaRepository.findById(id).map(AppointmentJpaEntity::toDomain);
    }
}

