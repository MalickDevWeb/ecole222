package ecole_221.sn.ecole.modules.parent.domain.model;

import java.time.LocalDateTime;

/**
 * Modèle de domaine représentant un parent.
 * Immuteable - les modifications passent par des méthodes de factory.
 */
public class Parent {
    private final String id;
    private final String userId;
    private final String nom;
    private final String prenom;
    private final String email;
    private final String telephone;
    private final String adresse;
    private final String profession;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Parent(String id, String userId, String nom, String prenom, String email,
                   String telephone, String adresse, String profession,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.adresse = adresse;
        this.profession = profession;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Factory method for creating a new Parent
    public static Parent create(String userId, String nom, String prenom, String email,
                                String telephone, String adresse, String profession) {
        LocalDateTime now = LocalDateTime.now();
        return new Parent(
            java.util.UUID.randomUUID().toString(),
            userId, nom, prenom, email, telephone, adresse, profession,
            now, now
        );
    }

    // Factory method for reconstituting from database
    public static Parent reconstitute(String id, String userId, String nom, String prenom, 
                                     String email, String telephone, String adresse, 
                                     String profession, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Parent(id, userId, nom, prenom, email, telephone, adresse, profession, 
                         createdAt, updatedAt);
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getTelephone() { return telephone; }
    public String getAdresse() { return adresse; }
    public String getProfession() { return profession; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getFullName() {
        return (nom != null ? nom : "") + " " + (prenom != null ? prenom : "");
    }
}
