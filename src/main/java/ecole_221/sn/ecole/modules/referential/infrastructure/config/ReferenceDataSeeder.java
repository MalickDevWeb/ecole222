package ecole_221.sn.ecole.modules.referential.infrastructure.config;

import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceList;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceValue;
import ecole_221.sn.ecole.modules.referential.domain.port.ReferenceValueRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Valeurs plateforme de départ (system=true), insérées seulement si absentes : un libellé modifié
 * ensuite via l'API n'est jamais écrasé. Flyway étant désactivé (ddl-auto: update), ce seeder
 * remplace une migration SQL. Puis normalise les anciennes données ({@link LegacyReferenceDataNormalizer}).
 */
@Component
@Order(2)
public class ReferenceDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataSeeder.class);

    /** code → {libellé, teinte}. */
    private static final Map<String, String[]> EXPENSE_CATEGORIES = Map.of(
            "RH", new String[]{"Salaires / RH", "sky"},
            "LOYER_CHARGES", new String[]{"Loyer / Charges", "amber"},
            "MATERIEL", new String[]{"Matériel & Équipement", "violet"},
            "AUTRE", new String[]{"Autre dépense", "slate"});

    private static final Map<CandidateStatus, String[]> CANDIDATE_STATUSES = Map.of(
            CandidateStatus.EN_ATTENTE, new String[]{"En attente", "amber"},
            CandidateStatus.EN_COURS, new String[]{"En cours d'étude", "sky"},
            CandidateStatus.ACCEPTED, new String[]{"Accepté", "emerald"},
            CandidateStatus.REJECTED, new String[]{"Refusé", "red"},
            CandidateStatus.FINALISE, new String[]{"Inscription finalisée", "indigo"});

    private final ReferenceValueRepositoryPort repository;
    private final LegacyReferenceDataNormalizer normalizer;

    public ReferenceDataSeeder(ReferenceValueRepositoryPort repository, LegacyReferenceDataNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public void run(String... args) {
        String[] order = {"RH", "LOYER_CHARGES", "MATERIEL", "AUTRE"};
        for (int i = 0; i < order.length; i++) {
            String[] v = EXPENSE_CATEGORIES.get(order[i]);
            seed(ReferenceList.EXPENSE_CATEGORY, order[i], v[0], v[1], (i + 1) * 10);
        }
        CandidateStatus[] statuses = CandidateStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            String[] v = CANDIDATE_STATUSES.get(statuses[i]);
            if (v == null) {   // nouveau statut ajouté dans le code sans libellé : on le signale
                log.error("❌ Statut {} sans libellé dans ReferenceDataSeeder", statuses[i]);
                v = new String[]{statuses[i].name(), "slate"};
            }
            seed(ReferenceList.CANDIDATE_STATUS, statuses[i].name(), v[0], v[1], (i + 1) * 10);
        }
        normalizer.normalize(EXPENSE_CATEGORIES, Arrays.asList(statuses));
    }

    private void seed(ReferenceList list, String code, String label, String color, int sortOrder) {
        if (repository.find(list, code, null).isEmpty()) {
            repository.save(new ReferenceValue(UUID.randomUUID().toString(), list, code, label, color,
                    sortOrder, true, true, null));
            log.info("📚 Référentiel {} : valeur {} créée", list.slug(), code);
        }
    }
}
