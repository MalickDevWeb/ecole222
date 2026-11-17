package ecole_221.sn.ecole.modules.referential.application.service;

import ecole_221.sn.ecole.modules.referential.application.dto.EffectiveReferenceValue;
import ecole_221.sn.ecole.modules.referential.application.dto.EffectiveReferenceValue.Scope;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceList;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceValue;
import ecole_221.sn.ecole.modules.referential.domain.port.ReferenceValueRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lecture des référentiels : fusion « valeurs plateforme + surcharges de l'école ».
 * Utilisé par l'API /v1/referentiels et par les autres modules pour valider une saisie.
 */
@Service
public class ReferenceQueryService {

    private final ReferenceValueRepositoryPort repository;

    public ReferenceQueryService(ReferenceValueRepositoryPort repository) {
        this.repository = repository;
    }

    /** Liste effective vue par l'école {@code organizationId} (null = vue plateforme). */
    public List<EffectiveReferenceValue> effectiveValues(ReferenceList list, String organizationId,
                                                         boolean includeInactive) {
        Map<String, ReferenceValue> globals = new LinkedHashMap<>();
        Map<String, ReferenceValue> locals = new LinkedHashMap<>();
        for (ReferenceValue v : repository.findVisible(list, organizationId)) {
            (v.isGlobal() ? globals : locals).put(v.code(), v);
        }
        Map<String, EffectiveReferenceValue> merged = new LinkedHashMap<>();
        globals.forEach((code, g) -> merged.put(code, toEffective(locals.getOrDefault(code, g),
                locals.containsKey(code) ? Scope.SURCHARGE : Scope.GLOBAL)));
        locals.forEach((code, l) -> merged.putIfAbsent(code, toEffective(l, Scope.ECOLE)));

        return merged.values().stream()
                .filter(v -> includeInactive || v.active())
                .sorted(Comparator.comparingInt(EffectiveReferenceValue::sortOrder)
                        .thenComparing(EffectiveReferenceValue::label, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Convertit une saisie (code ou libellé, casse et accents ignorés) en code canonique ACTIF.
     *
     * @throws IllegalArgumentException (HTTP 400) si la valeur n'existe pas ou est désactivée
     */
    public String resolveCode(ReferenceList list, String input, String organizationId) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(list.label() + " : valeur obligatoire");
        }
        String asCode = ReferenceValue.toCode(input);
        List<EffectiveReferenceValue> active = effectiveValues(list, organizationId, false);
        return active.stream()
                .filter(v -> v.code().equals(asCode) || ReferenceValue.toCode(v.label()).equals(asCode))
                .map(EffectiveReferenceValue::code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(list.label() + " : valeur inconnue ou désactivée « "
                        + input + " ». Valeurs autorisées : "
                        + active.stream().map(EffectiveReferenceValue::code).collect(Collectors.joining(", "))));
    }

    private EffectiveReferenceValue toEffective(ReferenceValue v, Scope scope) {
        return new EffectiveReferenceValue(v.code(), v.label(), v.color(), v.sortOrder(),
                v.active(), v.system(), v.list().isLocked(), scope);
    }
}
