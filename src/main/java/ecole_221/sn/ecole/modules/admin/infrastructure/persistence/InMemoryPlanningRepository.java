package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import ecole_221.sn.ecole.modules.admin.domain.model.*;
import ecole_221.sn.ecole.modules.admin.domain.port.repository.PlanningRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryPlanningRepository implements PlanningRepositoryPort {

    private final List<Room> rooms = new CopyOnWriteArrayList<>();
    private final List<Filiere> filieres = new CopyOnWriteArrayList<>();
    private final List<Classe> classes = new CopyOnWriteArrayList<>();
    private final List<TimeSlot> slots = new CopyOnWriteArrayList<>();

    public InMemoryPlanningRepository() {
        rooms.add(RoomFactory.reconstruct("room-1", "Salle 101", 30));
        rooms.add(RoomFactory.reconstruct("room-2", "Salle 102", 40));
        rooms.add(RoomFactory.reconstruct("room-3", "Amphi A", 120));

        filieres.add(FiliereFactory.reconstruct("filiere-1", "GL", "Génie Logiciel"));
        filieres.add(FiliereFactory.reconstruct("filiere-2", "RI", "Réseaux et Télécoms"));

        classes.add(ClasseFactory.reconstruct("class-1", "L3 GL", "filiere-1"));
        classes.add(ClasseFactory.reconstruct("class-2", "M1 GL", "filiere-1"));

        slots.add(TimeSlotFactory.reconstruct("slot-1", "Lundi", "08:00", "11:00", "Cours", "CM", "Professeur", "Salle", "Description", "class-1"));
        slots.add(TimeSlotFactory.reconstruct("slot-2", "Lundi", "11:00", "14:00", "Cours", "TD", "Professeur", "Salle", "Description", "class-2"));
        slots.add(TimeSlotFactory.reconstruct("slot-3", "Mardi", "08:00", "11:00", "Cours", "TP", "Professeur", "Salle", "Description", "class-1"));
    }

    @Override
    public List<Room> getAllRooms() { return new ArrayList<>(rooms); }

    @Override
    public void saveRooms(List<Room> rooms) {
        this.rooms.clear();
        this.rooms.addAll(rooms);
    }

    @Override
    public List<Filiere> getAllFilieres() { return new ArrayList<>(filieres); }

    @Override
    public void saveFilieres(List<Filiere> filieres) {
        this.filieres.clear();
        this.filieres.addAll(filieres);
    }

    @Override
    public List<Classe> getAllClasses() { return new ArrayList<>(classes); }

    @Override
    public void saveClasses(List<Classe> classes) {
        this.classes.clear();
        this.classes.addAll(classes);
    }

    @Override
    public List<TimeSlot> getAllSlots() { return new ArrayList<>(slots); }

    @Override
    public void saveSlots(List<TimeSlot> slots) {
        this.slots.clear();
        this.slots.addAll(slots);
    }
}
