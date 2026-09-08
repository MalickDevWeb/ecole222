package ecole_221.sn.ecole.modules.admin.application.port.usecase;

import ecole_221.sn.ecole.modules.admin.domain.model.*;
import java.util.List;

public interface PlanningUseCase {
    List<Room> getRooms();
    void updateRooms(List<Room> rooms);

    List<Filiere> getFilieres();
    void updateFilieres(List<Filiere> filieres);

    List<Classe> getClasses();
    void updateClasses(List<Classe> classes);

    List<TimeSlot> getSlots();
    void updateSlots(List<TimeSlot> slots);
}
