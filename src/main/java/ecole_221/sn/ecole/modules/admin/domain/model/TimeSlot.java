package ecole_221.sn.ecole.modules.admin.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Agrégat racine représentant un créneau horaire.
 * Constructeur package-private pour garantir l'invariance - utiliser TimeSlotFactory.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSlot {
    
    private final String id;
    private final String day;
    private final String heureDebut;
    private final String heureFin;
    private String nom;
    private final String type;
    private final String professeur;
    private final String salle;
    private String description;
    private final String classeId;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    TimeSlot(String id, String day, String heureDebut, String heureFin, String nom, 
                   String type, String professeur, String salle, String description, String classeId) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.day = Objects.requireNonNull(day, "Le jour ne peut pas être null");
        this.heureDebut = Objects.requireNonNull(heureDebut, "L'heure de début ne peut pas être null");
        this.heureFin = Objects.requireNonNull(heureFin, "L'heure de fin ne peut pas être null");
        this.nom = nom != null ? nom : "Cours";
        this.type = type != null ? type : "CM";
        this.professeur = professeur != null ? professeur : "Professeur";
        this.salle = salle != null ? salle : "Salle";
        this.description = description;
        this.classeId = Objects.requireNonNull(classeId, "L'ID classe ne peut pas être null");
    }
    
    // Getters
    public String getId() { return id; }
    
    @JsonProperty("jour")
    public String getDay() { return day; }
    
    @JsonProperty("heureDebut")
    public String getHeureDebut() { return heureDebut; }
    
    @JsonProperty("heureFin")
    public String getHeureFin() { return heureFin; }
    
    @JsonProperty("nom")
    public String getNom() { return nom; }
    
    @JsonProperty("type")
    public String getType() { return type; }
    
    @JsonProperty("professeur")
    public String getProfesseur() { return professeur; }
    
    @JsonProperty("salle")
    public String getSalle() { return salle; }
    
    @JsonProperty("description")
    public String getDescription() { return description; }
    
    @JsonProperty("classeId")
    public String getClasseId() { return classeId; }
    
    @JsonProperty("heureStr")
    public String getHeureStr() {
        return this.heureDebut + " - " + this.heureFin;
    }
    
    // Comportements métier
    public void updateNom(String newNom) {
        this.nom = Objects.requireNonNull(newNom, "Le nom ne peut pas être null");
    }
    
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    public boolean isCM() {
        return "CM".equals(this.type);
    }
    
    public boolean isTD() {
        return "TD".equals(this.type);
    }
    
    public boolean isTP() {
        return "TP".equals(this.type);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(id, timeSlot.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "TimeSlot{" +
                "id='" + id + '\'' +
                ", day='" + day + '\'' +
                ", heureDebut='" + heureDebut + '\'' +
                ", heureFin='" + heureFin + '\'' +
                ", nom='" + nom + '\'' +
                ", type='" + type + '\'' +
                ", classeId='" + classeId + '\'' +
                '}';
    }
}