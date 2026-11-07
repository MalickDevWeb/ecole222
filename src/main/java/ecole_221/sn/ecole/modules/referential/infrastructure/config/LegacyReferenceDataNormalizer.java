package ecole_221.sn.ecole.modules.referential.infrastructure.config;

import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Convertit les anciennes valeurs texte en codes de référentiel, pour TOUTES les écoles
 * (SQL direct : les entités @TenantId ne verraient qu'une école). Idempotent.
 * <ul>
 *   <li>expenses.category : « Matériel » → MATERIEL, etc.</li>
 *   <li>candidates.admission_status : « pending », « ACCEPTE », « accepted »… → clé CandidateStatus</li>
 * </ul>
 */
@Component
public class LegacyReferenceDataNormalizer {

    private static final Logger log = LoggerFactory.getLogger(LegacyReferenceDataNormalizer.class);

    private final JdbcTemplate jdbc;

    public LegacyReferenceDataNormalizer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @param categories code → {libellé, teinte} ; l'ancien stockage utilisait des libellés courts. */
    public void normalize(Map<String, String[]> categories, List<CandidateStatus> statuses) {
        Map<String, String> legacyCategories = Map.of(
                "RH", "RH", "Loyer/Charges", "LOYER_CHARGES", "Matériel", "MATERIEL", "Autre", "AUTRE");
        int expenses = 0;
        for (Map.Entry<String, String> e : legacyCategories.entrySet()) {
            if (!categories.containsKey(e.getValue())) continue;
            expenses += update("UPDATE expenses SET category = ? WHERE category = ? AND category <> ?",
                    e.getValue(), e.getKey(), e.getValue());
        }

        int candidates = 0;
        for (CandidateStatus s : statuses) {   // casse différente : « accepted », « en_attente »
            candidates += update("UPDATE candidates SET admission_status = ? "
                    + "WHERE UPPER(admission_status) = ? AND admission_status <> ?", s.name(), s.name(), s.name());
        }
        for (Map.Entry<String, CandidateStatus> a : CandidateStatus.LEGACY_ALIASES.entrySet()) {
            candidates += update("UPDATE candidates SET admission_status = ? WHERE UPPER(admission_status) = ?",
                    a.getValue().name(), a.getKey());
        }
        if (expenses + candidates > 0) {
            log.warn("🔁 Normalisation référentiels : {} dépense(s), {} candidature(s) converties en codes",
                    expenses, candidates);
        }
    }

    private int update(String sql, Object... args) {
        try {
            return jdbc.update(sql, args);
        } catch (DataAccessException ex) {   // table absente sur une base neuve : rien à convertir
            log.debug("Normalisation ignorée ({}) : {}", sql, ex.getMostSpecificCause().getMessage());
            return 0;
        }
    }
}
