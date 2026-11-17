package ecole_221.sn.ecole.modules.referential.application.service;

import ecole_221.sn.ecole.modules.referential.application.dto.ReferenceCaller;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceList;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceValue;
import ecole_221.sn.ecole.modules.referential.domain.port.ReferenceValueRepositoryPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Modification des référentiels. Règles :
 * <ul>
 *   <li>seuls ADMIN (son école) et SUPER_ADMIN (plateforme, ?scope=global) modifient — sinon 403 ;</li>
 *   <li>liste LOCKED : pas d'ajout ni de suppression, pas de désactivation d'une valeur système — 409 ;</li>
 *   <li>une école qui modifie une valeur plateforme crée une SURCHARGE ; la supprimer = revenir à la valeur plateforme.</li>
 *   <li>corbeille (V21) : une valeur PROPRE à l'école supprimée va dans la corbeille (restaurable 30 jours) ;
 *       la recréer sous le même code est refusé (409) tant qu'elle y est. Supprimer une surcharge (retour à la valeur
 *       plateforme) ou une valeur plateforme reste définitif : en PostgreSQL, l'unicité (list_code, code,
 *       organization_id) ne s'applique pas quand organization_id est NULL, une valeur plateforme restaurée
 *       pourrait donc doubler une valeur recréée entre-temps.</li>
 * </ul>
 */
@Service
@Transactional
public class ReferenceCommandService {

    private final ReferenceValueRepositoryPort repository;
    private final ReferenceQueryService queries;
    private final ecole_221.sn.ecole.modules.corbeille.application.CorbeilleService corbeille;

    public ReferenceCommandService(ReferenceValueRepositoryPort repository, ReferenceQueryService queries,
                                   ecole_221.sn.ecole.modules.corbeille.application.CorbeilleService corbeille) {
        this.repository = repository;
        this.queries = queries;
        this.corbeille = corbeille;
    }

    public ReferenceValue create(ReferenceList list, ReferenceCaller caller, String code, String label,
                                 String color, Integer sortOrder) {
        String org = authorize(caller);
        if (list.isLocked()) {
            throw conflict(list.label() + " : liste verrouillée, ses clés sont imposées par le code");
        }
        String finalCode = ReferenceValue.toCode(code != null && !code.isBlank() ? code : label);
        boolean exists = queries.effectiveValues(list, org, true).stream().anyMatch(v -> v.code().equals(finalCode));
        if (exists) {
            throw conflict(list.label() + " : le code " + finalCode + " existe déjà");
        }
        if (org != null) {
            corbeille.referentielEnCorbeille(list.name(), finalCode, org).ifPresent(l -> {
                throw conflict(l + " est dans la corbeille : restaurez-la plutôt que de la recréer");
            });
        }
        return repository.save(new ReferenceValue(UUID.randomUUID().toString(), list, finalCode, label, color,
                sortOrder != null ? sortOrder : 100, true, false, org));
    }

    public ReferenceValue update(ReferenceList list, String code, ReferenceCaller caller, String label,
                                 String color, Integer sortOrder, Boolean active) {
        String org = authorize(caller);
        ReferenceValue current = repository.find(list, code, org)
                .or(() -> repository.find(list, code, null)
                        .map(g -> g.asOverrideFor(UUID.randomUUID().toString(), org)))
                .orElseThrow(() -> notFound(list, code));
        if (Boolean.FALSE.equals(active) && list.isLocked() && current.system()) {
            throw conflict(list.label() + " : " + code + " est utilisé par le code et ne peut pas être désactivé");
        }
        return repository.save(current.withChanges(label, color, sortOrder, active));
    }

    /**
     * @return la réponse de mise à la corbeille pour une valeur propre à l'école (restaurable), vide pour une
     *         suppression définitive (surcharge ramenée à la valeur plateforme, ou valeur plateforme)
     */
    public java.util.Optional<java.util.Map<String, Object>> delete(ReferenceList list, String code, ReferenceCaller caller) {
        String org = authorize(caller);
        ReferenceValue current = repository.find(list, code, org).orElseThrow(() -> notFound(list, code));
        boolean isOverride = org != null && repository.find(list, code, null).isPresent();
        if (!isOverride && (list.isLocked() || current.system())) {
            throw conflict(list.label() + " : " + code + " est une valeur système — désactivez-la plutôt");
        }
        if (org != null && !isOverride) {
            var type = ecole_221.sn.ecole.modules.corbeille.application.TypeCorbeille.REFERENTIEL;
            return java.util.Optional.of(ecole_221.sn.ecole.modules.corbeille.application.CorbeilleService.reponseSuppression(
                    type, current.id(), corbeille.supprimer(type, current.id())));
        }
        repository.deleteById(current.id());   // surcharge (retour à la valeur plateforme) ou valeur plateforme
        return java.util.Optional.empty();
    }

    /** Vérifie les droits et renvoie le périmètre cible (null = plateforme). */
    private String authorize(ReferenceCaller caller) {
        if (caller.global() ? !caller.superAdmin() : !caller.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    caller.global() ? "Seul le SUPER_ADMIN modifie les valeurs plateforme"
                                    : "Seul un administrateur peut modifier les référentiels");
        }
        if (!caller.global() && caller.organizationId() == null) {
            throw new IllegalArgumentException("Aucune école courante : utilisez ?scope=global pour la plateforme");
        }
        return caller.targetOrganizationId();
    }

    private ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    private ResponseStatusException notFound(ReferenceList list, String code) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, list.label() + " : valeur " + code + " introuvable");
    }
}
