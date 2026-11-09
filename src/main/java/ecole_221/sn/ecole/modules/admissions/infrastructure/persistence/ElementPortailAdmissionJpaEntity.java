package ecole_221.sn.ecole.modules.admissions.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Élément du portail candidat d'une école : campagne d'admission, message ou entrée du journal d'audit.
 * Avant : trois listes statiques en mémoire, communes à toutes les écoles, perdues au redémarrage
 * (et deux campagnes « Licence Informatique » / « Master Cloud » inventées avec images Unsplash).
 * Désormais persisté et cloisonné par école (@TenantId) ; le contenu est conservé tel que saisi (JSON).
 */
@Entity
@Table(name = "admissions_portail_elements")
public class ElementPortailAdmissionJpaEntity {

    public static final String CAMPAGNE = "CAMPAGNE";
    public static final String MESSAGE = "MESSAGE";
    public static final String AUDIT = "AUDIT";

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    /** CAMPAGNE, MESSAGE ou AUDIT. */
    @Column(nullable = false, length = 20)
    private String nature;
    /** Ordre d'affichage dans la liste envoyée par le portail. */
    private int position;
    @Column(name = "contenu_json", columnDefinition = "TEXT")
    private String contenuJson;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ElementPortailAdmissionJpaEntity() {}

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNature() { return nature; }
    public void setNature(String nature) { this.nature = nature; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getContenuJson() { return contenuJson; }
    public void setContenuJson(String contenuJson) { this.contenuJson = contenuJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
