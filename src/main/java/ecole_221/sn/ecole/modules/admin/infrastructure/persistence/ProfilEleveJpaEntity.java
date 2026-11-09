package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Informations complémentaires d'un élève saisies par l'administration de SON école (santé comprise).
 * Avant : carte en mémoire, perdue à chaque redémarrage. Cloisonné par école (@TenantId).
 */
@Entity
@Table(name = "profils_eleves")
public class ProfilEleveJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "groupe_sanguin")
    private String groupeSanguin;
    @Column(length = 1000)
    private String allergies;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ProfilEleveJpaEntity() {}

    public ProfilEleveJpaEntity(String id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getGroupeSanguin() { return groupeSanguin; }
    public void setGroupeSanguin(String groupeSanguin) { this.groupeSanguin = groupeSanguin; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
