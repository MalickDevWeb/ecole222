package ecole_221.sn.ecole.modules.auth.domain.model;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Agrégat racine représentant un utilisateur global du système.
 * Constructeur privé pour garantir l'invariance - utiliser UserFactory.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements UserDetails {
    
    private final String id;
    private String nom;
    private String prenom;
    private final String email;
    private String password;
    private final String matricule;
    private RoleUtilisateur role;
    private String photoUrl;
    
    // Nouveaux champs pour User global (Migration V5)
    private String telephone;
    private Boolean emailVerified = false;
    private Boolean phoneVerified = false;
    private LocalDateTime lastLoginAt;
    private String language = "fr";
    private String timezone = "Africa/Dakar";
    
    // Constructeur privé
    private User(String id, String nom, String prenom, String email, String password, 
                String matricule, RoleUtilisateur role, String photoUrl, String telephone,
                Boolean emailVerified, Boolean phoneVerified, LocalDateTime lastLoginAt,
                String language, String timezone) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.nom = Objects.requireNonNull(nom, "Le nom ne peut pas être null");
        this.prenom = Objects.requireNonNull(prenom, "Le prénom ne peut pas être null");
        this.email = Objects.requireNonNull(email, "L'email ne peut pas être null");
        this.password = password;
        this.matricule = matricule != null ? matricule : "2026-EC-001";
        this.role = Objects.requireNonNull(role, "Le rôle ne peut pas être null");
        this.photoUrl = photoUrl;
        this.telephone = telephone;
        this.emailVerified = emailVerified != null ? emailVerified : false;
        this.phoneVerified = phoneVerified != null ? phoneVerified : false;
        this.lastLoginAt = lastLoginAt;
        this.language = language != null ? language : "fr";
        this.timezone = timezone != null ? timezone : "Africa/Dakar";
        
        // Validation de l'email
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email invalide");
        }
    }
    
    // Méthode package-private pour la Factory
    static User createInternal(String id, String nom, String prenom, String email, String password, 
                              String matricule, RoleUtilisateur role, String photoUrl, String telephone,
                              Boolean emailVerified, Boolean phoneVerified, LocalDateTime lastLoginAt,
                              String language, String timezone) {
        return new User(id, nom, prenom, email, password, matricule, role, photoUrl, telephone,
                      emailVerified, phoneVerified, lastLoginAt, language, timezone);
    }
    
    // Getters
    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getMatricule() { return matricule; }
    public RoleUtilisateur getRole() { return role; }
    public String getPhotoUrl() { return photoUrl; }
    
    // Nouveaux getters
    public String getTelephone() { return telephone; }
    public Boolean getEmailVerified() { return emailVerified; }
    public Boolean getPhoneVerified() { return phoneVerified; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public String getLanguage() { return language; }
    public String getTimezone() { return timezone; }
    
    // Setters (pour comportements métier)
    public void setNom(String nom) { 
        this.nom = Objects.requireNonNull(nom, "Le nom ne peut pas être null");
    }
    
    public void setPrenom(String prenom) { 
        this.prenom = Objects.requireNonNull(prenom, "Le prénom ne peut pas être null");
    }
    
    public void setPassword(String password) { 
        this.password = password;
    }
    
    public void setRole(RoleUtilisateur role) { 
        this.role = Objects.requireNonNull(role, "Le rôle ne peut pas être null");
    }
    
    public void setPhotoUrl(String photoUrl) { 
        this.photoUrl = photoUrl;
    }
    
    // Nouveaux setters
    public void setTelephone(String telephone) { 
        this.telephone = telephone;
    }
    
    public void setEmailVerified(Boolean emailVerified) { 
        this.emailVerified = emailVerified;
    }
    
    public void setPhoneVerified(Boolean phoneVerified) { 
        this.phoneVerified = phoneVerified;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) { 
        this.lastLoginAt = lastLoginAt;
    }
    
    public void setLanguage(String language) { 
        this.language = language;
    }
    
    public void setTimezone(String timezone) { 
        this.timezone = timezone;
    }
    
    // Comportements métier
    public void updateProfile(String nom, String prenom, String photoUrl) {
        this.nom = Objects.requireNonNull(nom, "Le nom ne peut pas être null");
        this.prenom = Objects.requireNonNull(prenom, "Le prénom ne peut pas être null");
        this.photoUrl = photoUrl;
    }
    
    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }
        this.password = newPassword;
    }
    
    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
    
    public void verifyEmail() {
        this.emailVerified = true;
    }
    
    public void verifyPhone() {
        this.phoneVerified = true;
    }
    
    public boolean isAdmin() {
        return role.equals(RoleUtilisateur.ADMIN) || role.equals(RoleUtilisateur.SUPER_ADMIN);
    }
    
    public boolean isStudent() {
        return role.equals(RoleUtilisateur.ETUDIANT);
    }
    
    public boolean isProfessor() {
        return role.equals(RoleUtilisateur.PROFESSEUR);
    }
    
    public boolean isFullyVerified() {
        return emailVerified && (telephone == null || phoneVerified);
    }
    
    // ==================== IMPLEMENTATION USERDETAILS ====================
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.value()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", matricule='" + matricule + '\'' +
                ", emailVerified=" + emailVerified +
                '}';
    }
}