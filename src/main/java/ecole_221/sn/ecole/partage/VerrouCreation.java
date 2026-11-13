package ecole_221.sn.ecole.partage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Verrou de création « vérifier puis enregistrer » (2026-11-13).
 *
 * <p>Avant : dix requêtes identiques envoyées au même instant (double clic de plusieurs personnes, réseau qui rejoue)
 * passaient toutes la vérification « ce nom existe-t-il ? » avant qu'aucune n'enregistre : 7 groupes de travail et
 * 3 classes portant le même nom ont été créés lors du test. Le verrou consultatif PostgreSQL
 * ({@code pg_advisory_xact_lock}) est pris sur la clé (école + nom) et tenu jusqu'à la fin de la transaction : les
 * créations concurrentes du même nom passent l'une après l'autre, et la deuxième voit la première. Il vaut aussi
 * avec plusieurs instances du serveur. Sans PostgreSQL (tests unitaires sur H2), aucun verrou n'est pris.</p>
 */
@Component
public class VerrouCreation {

    private static final Logger log = LoggerFactory.getLogger(VerrouCreation.class);

    private final JdbcTemplate jdbc;
    private volatile Boolean postgres;

    public VerrouCreation(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** À appeler DANS la transaction qui vérifie puis enregistre. */
    public void verrouiller(String cle) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("VerrouCreation.verrouiller doit être appelé dans une transaction.");
        }
        if (!estPostgres()) {
            return;
        }
        jdbc.query("select pg_advisory_xact_lock(hashtext(?))", rs -> null, cle);
    }

    private boolean estPostgres() {
        if (postgres == null) {
            try {
                String produit = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName());
                postgres = produit != null && produit.toLowerCase(java.util.Locale.ROOT).contains("postgres");
            } catch (RuntimeException e) {
                log.warn("Type de base inconnu : verrou de création désactivé ({})", e.getMessage());
                postgres = false;
            }
        }
        return postgres;
    }
}
