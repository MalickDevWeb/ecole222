package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Demande de report d'une séance faite par son professeur, validée ou refusée par l'administration (lot E). */
@Entity
@Table(name = "demandes_report")
public class DemandeReportJpaEntity {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String ACCEPTEE = "ACCEPTEE";
    public static final String REFUSEE = "REFUSEE";

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    @Column(name = "course_id")
    private String courseId;
    @Column(name = "course_title")
    private String courseTitle;
    @Column(name = "classe")
    private String classe;
    @Column(name = "professor_id", nullable = false)
    private String professorId;
    /** Créneau au moment de la demande. */
    @Column(name = "jour_actuel")
    private String jourActuel;
    @Column(name = "horaire_actuel")
    private String horaireActuel;
    @Column(name = "salle_actuelle")
    private String salleActuelle;
    /** Créneau demandé. */
    @Column(name = "jour", nullable = false)
    private String jour;
    @Column(name = "horaire", nullable = false)
    private String horaire;
    @Column(name = "salle", nullable = false)
    private String salle;
    @Column(name = "motif", length = 1000)
    private String motif;
    @Column(name = "statut", nullable = false)
    private String statut;
    @Column(name = "motif_refus", length = 1000)
    private String motifRefus;
    @Column(name = "decide_par")
    private String decidePar;
    @Column(name = "decide_le")
    private LocalDateTime decideLe;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getClasse() { return classe; }
    public void setClasse(String classe) { this.classe = classe; }
    public String getProfessorId() { return professorId; }
    public void setProfessorId(String professorId) { this.professorId = professorId; }
    public String getJourActuel() { return jourActuel; }
    public void setJourActuel(String jourActuel) { this.jourActuel = jourActuel; }
    public String getHoraireActuel() { return horaireActuel; }
    public void setHoraireActuel(String horaireActuel) { this.horaireActuel = horaireActuel; }
    public String getSalleActuelle() { return salleActuelle; }
    public void setSalleActuelle(String salleActuelle) { this.salleActuelle = salleActuelle; }
    public String getJour() { return jour; }
    public void setJour(String jour) { this.jour = jour; }
    public String getHoraire() { return horaire; }
    public void setHoraire(String horaire) { this.horaire = horaire; }
    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }
    public String getDecidePar() { return decidePar; }
    public void setDecidePar(String decidePar) { this.decidePar = decidePar; }
    public LocalDateTime getDecideLe() { return decideLe; }
    public void setDecideLe(LocalDateTime decideLe) { this.decideLe = decideLe; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
