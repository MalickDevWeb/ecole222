package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.TaskPriority;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.TaskStatus;

import java.util.Objects;

/**
 * Agrégat racine représentant une tâche.
 * Constructeur privé pour garantir l'invariance - utiliser TaskFactory.
 */
public class Task {
    
    private final String id;
    private String title;
    private String description;
    private final String workgroupId;
    private String assigneeId;
    private TaskStatus status;
    private TaskPriority priority;
    
    // Constructeur package-private pour permettre aux Factories d'y accéder
    Task(String id, String title, String description, String workgroupId, String assigneeId, 
                TaskStatus status, TaskPriority priority) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.title = Objects.requireNonNull(title, "Le titre ne peut pas être null");
        this.description = description;
        this.workgroupId = Objects.requireNonNull(workgroupId, "L'ID workgroup ne peut pas être null");
        this.assigneeId = assigneeId;
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.priority = Objects.requireNonNull(priority, "La priorité ne peut pas être null");
    }
    
    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getWorkgroupId() { return workgroupId; }
    public String getAssigneeId() { return assigneeId; }
    public TaskStatus getStatus() { return status; }
    public TaskPriority getPriority() { return priority; }
    
    // Comportements métier
    public void updateTitle(String newTitle) {
        this.title = Objects.requireNonNull(newTitle, "Le titre ne peut pas être null");
    }
    
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    public void assignTo(String newAssigneeId) {
        this.assigneeId = newAssigneeId;
    }
    
    public void markAsTodo() {
        this.status = TaskStatus.TODO;
    }
    
    public void markAsInProgress() {
        this.status = TaskStatus.IN_PROGRESS;
    }
    
    public void markAsDone() {
        this.status = TaskStatus.DONE;
    }
    
    public void markAsCancelled() {
        this.status = TaskStatus.CANCELLED;
    }
    
    public void setPriority(TaskPriority newPriority) {
        this.priority = Objects.requireNonNull(newPriority, "La priorité ne peut pas être null");
    }
    
    public boolean isTodo() {
        return this.status.equals(TaskStatus.TODO);
    }
    
    public boolean isInProgress() {
        return this.status.equals(TaskStatus.IN_PROGRESS);
    }
    
    public boolean isDone() {
        return this.status.equals(TaskStatus.DONE);
    }
    
    public boolean isCancelled() {
        return this.status.equals(TaskStatus.CANCELLED);
    }
    
    public boolean isUrgent() {
        return this.priority.equals(TaskPriority.URGENT);
    }
    
    public boolean isHighPriority() {
        return this.priority.equals(TaskPriority.HIGH) || this.priority.equals(TaskPriority.URGENT);
    }
    
    public boolean isAssigned() {
        return this.assigneeId != null && !this.assigneeId.isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", workgroupId='" + workgroupId + '\'' +
                ", assigneeId='" + assigneeId + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                '}';
    }
}