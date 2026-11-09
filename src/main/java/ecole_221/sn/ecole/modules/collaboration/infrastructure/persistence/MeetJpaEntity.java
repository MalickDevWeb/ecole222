package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "meets")
public class MeetJpaEntity {
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
    // APP-BE-14 : champs de la classe virtuelle envoyés par le frontend (auparavant perdus)
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
    @jakarta.persistence.Column(name = "restrict_google")
    private Boolean restrictToGoogleAccount;

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
    public Boolean getRestrictToGoogleAccount() { return restrictToGoogleAccount; }
    public void setRestrictToGoogleAccount(Boolean v) { this.restrictToGoogleAccount = v; }
    public void setActive(boolean active) { this.active = active; }
}
