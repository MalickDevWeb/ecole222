package ecole_221.sn.ecole.modules.parent.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Appointment {
    private final String id;
    private final String parentId;
    private final String professeurId;
    private final String professeurNom;
    private final String dateSouhaitee;
    private final String motif;
    private final String status;
    private final String meetUrl;
    private final LocalDateTime createdAt;

    public Appointment(String id, String parentId, String professeurId, String professeurNom,
                       String dateSouhaitee, String motif, String status, String meetUrl,
                       LocalDateTime createdAt) {
        this.id = id;
        this.parentId = parentId;
        this.professeurId = professeurId;
        this.professeurNom = professeurNom;
        this.dateSouhaitee = dateSouhaitee;
        this.motif = motif;
        this.status = status;
        this.meetUrl = meetUrl;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Appointment create(String parentId, String professeurId, String professeurNom,
                                     String dateSouhaitee, String motif) {
        String id = "rdv-" + UUID.randomUUID().toString().substring(0, 8);
        String meetUrl = "https://meet.google.com/rdv-" + UUID.randomUUID().toString().substring(0, 8);
        return new Appointment(id, parentId, professeurId, professeurNom, dateSouhaitee, motif,
                "CONFIRME", meetUrl, LocalDateTime.now());
    }

    public String getId() { return id; }
    public String getParentId() { return parentId; }
    public String getProfesseurId() { return professeurId; }
    public String getProfesseurNom() { return professeurNom; }
    public String getDateSouhaitee() { return dateSouhaitee; }
    public String getMotif() { return motif; }
    public String getStatus() { return status; }
    public String getMeetUrl() { return meetUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

