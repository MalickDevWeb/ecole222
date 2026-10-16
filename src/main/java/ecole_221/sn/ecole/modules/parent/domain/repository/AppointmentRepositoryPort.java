package ecole_221.sn.ecole.modules.parent.domain.repository;

import ecole_221.sn.ecole.modules.parent.domain.model.Appointment;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepositoryPort {
    Appointment save(Appointment appointment);
    List<Appointment> findByParentId(String parentId);
    List<Appointment> findByProfesseurId(String professeurId);
    List<Appointment> findAll();
    Optional<Appointment> findById(String id);
}

