package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Journal des modifications de notes d'une évaluation (création, modification, suppression ; verrouillage
 * et déverrouillage par l'administration). Écrit dans la même transaction que la note : pas de note sans trace.
 */
@Entity
@Table(name = "journal_notes_evaluations",
        indexes = @Index(name = "idx_journal_notes_eval", columnList = "evaluation_id"))
public class JournalNoteJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "evaluation_id", nullable = false)
    private String evaluationId;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    /** null pour un verrouillage / déverrouillage */
    @Column(name = "student_id")
    private String studentId;
    /** CREATION | MODIFICATION | SUPPRESSION | VERROUILLAGE | DEVERROUILLAGE */
    @Column(nullable = false, length = 20)
    private String action;
    @Column(name = "ancienne_valeur", length = 20)
    private String ancienneValeur;
    @Column(name = "nouvelle_valeur", length = 20)
    private String nouvelleValeur;
    @Column(name = "ancien_commentaire", length = 500)
    private String ancienCommentaire;
    @Column(name = "nouveau_commentaire", length = 500)
    private String nouveauCommentaire;
    @Column(name = "auteur_id")
    private String auteurId;
    @Column(name = "auteur_nom")
    private String auteurNom;
    @Column(nullable = false)
    private LocalDateTime horodatage;

    public JournalNoteJpaEntity() {}

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEvaluationId() { return evaluationId; }
    public void setEvaluationId(String evaluationId) { this.evaluationId = evaluationId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getAncienneValeur() { return ancienneValeur; }
    public void setAncienneValeur(String ancienneValeur) { this.ancienneValeur = ancienneValeur; }
    public String getNouvelleValeur() { return nouvelleValeur; }
    public void setNouvelleValeur(String nouvelleValeur) { this.nouvelleValeur = nouvelleValeur; }
    public String getAncienCommentaire() { return ancienCommentaire; }
    public void setAncienCommentaire(String ancienCommentaire) { this.ancienCommentaire = ancienCommentaire; }
    public String getNouveauCommentaire() { return nouveauCommentaire; }
    public void setNouveauCommentaire(String nouveauCommentaire) { this.nouveauCommentaire = nouveauCommentaire; }
    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String auteurId) { this.auteurId = auteurId; }
    public String getAuteurNom() { return auteurNom; }
    public void setAuteurNom(String auteurNom) { this.auteurNom = auteurNom; }
    public LocalDateTime getHorodatage() { return horodatage; }
    public void setHorodatage(LocalDateTime horodatage) { this.horodatage = horodatage; }
}
