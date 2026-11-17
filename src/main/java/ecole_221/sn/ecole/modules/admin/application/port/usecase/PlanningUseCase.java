package ecole_221.sn.ecole.modules.admin.application.port.usecase;

import ecole_221.sn.ecole.modules.admin.domain.model.*;
import java.util.List;

public interface PlanningUseCase {
    List<Room> getRooms();
    /** @return les lignes absentes de la liste, mises à la corbeille : {type, id, libelle} */
    List<java.util.Map<String, Object>> updateRooms(List<Room> rooms);

    List<Filiere> getFilieres();
    /** @return les lignes absentes de la liste, mises à la corbeille : {type, id, libelle} */
    List<java.util.Map<String, Object>> updateFilieres(List<Filiere> filieres);

    List<Classe> getClasses();
    /** @return les lignes absentes de la liste, mises à la corbeille : {type, id, libelle} */
    List<java.util.Map<String, Object>> updateClasses(List<Classe> classes);

    List<TimeSlot> getSlots();
    /** @return les lignes absentes de la liste, mises à la corbeille : {type, id, libelle} */
    List<java.util.Map<String, Object>> updateSlots(List<TimeSlot> slots);
}
