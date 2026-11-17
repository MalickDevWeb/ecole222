package ecole_221.sn.ecole.modules.admin.domain.port.repository;

import ecole_221.sn.ecole.modules.admin.domain.model.*;
import java.util.List;

public interface PlanningRepositoryPort {
    List<Room> getAllRooms();
    /** @return les lignes absentes de la liste, mises à la corbeille : {type, id, libelle} */
    List<java.util.Map<String, Object>> saveRooms(List<Room> rooms);

    List<Filiere> getAllFilieres();
    /** @return les lignes absentes de la liste, mises à la corbeille : {type, id, libelle} */
    List<java.util.Map<String, Object>> saveFilieres(List<Filiere> filieres);

    List<Classe> getAllClasses();
    /** @return les lignes absentes de la liste, mises à la corbeille : {type, id, libelle} */
    List<java.util.Map<String, Object>> saveClasses(List<Classe> classes);

    List<TimeSlot> getAllSlots();
    /** @return les lignes absentes de la liste, mises à la corbeille : {type, id, libelle} */
    List<java.util.Map<String, Object>> saveSlots(List<TimeSlot> slots);
}
