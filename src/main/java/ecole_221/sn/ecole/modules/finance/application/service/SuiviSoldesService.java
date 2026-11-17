package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Suivi des soldes (APP-TL-22) — LECTURE SEULE : aucune mesure, aucun message, aucun blocage.
 *
 * <p>Le dossier financier ne porte ni échéancier ni année scolaire. Le « retard estimé » suppose donc un paiement
 * régulier en 9 mensualités d'octobre à juin : mensualité = total dû ÷ 9 ; attendu à date = total × mois entamés
 * depuis le 1er octobre ÷ 9 (9 de juillet à septembre) ; retard estimé = attendu − payé. Un dossier est « à suivre »
 * si le retard estimé atteint deux mensualités ET qu'aucun versement n'est enregistré depuis 30 jours (date
 * d'ouverture du dossier s'il n'y en a aucun). Une famille qui paie ses trimestres à l'heure n'est jamais à suivre.
 * L'accès de l'élève (campus, application, bulletin) n'en dépend jamais. Lecture cloisonnée : dossiers et liens
 * parent-enfant portent @TenantId (école courante).</p>
 */
@Service
public class SuiviSoldesService {

    static final int MENSUALITES = 9;
    static final int MENSUALITES_DE_RETARD = 2;
    static final int JOURS_SANS_VERSEMENT = 30;

    private final StudentFinanceJpaRepository dossiers;
    private final ParentEnfantJpaRepository liensParents;
    private final UserJpaRepository comptes;

    public SuiviSoldesService(StudentFinanceJpaRepository dossiers, ParentEnfantJpaRepository liensParents, UserJpaRepository comptes) {
        this.dossiers = dossiers;
        this.liensParents = liensParents;
        this.comptes = comptes;
    }

    /** Mois entamés de l'année scolaire (octobre = 1, …, juin = 9 ; juillet à septembre = 9). */
    static int moisEntames(LocalDate jour) {
        int m = jour.getMonthValue();
        if (m >= 10) return m - 9;
        if (m <= 6) return m + 3;
        return MENSUALITES;
    }

    /** Retard estimé (FCFA, jamais négatif) pour un paiement régulier en 9 mensualités d'octobre à juin. */
    static long retardEstime(long totalDu, long paye, LocalDate jour) {
        long attendu = totalDu * moisEntames(jour) / MENSUALITES;
        return Math.max(0, attendu - paye);
    }

    static boolean aSuivre(long totalDu, long paye, long joursSansVersement, LocalDate jour) {
        long mensualite = totalDu / MENSUALITES;
        return mensualite > 0 && retardEstime(totalDu, paye, jour) >= MENSUALITES_DE_RETARD * mensualite
                && joursSansVersement >= JOURS_SANS_VERSEMENT;
    }

    private static long nul0(Long v) {
        return v == null ? 0 : v;
    }

    /** Dossiers « Non soldé » de l'école, avec le retard estimé et les parents liés (pour les appeler). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> soldes() {
        return soldes(LocalDate.now());
    }

    List<Map<String, Object>> soldes(LocalDate jour) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (StudentFinanceJpaEntity d : dossiers.findStudentsWithDebt()) {
            long total = nul0(d.getTotalRequired());
            long paye = nul0(d.getPaid());
            long reste = d.getDebt() != null ? Math.max(0, d.getDebt()) : Math.max(0, total - paye);
            if (reste <= 0) continue;
            LocalDateTime repere = d.getLastPaymentDate() != null ? d.getLastPaymentDate() : d.getCreatedAt();
            Long jours = repere == null ? null : Math.max(0, ChronoUnit.DAYS.between(repere.toLocalDate(), jour));
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("eleveId", d.getStudentId());
            l.put("eleve", d.getStudentName());
            l.put("classe", d.getClasse());
            l.put("totalDu", total);
            l.put("paye", paye);
            l.put("resteDu", reste);
            l.put("mensualite", total / MENSUALITES);
            l.put("retardEstime", retardEstime(total, paye, jour));
            l.put("dernierVersement", d.getLastPaymentDate() == null ? null : d.getLastPaymentDate().toLocalDate().toString());
            l.put("joursSansVersement", jours);
            l.put("aSuivre", jours != null && aSuivre(total, paye, jours, jour));
            l.put("parents", parents(d.getStudentId()));
            lignes.add(l);
        }
        lignes.sort(Comparator.comparing((Map<String, Object> l) -> !(Boolean) l.get("aSuivre"))
                .thenComparing(l -> -((Long) l.get("retardEstime"))));
        return lignes;
    }

    private List<Map<String, Object>> parents(String eleveId) {
        if (eleveId == null) return List.of();
        List<String> ids = liensParents.findByEnfantId(eleveId).stream().map(ParentEnfantJpaEntity::getParentId)
                .filter(Objects::nonNull).distinct().toList();
        List<Map<String, Object>> r = new ArrayList<>();
        for (UserJpaEntity p : comptes.findAllById(ids)) {
            Map<String, Object> m = new LinkedHashMap<>();
            String nom = ((p.getPrenom() == null ? "" : p.getPrenom()) + " " + (p.getNom() == null ? "" : p.getNom())).trim();
            m.put("nom", nom.isEmpty() ? null : nom);
            m.put("telephone", p.getTelephone());
            r.add(m);
        }
        return r;
    }
}
