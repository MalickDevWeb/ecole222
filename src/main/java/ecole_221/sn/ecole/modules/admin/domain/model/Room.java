package ecole_221.sn.ecole.modules.admin.domain.model;

import java.util.Objects;

/**
 * Agrégat racine représentant une salle.
 * Constructeur package-private pour garantir l'invariance - utiliser RoomFactory.
 */
public class Room {
    
    private final String id;
    private String name;
    private final int capacity;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    Room(String id, String name, int capacity) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacité doit être positive");
        }
        this.capacity = capacity;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    
    // Comportements métier
    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
    }
    
    public boolean canAccommodate(int requiredCapacity) {
        return this.capacity >= requiredCapacity;
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
                '}';
    }
}