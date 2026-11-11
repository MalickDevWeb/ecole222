package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Données d'administration d'une école.
 * <ul>
 *   <li>statut des frais : persisté par école (jamais présumé « En Règle ») ;</li>
 *   <li>affectation du portail : persistée par école (avant : partagée entre écoles, « Porte Principale » par défaut) ;</li>
 *   <li>journal temps réel : en mémoire mais PAR ÉCOLE (avant : un seul journal visible par toutes les écoles),
 *       limité aux 200 dernières lignes.</li>
 * </ul>
 */
@Repository
public class InMemoryAdminRepository implements AdminRepositoryPort {

    private static final int TAILLE_JOURNAL = 200;
    private final Map<String, List<String>> journaux = new ConcurrentHashMap<>();
    private final StatutFraisJpaRepository statutsFrais;
    private final AffectationPortailJpaRepository affectations;

    public InMemoryAdminRepository(StatutFraisJpaRepository statutsFrais, AffectationPortailJpaRepository affectations) {
        this.statutsFrais = statutsFrais;
        this.affectations = affectations;
    }

    @Override
    public void assignGate(String gate, String guard) {
        AffectationPortailJpaEntity a = affectations.findAll().stream().findFirst()
                .orElseGet(() -> new AffectationPortailJpaEntity(UUID.randomUUID().toString()));
        a.setPortail(gate);
        a.setVigile(guard);
        a.setUpdatedAt(LocalDateTime.now());
        affectations.save(a);
    }

    /** Affectation de l'école courante ; vide tant que l'école n'a rien saisi. */
    @Override
    public Map<String, String> getGateAssignment() {
        Map<String, String> m = new HashMap<>();
        affectations.findAll().stream().findFirst().ifPresent(a -> {
            if (a.getPortail() != null) m.put("gate", a.getPortail());
            if (a.getVigile() != null) m.put("guard", a.getVigile());
        });
        return m;
    }

    @Override
    public void updateStudentStatus(String studentId, StatutFrais status) {
        StatutFraisJpaEntity ligne = statutsFrais.findById(studentId)
                .orElseGet(() -> new StatutFraisJpaEntity(studentId, status.name()));
        ligne.setStatut(status.name());
        statutsFrais.save(ligne);
    }

    @Override
    public void addRealtimeLog(String message) {
        List<String> j = journaux.computeIfAbsent(ecole(), k -> new CopyOnWriteArrayList<>());
        j.add(0, "[" + LocalDateTime.now() + "] " + message);
        while (j.size() > TAILLE_JOURNAL) j.remove(j.size() - 1);
    }

    @Override
    public List<String> getRealtimeLogs() {
        return new ArrayList<>(journaux.getOrDefault(ecole(), List.of()));
    }

    @Override
    public StatutFrais getStudentStatus(String studentId) {
        // null = non renseigné : l'application ne présume JAMAIS qu'un élève est « En Règle »
        return statutsFrais.findById(studentId).map(l -> StatutFrais.valueOf(l.getStatut())).orElse(null);
    }

    private static String ecole() {
        return TenantContext.getCurrentOrganizationId().orElse("plateforme");
    }
}
