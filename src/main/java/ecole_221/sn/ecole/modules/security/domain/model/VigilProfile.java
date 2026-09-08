package ecole_221.sn.ecole.modules.security.domain.model;

/**
 * Modèle de domaine représentant un profil de vigilance.
 */
public class VigilProfile {
    private final String id;
    private final String name;
    private final String role;
    private final String userId;
    private final String status;
    private final String createdAt;
    
    private VigilProfile(String id, String name, String role, String userId, String status, String createdAt) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Méthode package-private pour la Factory
    static VigilProfile createInternal(String id, String name, String role, String userId, String status, String createdAt) {
        return new VigilProfile(id, name, role, userId, status, createdAt);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getUserId() { return userId; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}