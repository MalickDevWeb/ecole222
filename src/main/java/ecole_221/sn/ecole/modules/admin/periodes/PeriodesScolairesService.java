package ecole_221.sn.ecole.modules.admin.periodes;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Périodes scolaires et clôture des notes (BR-04, 2026-11-14).
 *
 * <p>Une évaluation appartient à la période qui contient sa date. Quand l'administration clôture une période, plus
 * aucune évaluation n'y est créée, modifiée, supprimée ni notée (vérifié par {@link #exigerOuverte}). La réouverture
 * est réservée à l'administration (jamais déléguée au personnel) et tracée au journal d'audit.</p>
 */
@Service
public class PeriodesScolairesService {

    static final int LIBELLE_MAX = 60;
    static final int DUREE_MAX_JOURS = 400;

    private final PeriodeScolaireJpaRepository periodes;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.infrastructure.logging.ActionLogger journal;

    public PeriodesScolairesService(PeriodeScolaireJpaRepository periodes) {
        this.periodes = periodes;
    }

    /** Période clôturée contenant cette date, s'il y en a une. */
    public Optional<PeriodeScolaireJpaEntity> clotureeContenant(LocalDate date) {
        if (date == null) return Optional.empty();
        return periodes.findAllByOrderByDateDebutAsc().stream().filter(p -> p.isCloturee() && p.contient(date)).findFirst();
    }

    /** 409 si la date tombe dans une période clôturée (BR-04). {@code quoi} : ce qui est refusé, pour le message. */
    public void exigerOuverte(LocalDate date, String quoi) {
        clotureeContenant(date).ifPresent(p -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La période « " + p.getLibelle() + " » ("
                    + p.getDateDebut() + " au " + p.getDateFin() + ") est clôturée : " + quoi
                    + ". Seule l'administration peut la rouvrir.");
        });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> liste() {
        return periodes.findAllByOrderByDateDebutAsc().stream().map(PeriodesScolairesService::vue).toList();
    }

    @Transactional
    public Map<String, Object> creer(Map<String, Object> corps) {
        PeriodeScolaireJpaEntity p = new PeriodeScolaireJpaEntity();
        p.setId("per-" + UUID.randomUUID());
        p.setCreeLe(LocalDateTime.now());
        appliquer(p, corps, true);
        return vue(periodes.save(p));
    }

    @Transactional
    public Map<String, Object> modifier(String id, Map<String, Object> corps) {
        PeriodeScolaireJpaEntity p = periode(id);
        if (p.isCloturee()) throw conflit("La période « " + p.getLibelle() + " » est clôturée : rouvrez-la avant de la modifier.");
        appliquer(p, corps, false);
        return vue(periodes.save(p));
    }

    @Transactional
    public void supprimer(String id) {
        PeriodeScolaireJpaEntity p = periode(id);
        if (p.isCloturee()) throw conflit("La période « " + p.getLibelle() + " » est clôturée : elle ne peut pas être supprimée.");
        periodes.delete(p);
    }

    @Transactional
    public Map<String, Object> cloturer(String id) {
        PeriodeScolaireJpaEntity p = periode(id);
        if (p.isCloturee()) throw conflit("La période « " + p.getLibelle() + " » est déjà clôturée.");
        p.setCloturee(true);
        p.setClotureeLe(LocalDateTime.now());
        p.setClotureePar(auteur());
        tracer("CLOTURE_PERIODE", p, "Période « " + p.getLibelle() + " » clôturée : notes figées");
        return vue(periodes.save(p));
    }

