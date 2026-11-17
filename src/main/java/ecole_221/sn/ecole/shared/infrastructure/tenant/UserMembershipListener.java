package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import jakarta.persistence.PostPersist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;
import java.util.UUID;

/**
 * SEC-003 phase 2 — point de passage UNIQUE : tout compte inséré pendant une requête rattachée
 * à une école (TenantContext) devient membre ACTIF de cette école (organization_users).
 * Couvre tous les parcours de création (admin, import, inscription…) sans les modifier un par un.
 *
 * <p>Non concernés : créations hors école (démarrage, mode plateforme) et rôles plateforme ;
 * les appartenances explicites déjà créées (ex. TenantService) ne sont pas dupliquées.
 * L'insertion a lieu APRÈS validation de la transaction du compte (pas de flush imbriqué).</p>
 */
@Component
public class UserMembershipListener {

    private static final Logger log = LoggerFactory.getLogger(UserMembershipListener.class);
    private static final Set<String> PLATFORM_ROLES = Set.of("SUPER_ADMIN", "PLATFORM_ADMIN");
    private static final String INSERT_IF_ABSENT =
            "INSERT INTO organization_users (id, organization_id, user_id, role, status, joined_at) "
          + "SELECT ?, ?, ?, ?, 'ACTIVE', now() WHERE NOT EXISTS "
          + "(SELECT 1 FROM organization_users WHERE user_id = ? AND organization_id = ?)";

    private final JdbcTemplate jdbc;

    public UserMembershipListener(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostPersist
    public void onUserCreated(UserJpaEntity user) {
        String organizationId = TenantContext.getCurrentOrganizationId().orElse(null);
        String role = user.getRole();
        if (organizationId == null || TenantContext.isPlatformMode() || role == null || PLATFORM_ROLES.contains(role)) {
            return;
        }
        Runnable link = () -> {
            int created = jdbc.update(INSERT_IF_ABSENT, UUID.randomUUID().toString(), organizationId, user.getId(),
                    role, user.getId(), organizationId);
            if (created > 0) {
                log.info("SEC-003 : compte {} rattaché à l'école {} (rôle {})", user.getId(), organizationId, role);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    link.run();
                }
            });
        } else {
            link.run();
        }
    }
}
