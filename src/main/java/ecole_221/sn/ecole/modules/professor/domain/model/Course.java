package ecole_221.sn.ecole.modules.professor.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Agrégat racine représentant un cours.
 * Constructeur privé pour garantir l'invariance - utiliser CourseFactory.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Course {
    
    private final String id;
    private String name;
    private final String classe;
    private final int totalHours;
    private final String profId;
    private String code;
    private String description;
    
    // Constructeur privé
    private Course(String id, String name, String classe, int totalHours, String profId, 
                String code, String description) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        this.classe = Objects.requireNonNull(classe, "La classe ne peut pas être null");
        if (totalHours <= 0) {
            throw new IllegalArgumentException("Le nombre d'heures doit être positif");
        }
        this.totalHours = totalHours;
        this.profId = Objects.requireNonNull(profId, "L'ID professeur ne peut pas être null");
        this.code = code != null ? code : "DEFAULT-CODE";
        this.description = description != null ? description : "Description par défaut";
    }

    // Méthode package-private pour la Factory
    static Course createInternal(String id, String name, String classe, int totalHours, String profId, 
                               String code, String description) {
        return new Course(id, name, classe, totalHours, profId, code, description);
    }
    
    // Getters
    public String getId() { return id; }
    
    @JsonProperty("nom")
    public String getName() { return name; }
    
    @JsonProperty("classeId")
    public String getClasse() { return classe; }
    
    public int getTotalHours() { return totalHours; }
    
    public String getProfId() { return profId; }
    
    @JsonProperty("code")
    public String getCode() { return code; }
    
    @JsonProperty("description")
    public String getDescription() { return description; }
    
    // Comportements métier
    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
    }
    
    public void updateCode(String newCode) {
        this.code = Objects.requireNonNull(newCode, "Le code ne peut pas être null");
    }
    
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    public boolean isDefaultCode() {
        return "DEFAULT-CODE".equals(this.code);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(id, course.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Course{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", classe='" + classe + '\'' +
                ", totalHours=" + totalHours +
                ", profId='" + profId + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}