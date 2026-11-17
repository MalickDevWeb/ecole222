package ecole_221.sn.ecole.modules.audit.application.service;

import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.partage.ChampsRequete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Durée de conservation du journal d'audit : « pour l'instant 30 minutes, mais on peut le changer ; elle ne doit pas
 * être figée dans le code ».
 *
 * <p>La durée est rangée dans {@code parametres_plateforme} (migration V14) et modifiée par le super-administrateur.
 * La base n'admet la suppression d'une entrée que si elle est plus ancienne que cette durée (déclencheur V14) ; la
 * purge, lancée chaque minute, supprime les entrées échues et s'inscrit elle-même au journal.</p>
 */
@Service
public class ConservationJournalAuditService {

    private static final Logger log = LoggerFactory.getLogger(ConservationJournalAuditService.class);
    static final String CLE = "audit.conservation";
    /** Unité affichée → unité d'intervalle PostgreSQL, et nombre de minutes par unité. */
    static final Map<String, String> UNITES_SQL = Map.of("minutes", "minutes", "heures", "hours", "jours", "days");
    static final Map<String, Long> MINUTES_PAR_UNITE = Map.of("minutes", 1L, "heures", 60L, "jours", 1_440L);
    static final long MINUTES_MIN = 1;
    static final long MINUTES_MAX = 3_650L * 1_440L;   // 10 ans

    private final JdbcTemplate jdbc;
    private final JournalAudit journal;

    public ConservationJournalAuditService(JdbcTemplate jdbc, JournalAudit journal) {
        this.jdbc = jdbc;
        this.journal = journal;
    }

    /** Valeur et unité les plus lisibles pour une durée en minutes : 1 440 → 1 jour, 120 → 2 heures, 30 → 30 minutes. */
    static Map<String, Object> lisible(long minutes) {
        String unite = minutes % 1_440 == 0 ? "jours" : minutes % 60 == 0 ? "heures" : "minutes";
        long valeur = minutes / MINUTES_PAR_UNITE.get(unite);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valeur", valeur);
        m.put("unite", unite);
        m.put("libelle", valeur + " " + (valeur == 1 ? unite.substring(0, unite.length() - 1) : unite));
        m.put("minutes", minutes);
        return m;
    }

    public Map<String, Object> lire() {
        List<Map<String, Object>> lignes = jdbc.queryForList(
                "SELECT (extract(epoch FROM valeur::interval) / 60)::bigint AS minutes, modifie_le, modifie_par "
                        + "FROM parametres_plateforme WHERE cle = ?", CLE);
        if (lignes.isEmpty()) {
            throw new IllegalStateException("Durée de conservation du journal d'audit absente (migration V14 non appliquée).");
        }
        Map<String, Object> l = lignes.get(0);
        Map<String, Object> m = lisible(((Number) l.get("minutes")).longValue());
        m.put("modifieLe", l.get("modifie_le") == null ? null : l.get("modifie_le").toString());
        m.put("modifiePar", l.get("modifie_par"));
        return m;
    }

    /** Corps : {valeur: 30, unite: "minutes" | "heures" | "jours"} ; entre 1 minute et 10 ans. */
    public Map<String, Object> modifier(Map<String, Object> corps) {
        ChampsRequete.corpsRequis(corps);
        String unite = ChampsRequete.choix(corps, "unite", "L'unité", List.of("minutes", "heures", "jours"), null);
        int valeur = ChampsRequete.entier(corps, "valeur", "La durée", 1, MINUTES_MAX);
        long minutes = valeur * MINUTES_PAR_UNITE.get(unite);
        if (minutes < MINUTES_MIN || minutes > MINUTES_MAX) {
            throw ChampsRequete.refus("La durée de conservation doit être comprise entre 1 minute et 10 ans.");
        }
        String auteur = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User u ? u.getId() : null;
        jdbc.update("UPDATE parametres_plateforme SET valeur = ?, modifie_le = ?, modifie_par = ? WHERE cle = ?",
                valeur + " " + UNITES_SQL.get(unite), Timestamp.valueOf(LocalDateTime.now()), auteur, CLE);
        log.info("Conservation du journal d'audit fixée à {} {} par {}", valeur, unite, auteur);
        return lire();
    }

    /** Supprime les entrées plus anciennes que la durée de conservation ; la purge est elle-même inscrite au journal. */
    public int purger() {
        int n = jdbc.update("DELETE FROM audit_logs WHERE action_timestamp < localtimestamp - "
                + "(SELECT valeur::interval FROM parametres_plateforme WHERE cle = ?)", CLE);
        if (n > 0) {
            String duree = String.valueOf(lire().get("libelle"));
            log.info("Purge de conservation du journal d'audit : {} entrée(s) de plus de {} supprimée(s)", n, duree);
            journal.inscrire(new JournalAudit.Entree(null, null, "système (conservation du journal)", null, "DELETE",
                    "audit/conservation", null, "Purge de conservation : " + n + " entrée(s) de plus de " + duree
                    + " supprimée(s)", null, null, "audit"));
        }
        return n;
    }
}
