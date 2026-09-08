package ecole_221.sn.ecole.modules.auth.domain.model;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory pour créer des instances de User.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class UserFactory {
    
    /**
     * Crée un nouvel utilisateur avec un ID généré et un matricule par défaut.
     */
    public static User create(String nom, String prenom, String email, String password, RoleUtilisateur role) {
        String id = UUID.randomUUID().toString();
        return User.createInternal(id, nom, prenom, email, password, null, role, null, null, false, false, null, "fr", "Africa/Dakar");
    }
    
    /**
     * Crée un nouvel utilisateur avec un ID généré, matricule et photo URL.
     */
    public static User create(String nom, String prenom, String email, String password, 
                             String matricule, RoleUtilisateur role, String photoUrl) {
        String id = UUID.randomUUID().toString();
        return User.createInternal(id, nom, prenom, email, password, matricule, role, photoUrl, null, false, false, null, "fr", "Africa/Dakar");
    }
    
    /**
     * Crée un nouvel utilisateur avec tous les champs.
     */
    public static User create(String nom, String prenom, String email, String password, 
                             String matricule, RoleUtilisateur role, String photoUrl, String telephone,
                             Boolean emailVerified, Boolean phoneVerified, String language, String timezone) {
        String id = UUID.randomUUID().toString();
        return User.createInternal(id, nom, prenom, email, password, matricule, role, photoUrl, telephone,
                               emailVerified, phoneVerified, null, language, timezone);
    }
    
    /**
     * Reconstruit un utilisateur existant (pour la persistance).
     */
    public static User reconstruct(String id, String nom, String prenom, String email, 
                                  String password, String matricule, RoleUtilisateur role, 
                                  String photoUrl, String telephone, Boolean emailVerified, 
                                  Boolean phoneVerified, LocalDateTime lastLoginAt, String language, String timezone) {
        return User.createInternal(id, nom, prenom, email, password, matricule, role, photoUrl, telephone,
                               emailVerified, phoneVerified, lastLoginAt, language, timezone);
    }
    
    /**
     * Reconstruit un utilisateur existant (version simplifiée avec valeurs par défaut).
     */
    public static User reconstructSimple(String id, String nom, String prenom, String email, 
                                        String password, String matricule, RoleUtilisateur role, 
                                        String photoUrl) {
        return User.createInternal(id, nom, prenom, email, password, matricule, role, photoUrl, null,
                               false, false, null, "fr", "Africa/Dakar");
    }
}