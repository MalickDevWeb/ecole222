package ecole_221.sn.ecole.modules.admin.domain.port.repository;

import ecole_221.sn.ecole.modules.admin.domain.model.*;
import java.util.List;

public interface PlanningRepositoryPort {
    List<Room> getAllRooms();
    void saveRooms(List<Room> rooms);

    List<Filiere> getAllFilieres();
    void saveFilieres(List<Filiere> filieres);

    List<Classe> getAllClasses();
    void saveClasses(List<Classe> classes);

    List<TimeSlot> getAllSlots();
    void saveSlots(List<TimeSlot> slots);
}
