package ecole_221.sn.ecole.modules.auth.application.port.dto;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;

public class LoginResponseDto {
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private RoleUtilisateur role;
    private String token;
    private String tenantSlug;

    public LoginResponseDto() {}

    public LoginResponseDto(String id, String nom, String prenom, String email, RoleUtilisateur role, String token) {
        this(id, nom, prenom, email, role, token, null);
    }

    public LoginResponseDto(String id, String nom, String prenom, String email, RoleUtilisateur role, String token, String tenantSlug) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
        this.token = token;
        this.tenantSlug = tenantSlug;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public RoleUtilisateur getRole() { return role; }
    public void setRole(RoleUtilisateur role) { this.role = role; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTenantSlug() { return tenantSlug; }
    public void setTenantSlug(String tenantSlug) { this.tenantSlug = tenantSlug; }
}
