package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "meets")
public class MeetJpaEntity {

    @Id
    private String id;
    private String title;
    private String link;
    private String time;
    private Boolean active;

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
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