    /** Réouverture exceptionnelle : administration seulement (le personnel ne rouvre jamais une période). */
    @Transactional
    public Map<String, Object> rouvrir(String id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean administration = auth != null && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_ADMINISTRATOR") || a.equals("ROLE_SUPER_ADMIN"));
        if (!administration) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seule l'administration de l'école peut rouvrir une période clôturée.");
        }
        PeriodeScolaireJpaEntity p = periode(id);
        if (!p.isCloturee()) throw conflit("La période « " + p.getLibelle() + " » n'est pas clôturée.");
        p.setCloturee(false);
        p.setClotureeLe(null);
        p.setClotureePar(null);
        tracer("REOUVERTURE_PERIODE", p, "Période « " + p.getLibelle() + " » rouverte : notes de nouveau modifiables");
        return vue(periodes.save(p));
    }

    // ---------------------------------------------------------------- outils

    private PeriodeScolaireJpaEntity periode(String id) {
        return periodes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Période introuvable."));
    }

    private void appliquer(PeriodeScolaireJpaEntity p, Map<String, Object> corps, boolean creation) {
        if (corps == null || corps.isEmpty()) throw refus("Renseignez la période : libellé, date de début et date de fin.");
        if (creation || corps.containsKey("libelle")) {
            String l = texte(corps.get("libelle"));
            if (l == null) throw refus("Le libellé est obligatoire (ex. : Trimestre 1).");
            if (l.length() > LIBELLE_MAX) throw refus("Le libellé ne doit pas dépasser " + LIBELLE_MAX + " caractères.");
            String cle = l.toLowerCase(Locale.ROOT);
            boolean doublon = periodes.findAllByOrderByDateDebutAsc().stream()
                    .anyMatch(x -> !x.getId().equals(p.getId()) && x.getLibelle().toLowerCase(Locale.ROOT).equals(cle));
            if (doublon) throw conflit("Une période porte déjà le libellé « " + l + " ».");
            p.setLibelle(l);
        }
        if (creation || corps.containsKey("dateDebut")) p.setDateDebut(date(corps.get("dateDebut"), "La date de début"));
        if (creation || corps.containsKey("dateFin")) p.setDateFin(date(corps.get("dateFin"), "La date de fin"));
        if (p.getDateFin().isBefore(p.getDateDebut())) throw refus("La date de fin doit être après la date de début.");
        if (ChronoUnit.DAYS.between(p.getDateDebut(), p.getDateFin()) > DUREE_MAX_JOURS) {
            throw refus("Une période ne peut pas dépasser " + DUREE_MAX_JOURS + " jours.");
        }
        periodes.findAllByOrderByDateDebutAsc().stream()
                .filter(x -> !x.getId().equals(p.getId()))
                .filter(x -> !x.getDateFin().isBefore(p.getDateDebut()) && !x.getDateDebut().isAfter(p.getDateFin()))
                .findFirst()
                .ifPresent(x -> { throw conflit("Ces dates chevauchent la période « " + x.getLibelle() + " » ("
                        + x.getDateDebut() + " au " + x.getDateFin() + ")."); });
    }

    private static LocalDate date(Object v, String libelle) {
        String t = texte(v);
        if (t == null) throw refus(libelle + " est obligatoire (format AAAA-MM-JJ).");
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw refus(libelle + " est invalide : " + t + " (format AAAA-MM-JJ).");
        }
    }

    private static String texte(Object v) {
        if (v == null) return null;
        if (!(v instanceof String s)) throw refus("Valeur invalide : " + v + ".");
        String t = ecole_221.sn.ecole.partage.TexteNormalise.normaliser(s);
        return t.isEmpty() ? null : t;
    }

    private static ResponseStatusException refus(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private static ResponseStatusException conflit(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); }

    private static String auteur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof User u ? u.getId() : null;
    }

    /**
     * Détail lisible dans les traces de l'application. L'entrée du journal d'audit INALTÉRABLE (auteur, rôle, école,
     * identifiant de la période) est écrite par {@code JournalAuditInterceptor} pour toute écriture réussie
     * (2026-11-14 ; avant, la « trace au journal d'audit » annoncée n'allait que dans la console).
     */
    private void tracer(String action, PeriodeScolaireJpaEntity p, String description) {
        if (journal != null) {
            try { journal.logAdminAction(action, "ADMIN", p.getId(), description); } catch (RuntimeException ignore) { /* journal indisponible */ }
        }
    }

    static Map<String, Object> vue(PeriodeScolaireJpaEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("libelle", p.getLibelle());
        m.put("dateDebut", p.getDateDebut().toString());
        m.put("dateFin", p.getDateFin().toString());
        m.put("cloturee", p.isCloturee());
        m.put("clotureeLe", p.getClotureeLe() == null ? null : p.getClotureeLe().toString());
        return m;
    }
}
