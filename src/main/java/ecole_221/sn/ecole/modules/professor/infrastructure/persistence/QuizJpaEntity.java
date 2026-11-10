package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quizzes")
public class QuizJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    private String courseId;
    private String title;
    private int duration;
    // Quiz QCM complet (avant : seuls titre et durée étaient gardés ; questions perdues ; l'identifiant du
    // module était enregistré à la place de celui du cours)
    @jakarta.persistence.Column(name = "module_id")
    private String moduleId;
    @jakarta.persistence.Column(length = 2000)
    private String description;
    @jakarta.persistence.Column(name = "questions_json", columnDefinition = "TEXT")
    private String questionsJson;

    public QuizJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getQuestionsJson() { return questionsJson; }
    public void setQuestionsJson(String questionsJson) { this.questionsJson = questionsJson; }
}
