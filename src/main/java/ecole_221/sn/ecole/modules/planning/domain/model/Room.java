package ecole_221.sn.ecole.modules.planning.domain.model;

import java.util.Objects;

/**
 * Agrégat racine représentant une salle de cours.
 * Constructeur package-private pour garantir l'invariance - utiliser RoomFactory.
 */
public class Room {
    
    private final String id;
    private String name;
    private final int capacity;
    private String equipment;
    private final String location;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    Room(String id, String name, int capacity, String equipment, String location) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacité doit être positive");
        }
        this.capacity = capacity;
        this.equipment = equipment;
        this.location = Objects.requireNonNull(location, "L'emplacement ne peut pas être null");
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public String getEquipment() { return equipment; }
    public String getLocation() { return location; }
    
    // Comportements métier
    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
    }
    
    public void updateEquipment(String newEquipment) {
        this.equipment = newEquipment;
    }
    
    public boolean hasEquipment(String requiredEquipment) {
        return this.equipment != null && this.equipment.contains(requiredEquipment);
    }
    
    public boolean isLargeRoom() {
        return this.capacity >= 50;
    }
    
    public boolean isSmallRoom() {
        return this.capacity <= 20;
    }
    
    public boolean isMediumRoom() {
        return this.capacity > 20 && this.capacity < 50;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return Objects.equals(id, room.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Room{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", capacity=" + capacity +
                ", location='" + location + '\'' +
                '}';
    }
}