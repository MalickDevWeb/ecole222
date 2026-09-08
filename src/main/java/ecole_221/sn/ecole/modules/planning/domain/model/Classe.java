package ecole_221.sn.ecole.modules.planning.domain.model;

import java.util.Objects;

/**
 * Agrégat racine représentant une classe.
 * Constructeur package-private pour garantir l'invariance - utiliser ClasseFactory.
 */
public class Classe {
    
    private final String id;
    private String name;
    private String code;
    private final String filiereId;
    private final String level;
    private final int capacity;
    private int currentSize;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    Classe(String id, String name, String code, String filiereId, String level, int capacity, int currentSize) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        this.code = code;
        this.filiereId = Objects.requireNonNull(filiereId, "L'ID filière ne peut pas être null");
        this.level = Objects.requireNonNull(level, "Le niveau ne peut pas être null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacité doit être positive");
        }
        this.capacity = capacity;
        if (currentSize < 0) {
            throw new IllegalArgumentException("La taille actuelle ne peut pas être négative");
        }
        this.currentSize = currentSize;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getFiliereId() { return filiereId; }
    public String getLevel() { return level; }
    public int getCapacity() { return capacity; }
    public int getCurrentSize() { return currentSize; }
    
    // Comportements métier
    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
    }
    
    public void updateCode(String newCode) {
        this.code = newCode;
    }
    
    public void addStudent() {
        if (this.currentSize < this.capacity) {
            this.currentSize++;
        } else {
            throw new IllegalStateException("La classe est pleine");
        }
    }
    
    public void removeStudent() {
        if (this.currentSize > 0) {
            this.currentSize--;
        }
    }
    
    public boolean isFull() {
        return this.currentSize >= this.capacity;
    }
    
    public boolean hasAvailableSeats() {
        return this.currentSize < this.capacity;
    }
    
    public int getAvailableSeats() {
        return this.capacity - this.currentSize;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Classe classe = (Classe) o;
        return Objects.equals(id, classe.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Classe{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", filiereId='" + filiereId + '\'' +
                ", level='" + level + '\'' +
                ", capacity=" + capacity +
                ", currentSize=" + currentSize +
                '}';
    }
}