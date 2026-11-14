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
        // 2026-11-14 : avant, statut « CONFIRME » d'office (personne ne confirmait) et lien Meet INVENTÉ
        // (https://meet.google.com/rdv-xxxxxxxx, aucune réunion). Désormais : demande en attente, sans lien ; elle est
        // transmise au professeur par la messagerie parents ↔ école, où il répond.
        return new Appointment(id, parentId, professeurId, professeurNom, dateSouhaitee, motif,
                STATUT_EN_ATTENTE, null, LocalDateTime.now());
    }

    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";

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

