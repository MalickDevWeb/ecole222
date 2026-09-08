package ecole_221.sn.ecole.modules.planning.domain.model;

import ecole_221.sn.ecole.modules.planning.domain.valueobject.SlotType;

import java.util.Objects;

/**
 * Agrégat racine représentant un créneau horaire.
 * Constructeur package-private pour garantir l'invariance - utiliser SlotFactory.
 */
public class Slot {
    
    private final String id;
    private final String startTime;
    private final String endTime;
    private final String dayOfWeek;
    private SlotType type;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    Slot(String id, String startTime, String endTime, String dayOfWeek, SlotType type) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.startTime = Objects.requireNonNull(startTime, "L'heure de début ne peut pas être null");
        this.endTime = Objects.requireNonNull(endTime, "L'heure de fin ne peut pas être null");
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "Le jour ne peut pas être null");
        this.type = Objects.requireNonNull(type, "Le type ne peut pas être null");
        
        // Validation du format horaire
        if (!startTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new IllegalArgumentException("Format d'heure de début invalide (HH:MM)");
        }
        if (!endTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new IllegalArgumentException("Format d'heure de fin invalide (HH:MM)");
        }
    }
    
    // Getters
    public String getId() { return id; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getDayOfWeek() { return dayOfWeek; }
    public SlotType getType() { return type; }
    
    // Comportements métier
    public void updateType(SlotType newType) {
        this.type = Objects.requireNonNull(newType, "Le type ne peut pas être null");
    }
    
    public boolean isCM() {
        return this.type.equals(SlotType.CM);
    }
    
    public boolean isTD() {
        return this.type.equals(SlotType.TD);
    }
    
    public boolean isTP() {
        return this.type.equals(SlotType.TP);
    }
    
    public boolean isExam() {
        return this.type.equals(SlotType.EXAM);
    }
    
    public boolean isProject() {
        return this.type.equals(SlotType.PROJECT);
    }
    
    public int getDurationInMinutes() {
        String[] start = startTime.split(":");
        String[] end = endTime.split(":");
        int startMinutes = Integer.parseInt(start[0]) * 60 + Integer.parseInt(start[1]);
        int endMinutes = Integer.parseInt(end[0]) * 60 + Integer.parseInt(end[1]);
        return endMinutes - startMinutes;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Slot slot = (Slot) o;
        return Objects.equals(id, slot.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Slot{" +
                "id='" + id + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", dayOfWeek='" + dayOfWeek + '\'' +
                ", type=" + type +
                '}';
    }
}