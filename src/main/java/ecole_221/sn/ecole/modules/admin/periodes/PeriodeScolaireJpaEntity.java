package ecole_221.sn.ecole.modules.admin.periodes;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Période scolaire (trimestre…) d'une école ; une fois clôturée, ses notes sont figées (BR-04, V13). */
@Entity
@Table(name = "periodes_scolaires")
public class PeriodeScolaireJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    @Column(length = 64)
    private String id;
    @Column(nullable = false, length = 60)
    private String libelle;
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
    @Column(nullable = false)
    private boolean cloturee;
    @Column(name = "cloturee_le")
    private LocalDateTime clotureeLe;
    @Column(name = "cloturee_par")
    private String clotureePar;
    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public boolean isCloturee() { return cloturee; }
    public void setCloturee(boolean cloturee) { this.cloturee = cloturee; }
    public LocalDateTime getClotureeLe() { return clotureeLe; }
    public void setClotureeLe(LocalDateTime clotureeLe) { this.clotureeLe = clotureeLe; }
    public String getClotureePar() { return clotureePar; }
    public void setClotureePar(String clotureePar) { this.clotureePar = clotureePar; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }

    public boolean contient(LocalDate d) {
        return d != null && !d.isBefore(dateDebut) && !d.isAfter(dateFin);
    }
}
