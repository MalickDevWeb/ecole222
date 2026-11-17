package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.model.ReglagesArchivage;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Réglages de l'archivage dans parametres_plateforme (même mécanisme que la conservation du journal d'audit, V14) :
 * seuil de suspension avant archivage et conservation d'une archive, en jours, modifiables par le SUPER_ADMIN seul,
 * dans les bornes de {@link ReglagesArchivage}. Toute modification est inscrite au journal d'audit (avant / après).
 */
public class ReglagesArchivageService {

    static final String CLE_SEUIL = "ecoles.archivage.delai_suspension";
    static final String CLE_CONSERVATION = "ecoles.archive.conservation";

    private final JdbcTemplate jdbc;
    private final JournalAudit audit;

    public ReglagesArchivageService(JdbcTemplate jdbc, JournalAudit audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public record Valeurs(ReglagesArchivage reglages, LocalDateTime modifieLe, String modifiePar) {
    }

    public Valeurs lire() {
        Map<String, Integer> jours = new HashMap<>();
        LocalDateTime[] dernier = {null};
        String[] auteur = {null};
        jdbc.query("SELECT cle, round(extract(epoch FROM valeur::interval) / 86400)::int AS jours, modifie_le, modifie_par "
                + "FROM parametres_plateforme WHERE cle IN (?, ?)", rs -> {
            jours.put(rs.getString("cle"), rs.getInt("jours"));
            Timestamp t = rs.getTimestamp("modifie_le");
            if (t != null && (dernier[0] == null || t.toLocalDateTime().isAfter(dernier[0]))) {
                dernier[0] = t.toLocalDateTime();
                auteur[0] = rs.getString("modifie_par");
            }
        }, CLE_SEUIL, CLE_CONSERVATION);
        int seuil = borner(jours.getOrDefault(CLE_SEUIL, ReglagesArchivage.SEUIL_DEFAUT),
                ReglagesArchivage.SEUIL_MIN, ReglagesArchivage.SEUIL_MAX);
        int conservation = borner(jours.getOrDefault(CLE_CONSERVATION, ReglagesArchivage.CONSERVATION_DEFAUT),
                ReglagesArchivage.CONSERVATION_MIN, ReglagesArchivage.CONSERVATION_MAX);
        return new Valeurs(new ReglagesArchivage(seuil, conservation), dernier[0], auteur[0]);
    }

    /** Valeur hors bornes trouvée en base (écrite à la main) : ramenée dans les bornes plutôt qu'appliquée. */
    private static int borner(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Corps : {seuilJours?, conservationJours?} — au moins un des deux ; 400 explicite hors bornes. */
    public Valeurs modifier(Integer seuilJours, Integer conservationJours, String acteurId, String acteurNom) {
        if (seuilJours == null && conservationJours == null) {
            throw ArchivageException.invalide("Indiquez seuilJours ou conservationJours.");
        }
        if (seuilJours != null) ReglagesArchivage.verifierSeuil(seuilJours);
        if (conservationJours != null) ReglagesArchivage.verifierConservation(conservationJours);
        ReglagesArchivage avant = lire().reglages();
        LocalDateTime maintenant = LocalDateTime.now();
        if (seuilJours != null) ecrire(CLE_SEUIL, seuilJours, acteurId, maintenant);
        if (conservationJours != null) ecrire(CLE_CONSERVATION, conservationJours, acteurId, maintenant);
        Valeurs apres = lire();
        if (audit != null) {
            audit.inscrire(new JournalAudit.Entree(JournalAudit.PLATEFORME, acteurId, acteurNom, "SUPER_ADMIN",
                    "REGLAGE_ARCHIVAGE", "archivage/reglages", null,
                    "Réglages de l'archivage : seuil " + avant.seuilJours() + " → " + apres.reglages().seuilJours()
                            + " jours, conservation " + avant.conservationJours() + " → " + apres.reglages().conservationJours()
                            + " jours", null, null, "archivage"));
        }
        return apres;
    }

    private void ecrire(String cle, int jours, String acteurId, LocalDateTime maintenant) {
        jdbc.update("INSERT INTO parametres_plateforme (cle, valeur, modifie_le, modifie_par) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (cle) DO UPDATE SET valeur = EXCLUDED.valeur, modifie_le = EXCLUDED.modifie_le, "
                        + "modifie_par = EXCLUDED.modifie_par",
                cle, jours + " days", Timestamp.valueOf(maintenant), acteurId);
    }

    /** Vue des réglages pour l'écran du super-admin (valeurs, bornes, dernière modification). */
    public static Map<String, Object> vue(Valeurs v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seuilJours", v.reglages().seuilJours());
        m.put("conservationJours", v.reglages().conservationJours());
        m.put("bornes", Map.of(
                "seuilJours", Map.of("min", ReglagesArchivage.SEUIL_MIN, "max", ReglagesArchivage.SEUIL_MAX),
                "conservationJours", Map.of("min", ReglagesArchivage.CONSERVATION_MIN, "max", ReglagesArchivage.CONSERVATION_MAX)));
        m.put("modifieLe", v.modifieLe());
        m.put("modifiePar", v.modifiePar());
        return m;
    }

    static List<String> cles() {
        return List.of(CLE_SEUIL, CLE_CONSERVATION);
    }
}
