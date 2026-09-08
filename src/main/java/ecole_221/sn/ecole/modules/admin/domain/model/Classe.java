package ecole_221.sn.ecole.modules.admin.domain.model;

import java.util.Objects;

/**
 * Agrégat racine représentant une classe.
 * Constructeur package-private pour garantir l'invariance - utiliser ClasseFactory.
 */
public class Classe {
    
    private final String id;
    private String name;
    private final String filiereId;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    Classe(String id, String name, String filiereId) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        this.filiereId = Objects.requireNonNull(filiereId, "L'ID filière ne peut pas être null");
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getFiliereId() { return filiereId; }
    
    // Comportements métier
    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
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
                ", filiereId='" + filiereId + '\'' +
                '}';
    }
}