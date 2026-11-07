package ecole_221.sn.ecole.modules.referential.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Table reference_values : valeurs des listes de référence.
 *
 * <p>Pas de {@code @TenantId} volontairement : organization_id NULL = valeur plateforme visible par
 * toutes les écoles. L'isolation est faite explicitement par les requêtes du repository
 * ({@code organization_id IS NULL OR organization_id = :org}).</p>
 */
@Entity
@Table(name = "reference_values",
        uniqueConstraints = @UniqueConstraint(name = "uk_reference_value_scope",
                columnNames = {"list_code", "code", "organization_id"}),
        indexes = @Index(name = "idx_reference_values_list_org", columnList = "list_code, organization_id"))
public class ReferenceValueJpaEntity {

    @Id
    private String id;

    @Column(name = "list_code", nullable = false, length = 40)
    private String listCode;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "label", nullable = false, length = 80)
    private String label;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "system_value", nullable = false)
    private boolean systemValue;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ReferenceValueJpaEntity() { }

    public ReferenceValueJpaEntity(String id, String listCode, String code, String label, String color,
                                   int sortOrder, boolean active, boolean systemValue, String organizationId) {
        this.id = id;
        this.listCode = listCode;
        this.code = code;
        this.label = label;
        this.color = color;
        this.sortOrder = sortOrder;
        this.active = active;
        this.systemValue = systemValue;
        this.organizationId = organizationId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getListCode() { return listCode; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getColor() { return color; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public boolean isSystemValue() { return systemValue; }
    public String getOrganizationId() { return organizationId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
