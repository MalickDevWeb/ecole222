package ecole_221.sn.ecole.modules.student.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * DTO représentant le profil d'un étudiant.
 */
@Schema(description = "Profil d'un étudiant")
public record StudentProfileDto(
    @Schema(description = "Identifiant unique de l'étudiant", example = "usr-001")
    String id,
    
    @Schema(description = "Nom de famille", example = "Sall")
    String nom,
    
    @Schema(description = "Prénom", example = "Mamadou")
    String prenom,
    
    @Schema(description = "Adresse email", example = "etudiant221@gmail.com")
    String email,
    
    @Schema(description = "Matricule étudiant", example = "SN-2026-1234")
    String matricule,
    
    @Schema(description = "Date de naissance")
    LocalDate dateNaissance,
    
    @Schema(description = "Numéro de téléphone", example = "+221771234567")
    String telephone,
    
    @Schema(description = "Adresse complète")
    String adresse,
    
    @Schema(description = "Nom de la filière", example = "Genie Logiciel")
    String filiere,
    
    @Schema(description = "Niveau d'étude", example = "Licence 3")
    String niveau,
    
    @Schema(description = "URL de la photo de profil")
    String photoUrl
) {
    public StudentProfileDto {
        // Compact constructor - can add validation here
    }
}
