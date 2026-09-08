package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "live_chat_messages")
public class LiveChatMessageJpaEntity {

    @Id
    private String id;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "user_name")
    private String userName;
    
    @Column(name = "user_role")
    private String userRole;
    
    @Column(name = "text")
    private String text;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public LiveChatMessageJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.timestamp = LocalDateTime.now();
    }

    public LiveChatMessageJpaEntity(String id, String sessionId, String userId, String userName, 
                                    String userRole, String text) {
        this();
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.text = text;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}