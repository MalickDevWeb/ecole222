package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class TaskJpaEntity {

    @Id
    private String id;
    private String groupId;
    private String title;
    private String status;
    private String assignedTo;
    private String priority;

    public TaskJpaEntity() {}

    public TaskJpaEntity(String id, String groupId, String title, String status, String assignedTo, String priority) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.status = status;
        this.assignedTo = assignedTo;
        this.priority = priority != null ? priority : "MEDIUM";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
