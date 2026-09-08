package ecole_221.sn.ecole.modules.planning.domain.model;

import java.util.Objects;

/**
 * Agrégat racine représentant une filière.
 * Constructeur package-private pour garantir l'invariance - utiliser FiliereFactory.
 */
public class Filiere {
    
    private final String id;
    private String name;
    private String code;
    private String description;
    private final String level;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    Filiere(String id, String name, String code, String description, String level) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        this.code = code;
        this.description = description;
        this.level = Objects.requireNonNull(level, "Le niveau ne peut pas être null");
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getLevel() { return level; }
    
    // Comportements métier
    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
    }
    
    public void updateCode(String newCode) {
        this.code = newCode;
    }
    
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Filiere filiere = (Filiere) o;
        return Objects.equals(id, filiere.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Filiere{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", level='" + level + '\'' +
                '}';
    }
}