package ecole_221.sn.ecole.modules.referential.domain.port;

import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceList;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceValue;

import java.util.List;
import java.util.Optional;

/** Port de persistance des valeurs de référence (implémenté par l'adaptateur JPA). */
public interface ReferenceValueRepositoryPort {

    /** Valeurs globales + valeurs de l'école {@code organizationId} (si non null). */
    List<ReferenceValue> findVisible(ReferenceList list, String organizationId);

    /** Recherche exacte dans un périmètre : {@code organizationId} null = valeur globale. */
    Optional<ReferenceValue> find(ReferenceList list, String code, String organizationId);

    ReferenceValue save(ReferenceValue value);

    void deleteById(String id);
}
