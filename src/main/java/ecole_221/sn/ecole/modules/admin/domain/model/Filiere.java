package ecole_221.sn.ecole.modules.admin.domain.model;

import java.util.Objects;

/**
 * Agrégat racine représentant une filière.
 * Constructeur package-private pour garantir l'invariance - utiliser FiliereFactory.
 */
public class Filiere {
    
    private final String id;
    private String code;
    private String name;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    Filiere(String id, String code, String name) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.code = Objects.requireNonNull(code, "Le code ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
    }
    
    // Getters
    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    
    // Comportements métier
    public void updateCode(String newCode) {
        this.code = Objects.requireNonNull(newCode, "Le code ne peut pas être null");
    }
    
    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
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
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}