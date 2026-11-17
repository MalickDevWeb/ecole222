package ecole_221.sn.ecole.modules.audit.infrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Écriture d'une entrée dans le journal d'audit inaltérable {@code audit_logs} (BR-05, ENF-07).
 *
 * <p>Utilisé par {@code JournalAuditInterceptor} (toute écriture HTTP réussie) et par les écritures faites hors HTTP
 * (messages du cours en direct, reçus par WebSocket). Insertion directe en SQL : l'école est fixée explicitement, y
 * compris « plateforme » pour le super-administrateur hors école. Une panne d'écriture du journal est signalée dans
 * les traces mais n'annule pas l'action déjà enregistrée.</p>
 */
@Component
public class JournalAudit {

    private static final Logger log = LoggerFactory.getLogger(JournalAudit.class);
    public static final String PLATEFORME = "plateforme";

    /** Une entrée. {@code acteurId} nul : action anonyme (formulaire public). Aucun contenu de requête. */
    public record Entree(String ecole, String acteurId, String acteurNom, String role, String typeAction, String typeEntite,
                         String identifiants, String description, String ip, String navigateur, String module) {
    }

    private final JdbcTemplate jdbc;

    public JournalAudit(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static String borne(String s) {
        return s == null ? null : (s.length() <= 255 ? s : s.substring(0, 255));
    }

    public void inscrire(Entree e) {
        try {
            Timestamp maintenant = Timestamp.valueOf(LocalDateTime.now());
            String ecole = e.ecole() == null || e.ecole().isBlank() ? PLATEFORME : e.ecole();
            jdbc.update("INSERT INTO audit_logs (id, organization_id, action_type, entity_type, entity_id, actor_id, actor_name, "
                            + "actor_role, action_description, ip_address, user_agent, severity, status, module, action_timestamp, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    "aud-" + UUID.randomUUID(), ecole, e.typeAction(), borne(e.typeEntite()), borne(e.identifiants()),
                    e.acteurId(), borne(e.acteurNom()), borne(e.role()), borne(e.description()), borne(e.ip()),
                    borne(e.navigateur()), "DELETE".equals(e.typeAction()) ? "warning" : "info", "success",
                    borne(e.module()), maintenant, maintenant);
        } catch (RuntimeException ex) {
            log.warn("Écriture non inscrite au journal d'audit ({}) : {}", e.description(), ex.getMessage());
        }
    }
}
