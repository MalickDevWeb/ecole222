package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
// SEC-003 : tout compte créé dans le contexte d'une école en devient membre
@jakarta.persistence.EntityListeners(ecole_221.sn.ecole.shared.infrastructure.tenant.UserMembershipListener.class)
public class UserJpaEntity {

    @Id
    private String id;
    private String nom;
    private String prenom;

    @Column(unique = true)
    private String email;
    private String password;

    @Column(name = "role")
    private String role; // Stocké comme String (Value Object serialisé)
    
    @Column(name = "matricule")
    private String matricule;
    
    private String photoUrl;
    
    // Nouveaux champs pour User global (Migration V5)
    @Column(name = "telephone", unique = true)
    private String telephone;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "language")
    private String language = "fr";
    
    @Column(name = "timezone")
    private String timezone = "Africa/Dakar";

    /** Mot de passe provisoire (généré) à changer à la première connexion ; null (comptes existants) = false. */
    @Column(name = "doit_changer_mot_de_passe")
    private Boolean doitChangerMotDePasse;

    /** Identifiant National de l'Élève (INE) : facultatif, saisi par l'école, jamais généré (V16). */
    @Column(name = "ine", length = 30)
    private String ine;

    public String getIne() { return ine; }
    public void setIne(String ine) { this.ine = ine; }

    public UserJpaEntity() {}

    public UserJpaEntity(String id, String nom, String prenom, String email, String password, 
                        String role, String matricule, String photoUrl) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.matricule = matricule;
        this.photoUrl = photoUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    
    // Nouveaux getters/setters
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { 
        this.telephone = (telephone != null && !telephone.trim().isEmpty()) ? telephone.trim() : null; 
    }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public Boolean getPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(Boolean phoneVerified) { this.phoneVerified = phoneVerified; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Boolean getDoitChangerMotDePasse() { return doitChangerMotDePasse; }
    public void setDoitChangerMotDePasse(Boolean doitChangerMotDePasse) { this.doitChangerMotDePasse = doitChangerMotDePasse; }

    // ── V20 : date de la dernière modification du numéro de téléphone ──────────────────────────────────────────
    /** Un lien d'accès n'est pas envoyé à un numéro modifié depuis moins de 24 h sans l'accord d'un second administrateur. */
    @Column(name = "telephone_modifie_le")
    private java.time.LocalDateTime telephoneModifieLe;

    /** Numéro lu en base (comparé avant chaque mise à jour) ; null pour un compte pas encore enregistré. */
    @jakarta.persistence.Transient
    private String telephoneCharge;
    @jakarta.persistence.Transient
    private boolean charge;

    public java.time.LocalDateTime getTelephoneModifieLe() { return telephoneModifieLe; }
    public void setTelephoneModifieLe(java.time.LocalDateTime telephoneModifieLe) { this.telephoneModifieLe = telephoneModifieLe; }

    @jakarta.persistence.PostLoad
    void memoriserTelephone() {
        this.telephoneCharge = this.telephone;
        this.charge = true;
    }

    /** Toute modification du numéro d'un compte existant (ajout compris) est datée ; la création ne l'est pas. */
    @jakarta.persistence.PreUpdate
    void daterChangementTelephone() {
        if (charge && !java.util.Objects.equals(telephoneCharge, telephone)) {
            this.telephoneModifieLe = java.time.LocalDateTime.now();
            this.telephoneCharge = this.telephone;
        }
    }
}
