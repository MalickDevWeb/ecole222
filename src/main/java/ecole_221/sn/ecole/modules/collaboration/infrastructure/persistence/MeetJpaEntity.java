package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "meets")
@org.hibernate.annotations.SQLRestriction("supprime_le is null")   // corbeille (V21) : ligne supprimée invisible partout
public class MeetJpaEntity extends ecole_221.sn.ecole.partage.ColonnesCorbeille {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    private String title;
    private String link;
    private String time;
    private Boolean active;
    // APP-BE-14 : champs de la classe virtuelle envoyés par le frontend
    @jakarta.persistence.Column(name = "subject_name")
    private String subjectName;
    @jakarta.persistence.Column(name = "teacher_name")
    private String teacherName;
    @jakarta.persistence.Column(name = "meet_date")
    private String meetDate;
    @jakarta.persistence.Column(name = "class_ids", length = 2000)
    private String classIds;      // identifiants séparés par des virgules
    @jakarta.persistence.Column(name = "class_names", length = 2000)
    private String classNames;
    private Boolean published;
    /** V22 : compte qui a lancé la séance (null pour les séances antérieures). */
    @jakarta.persistence.Column(name = "auteur_id")
    private String auteurId;
    /** V22 : cours de l'enseignant auquel la séance est rattachée, s'il est identifiable sans ambiguïté. */
    @jakarta.persistence.Column(name = "course_id")
    private String courseId;
    /** V26 : heure à partir de laquelle le lien est montré aux élèves et aux parents. */
    private java.time.LocalDateTime debut;
    /** V26 : heure après laquelle la classe virtuelle n'est plus montrée aux élèves et aux parents. */
    private java.time.LocalDateTime fin;
    // APP-TL-26 : champ « compte Google obligatoire » retiré (enregistré sans aucun effet). La colonne
    // restrict_google (V1) reste en base, facultative et inutilisée : aucune migration.

    public MeetJpaEntity() {}

    public MeetJpaEntity(String id, String title, String link, String time, Boolean active) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.time = time;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public boolean isActive() { return Boolean.TRUE.equals(active); }   // null-safe (jamais renseigné à la création)
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String v) { this.subjectName = v; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String v) { this.teacherName = v; }
    public String getMeetDate() { return meetDate; }
    public void setMeetDate(String v) { this.meetDate = v; }
    public String getClassIds() { return classIds; }
    public void setClassIds(String v) { this.classIds = v; }
    public String getClassNames() { return classNames; }
    public void setClassNames(String v) { this.classNames = v; }
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean v) { this.published = v; }
    public void setActive(boolean active) { this.active = active; }
    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String v) { this.auteurId = v; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String v) { this.courseId = v; }
    public java.time.LocalDateTime getDebut() { return debut; }
    public void setDebut(java.time.LocalDateTime debut) { this.debut = debut; }
    public java.time.LocalDateTime getFin() { return fin; }
    public void setFin(java.time.LocalDateTime fin) { this.fin = fin; }
}
