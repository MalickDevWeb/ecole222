package ecole_221.sn.ecole.modules.admin.application.service;

import org.springframework.stereotype.Service;
import ecole_221.sn.ecole.modules.admin.application.port.usecase.PlanningUseCase;
import ecole_221.sn.ecole.modules.admin.domain.model.*;
import ecole_221.sn.ecole.modules.admin.domain.port.repository.PlanningRepositoryPort;

import java.util.List;

@Service("adminPlanningService")
public class PlanningService implements PlanningUseCase {

    private final PlanningRepositoryPort repositoryPort;

    public PlanningService(PlanningRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public List<Room> getRooms() { return repositoryPort.getAllRooms(); }

    @Override
    public List<java.util.Map<String, Object>> updateRooms(List<Room> rooms) { return repositoryPort.saveRooms(rooms); }

    @Override
    public List<Filiere> getFilieres() { return repositoryPort.getAllFilieres(); }

    @Override
    public List<java.util.Map<String, Object>> updateFilieres(List<Filiere> filieres) { return repositoryPort.saveFilieres(filieres); }

    @Override
    public List<Classe> getClasses() { return repositoryPort.getAllClasses(); }

    @Override
    public List<java.util.Map<String, Object>> updateClasses(List<Classe> classes) { return repositoryPort.saveClasses(classes); }

    @Override
    public List<TimeSlot> getSlots() { return repositoryPort.getAllSlots(); }

    @Override
    public List<java.util.Map<String, Object>> updateSlots(List<TimeSlot> slots) { return repositoryPort.saveSlots(slots); }
}
