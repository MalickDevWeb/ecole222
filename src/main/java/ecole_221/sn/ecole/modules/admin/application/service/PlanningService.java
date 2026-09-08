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
    public void updateRooms(List<Room> rooms) { repositoryPort.saveRooms(rooms); }

    @Override
    public List<Filiere> getFilieres() { return repositoryPort.getAllFilieres(); }

    @Override
    public void updateFilieres(List<Filiere> filieres) { repositoryPort.saveFilieres(filieres); }

    @Override
    public List<Classe> getClasses() { return repositoryPort.getAllClasses(); }

    @Override
    public void updateClasses(List<Classe> classes) { repositoryPort.saveClasses(classes); }

    @Override
    public List<TimeSlot> getSlots() { return repositoryPort.getAllSlots(); }

    @Override
    public void updateSlots(List<TimeSlot> slots) { repositoryPort.saveSlots(slots); }
}
