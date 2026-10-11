package ecole_221.sn.ecole.modules.auth.domain.model;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.TenantProfile;
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
    private RoleUtilisateur globalRole;
    private List<TenantProfile> tenantProfiles = new ArrayList<>();
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
                String matricule, RoleUtilisateur globalRole, List<TenantProfile> tenantProfiles, String photoUrl, String telephone,
                Boolean emailVerified, Boolean phoneVerified, LocalDateTime lastLoginAt,
                String language, String timezone) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.nom = Objects.requireNonNull(nom, "Le nom ne peut pas être null");
        this.prenom = Objects.requireNonNull(prenom, "Le prénom ne peut pas être null");
        this.email = Objects.requireNonNull(email, "L'email ne peut pas être null");
        this.password = password;
        this.matricule = matricule != null ? matricule : "2026-EC-001";
        this.globalRole = globalRole;
        this.tenantProfiles = tenantProfiles != null ? new ArrayList<>(tenantProfiles) : new ArrayList<>();
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
                              String matricule, RoleUtilisateur globalRole, List<TenantProfile> tenantProfiles, String photoUrl, String telephone,
                              Boolean emailVerified, Boolean phoneVerified, LocalDateTime lastLoginAt,
                              String language, String timezone) {
        return new User(id, nom, prenom, email, password, matricule, globalRole, tenantProfiles, photoUrl, telephone,
                      emailVerified, phoneVerified, lastLoginAt, language, timezone);
    }
    
    // Getters
    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getMatricule() { return matricule; }
    public RoleUtilisateur getGlobalRole() { return globalRole; }
    public List<TenantProfile> getTenantProfiles() { return List.copyOf(tenantProfiles); }
    public String getPhotoUrl() { return photoUrl; }
    public RoleUtilisateur getRole() { return getContextualRole(); }
    
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
    
    public void setGlobalRole(RoleUtilisateur globalRole) { 
        this.globalRole = globalRole;
    }
    
    public void addTenantProfile(TenantProfile profile) {
        if (!this.tenantProfiles.contains(profile)) {
            this.tenantProfiles.add(profile);
        }
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
    
    public boolean isFullyVerified() {
        return emailVerified && (telephone == null || phoneVerified);
    }
    
    // Utilitaires de rôle contextuel
    public RoleUtilisateur getContextualRole() {
        if (ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.hasTenant()) {
            String tenantId = ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId().orElse(null);
            return tenantProfiles.stream()
                    .filter(p -> p.getTenantId().equals(tenantId))
                    .map(TenantProfile::getRole)
                    .findFirst()
                    .orElse(globalRole != null ? globalRole : RoleUtilisateur.VISITEUR); // Fallback
        }
        return globalRole != null ? globalRole : RoleUtilisateur.VISITEUR;
    }
    
    public boolean isAdmin() {
        RoleUtilisateur r = getContextualRole();
        return r.equals(RoleUtilisateur.ADMIN) || r.equals(RoleUtilisateur.ADMINISTRATOR) || r.equals(RoleUtilisateur.SUPER_ADMIN) || r.equals(RoleUtilisateur.PLATFORM_ADMIN);
    }
    
    public boolean isStudent() {
        RoleUtilisateur r = getContextualRole();
        return r.equals(RoleUtilisateur.ETUDIANT) || r.equals(RoleUtilisateur.STUDENT);
    }
    
    public boolean isProfessor() {
        RoleUtilisateur r = getContextualRole();
        return r.equals(RoleUtilisateur.PROFESSEUR) || r.equals(RoleUtilisateur.TEACHER);
    }
    
    // ==================== IMPLEMENTATION USERDETAILS ====================
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        RoleUtilisateur r = getContextualRole();
        return List.of(new SimpleGrantedAuthority("ROLE_" + r.value()));
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
                ", globalRole=" + globalRole +
                ", tenantProfilesCount=" + tenantProfiles.size() +
                ", matricule='" + matricule + '\'' +
                ", emailVerified=" + emailVerified +
                '}';
    }
}