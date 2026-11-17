package ecole_221.sn.ecole.modules.archivage.infrastructure.persistence;

import ecole_221.sn.ecole.modules.archivage.domain.port.EcolesArchivablesPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Adaptateur JDBC vers organizations pour l'archivage : l'agrégat Organization décide de la transition, l'écriture
 * est conditionnelle au statut lu (verrou optimiste, organizations n'a pas de colonne version).
 */
@Repository
public class EcolesArchivablesJdbc implements EcolesArchivablesPort {

    private final JdbcTemplate jdbc;

    public EcolesArchivablesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<EcoleArchivable> ECOLE = (rs, i) -> new EcoleArchivable(rs.getString("id"),
            rs.getString("name"), rs.getString("slug"), rs.getString("status"),
            rs.getTimestamp("suspended_at") == null ? null : rs.getTimestamp("suspended_at").toLocalDateTime());

    @Override
    public Optional<EcoleArchivable> lire(String organizationId) {
        return jdbc.query("SELECT id, name, slug, status, suspended_at FROM organizations WHERE id = ?", ECOLE, organizationId)
                .stream().findFirst();
    }

    @Override
    public boolean geler(String organizationId) {
        return appliquer(organizationId, "SUSPENDED", Organization::archive);
    }

    @Override
    public boolean rendreSuspendue(String organizationId) {
        return appliquer(organizationId, "ARCHIVED", Organization::unarchive);
    }

    @Override
    public boolean restaurer(String organizationId) {
        return appliquer(organizationId, "ARCHIVED", Organization::restaurer);
    }

    /** Lit l'école, vérifie le statut attendu, applique la règle du domaine, écrit si le statut n'a pas bougé. */
    private boolean appliquer(String organizationId, String statutAttendu, Consumer<Organization> transition) {
        List<Organization> lues = jdbc.query("SELECT id, name, type, status, slug, created_at, suspended_at FROM organizations "
                + "WHERE id = ?", (rs, i) -> OrganizationFactory.reconstruct(rs.getString("id"), rs.getString("name"),
                rs.getString("type"), rs.getString("status"), rs.getString("slug"), null, null, null, null, null, null, null,
                null, null, null, rs.getTimestamp("created_at").toLocalDateTime(), null, null,
                rs.getTimestamp("suspended_at") == null ? null : rs.getTimestamp("suspended_at").toLocalDateTime()), organizationId);
        if (lues.isEmpty() || !statutAttendu.equals(lues.get(0).getStatus().value())) {
            return false;
        }
        Organization ecole = lues.get(0);
        try {
            transition.accept(ecole);
        } catch (IllegalStateException e) {
            return false;
        }
        return jdbc.update("UPDATE organizations SET status = ?, suspended_at = ?, updated_at = ? WHERE id = ? AND status = ?",
                ecole.getStatus().value(), ecole.getSuspendedAt() == null ? null : Timestamp.valueOf(ecole.getSuspendedAt()),
                Timestamp.valueOf(LocalDateTime.now()), organizationId, statutAttendu) == 1;
    }

    @Override
    public List<EcoleArchivable> suspenduesAvant(LocalDateTime limite) {
        return jdbc.query("SELECT o.id, o.name, o.slug, o.status, o.suspended_at FROM organizations o "
                + "WHERE o.status = 'SUSPENDED' AND o.suspended_at IS NOT NULL AND o.suspended_at <= ? "
                + "AND NOT EXISTS (SELECT 1 FROM archivages_ecoles a WHERE a.organization_id = o.id "
                + "AND a.etat IN ('ARCHIVAGE_EN_COURS', 'ARCHIVEE', 'SUPPRESSION_A_CONFIRMER', 'RESTAURATION_EN_COURS')) "
                + "ORDER BY o.suspended_at", ECOLE, Timestamp.valueOf(limite));
    }
}
