package ecole_221.sn.ecole.modules.auth.application.port.dto;

/**
 * DTO pour représenter un utilisateur dans l'API.
 * Simple POJO sans logique métier.
 */
public class UserDto {
    
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String matricule;
    private String photoUrl;
    
    public UserDto() {}
    
    public UserDto(String id, String nom, String prenom, String email, 
                  String role, String matricule, String photoUrl) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
        this.matricule = matricule;
        this.photoUrl = photoUrl;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}