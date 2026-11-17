package ecole_221.sn.ecole.modules.corbeille.application;

import ecole_221.sn.ecole.modules.admin.periodes.PeriodesScolairesService;
import ecole_221.sn.ecole.modules.admin.rbac.CataloguePermissions;
import ecole_221.sn.ecole.modules.admin.rbac.PermissionsEcoleService;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import ecole_221.sn.ecole.partage.CompteCourant;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Corbeille (décision du Tech Lead du 15/11) : « si un professeur supprime une chose, elle est supprimée pour l'élève
 * et pour le professeur, mais s'il veut la restaurer il pourra » ; « pour les choses critiques, on doit savoir qui l'a
 * fait, quand ».
 *
 * <ul>
 *   <li>Mise à la corbeille : la ligne reçoit supprime_le, supprime_par et un libellé lisible ; l'entité porte
 *       {@code @SQLRestriction("supprime_le is null")}, donc plus aucune lecture JPA ne la voit (élève, parent,
 *       professeur, administration, exports, comptages).</li>
 *   <li>Liste : l'administration voit toute la corbeille de son école ; tout autre compte voit ce qu'il a supprimé.</li>
 *   <li>Restauration : par l'auteur de la suppression ou l'administration de l'école ; le personnel doit en plus avoir
 *       le droit d'écriture du module ; un professeur doit toujours enseigner le cours. 409 si l'élément dépend d'un
 *       parent supprimé (module, filière, classe, discussion, cours) ou d'une période clôturée.</li>
 *   <li>Purge définitive : automatique 30 jours après la suppression (PurgeCorbeille), ou manuelle par
 *       l'administration avec confirmation. Les tentatives d'un quiz et les rendus d'un devoir partent avec lui.</li>
 * </ul>
 *
 * <p>Toute requête est bornée par organization_id = école courante : l'élément d'une autre école donne 404. Chaque
 * suppression, restauration et purge est inscrite au journal d'audit avec son libellé (jamais le contenu d'un
 * message ni une note).</p>
 */
@Service
public class CorbeilleService {

    private static final Logger log = LoggerFactory.getLogger(CorbeilleService.class);
    static final String SYSTEME = "système (purge automatique de la corbeille)";

    private final JdbcTemplate jdbc;
    private final JournalAudit journal;
    private final TransactionTemplate transaction;
    private final ObjectProvider<PermissionsEcoleService> droits;
    private final ObjectProvider<PeriodesScolairesService> periodes;
    private final int conservationJours;

    public CorbeilleService(JdbcTemplate jdbc, JournalAudit journal, PlatformTransactionManager transactions,
                            ObjectProvider<PermissionsEcoleService> droits, ObjectProvider<PeriodesScolairesService> periodes,
                            @Value("${ecole.corbeille.conservation-jours:30}") int conservationJours) {
        this.jdbc = jdbc;
        this.journal = journal;
        this.transaction = new TransactionTemplate(transactions);
        this.droits = droits;
        this.periodes = periodes;
        this.conservationJours = conservationJours;
    }

    public int conservationJours() {
        return conservationJours;
    }

    // ================================================================ mise à la corbeille

    /**
     * Met l'élément {@code id} de l'école courante à la corbeille et l'inscrit au journal. À appeler à la place du
     * {@code delete} définitif, APRÈS les contrôles d'accès et de cohérence de l'appelant.
     *
     * @return le libellé lisible conservé
     */
    public String supprimer(TypeCorbeille type, String id) {
        String ecole = ecoleCourante();
        String libelle = libelle(type, id, ecole, false)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, type.libelle() + " introuvable."));
        int n = jdbc.update("UPDATE " + type.table() + " SET supprime_le = ?, supprime_par = ?, supprime_libelle = ? "
                        + "WHERE id = ? AND organization_id = ? AND supprime_le IS NULL",
                Timestamp.valueOf(LocalDateTime.now()), CompteCourant.id(), borne(libelle), id, ecole);
        if (n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, type.libelle() + " introuvable.");
        inscrire(ecole, CompteCourant.id(), nomActeur(), roleActeur(), "DELETE", type, id,
                "Mise à la corbeille : " + libelle + " (restaurable " + conservationJours + " jours)");
        return libelle;
    }

    /**
     * Met l'élément à la corbeille et renvoie la réponse HTTP commune des suppressions douces (2026-11-16 : avant,
     * salles, créneaux, filières, classes, évaluations, valeurs de référentiel et messages répondaient 204 sans corps,
     * et l'écran ne pouvait pas dire que l'élément restait restaurable) :
     * {@code {"id", "type", "libelle", "supprime": true, "corbeille": true, "message"}}.
     */
    public Map<String, Object> supprimerEtRepondre(TypeCorbeille type, String id) {
        return reponseSuppression(type, id, supprimer(type, id));
    }

    /** Réponse d'une mise à la corbeille déjà faite (même forme que {@link #supprimerEtRepondre}). */
    public static Map<String, Object> reponseSuppression(TypeCorbeille type, String id, String libelle) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("type", type.code());
        r.put("libelle", libelle);
        r.put("supprime", true);
        r.put("corbeille", true);
        r.put("message", type.messageMiseALaCorbeille(libelle));
        return r;
    }

    /** Refus explicite (409) de recréer sous le même identifiant un élément qui est dans la corbeille. */
    public void exigerHorsCorbeille(TypeCorbeille type, String id) {
        if (id == null) return;
        List<String> libelles = jdbc.queryForList("SELECT supprime_libelle FROM " + type.table()
                + " WHERE id = ? AND organization_id = ? AND supprime_le IS NOT NULL", String.class, id, ecoleCourante());
        if (!libelles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, libelles.get(0)
                    + " est dans la corbeille : " + (type.feminin() ? "restaurez-la" : "restaurez-le")
                    + " depuis la corbeille plutôt que de " + (type.feminin() ? "la" : "le") + " recréer.");
        }
    }

    /** Valeur de référentiel de l'école déjà dans la corbeille avec ce code (sinon doublon à la création). */
    public Optional<String> referentielEnCorbeille(String listCode, String code, String ecole) {
        return jdbc.queryForList("SELECT supprime_libelle FROM reference_values WHERE list_code = ? AND code = ? "
                + "AND organization_id = ? AND supprime_le IS NOT NULL", String.class, listCode, code, ecole).stream().findFirst();
    }

    // ================================================================ liste

    /** Corbeille de l'école courante : tout pour l'administration, ses propres suppressions pour les autres comptes. */
    public Map<String, Object> lister(String typeFiltre) {
        String ecole = ecoleCourante();
        List<TypeCorbeille> types = typeFiltre == null || typeFiltre.isBlank() ? List.of(TypeCorbeille.values())
                : List.of(TypeCorbeille.parCode(typeFiltre.trim()).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Type inconnu : " + typeFiltre + ". Types possibles : " + codes() + ".")));
        boolean admin = administration();
        String moi = CompteCourant.id();
        if (!admin && moi == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");

        List<Map<String, Object>> elements = new ArrayList<>();
        for (TypeCorbeille type : types) {
            String sql = "SELECT id, supprime_le, supprime_par, supprime_libelle FROM " + type.table()
                    + " WHERE organization_id = ? AND supprime_le IS NOT NULL" + (admin ? "" : " AND supprime_par = ?");
            List<Map<String, Object>> lignes = admin ? jdbc.queryForList(sql, ecole) : jdbc.queryForList(sql, ecole, moi);
            for (Map<String, Object> l : lignes) {
                String id = (String) l.get("id");
                LocalDateTime le = dateHeure(l.get("supprime_le"));
                String conflit = conflit(type, id, ecole);
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("type", type.code());
                e.put("typeLibelle", type.libelle());
                e.put("id", id);
                e.put("libelle", l.get("supprime_libelle"));
                e.put("supprimeLe", le);
                e.put("supprimePar", l.get("supprime_par"));
                e.put("supprimeParNom", null);
                e.put("purgeLe", le == null ? null : le.plusDays(conservationJours));
                e.put("peutRestaurer", admin || peutRestaurerSansAdministration(type, id, ecole, (String) l.get("supprime_par")));
                e.put("conflit", conflit);
                e.put("peutPurger", admin);
                elements.add(e);
            }
        }
        elements.sort(Comparator.comparing((Map<String, Object> e) -> (LocalDateTime) e.get("supprimeLe"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        nommerAuteurs(elements);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("elements", elements);
        r.put("total", elements.size());
        r.put("conservationJours", conservationJours);
        r.put("vueAdministration", admin);
        return r;
    }

    // ================================================================ restauration

    public Map<String, Object> restaurer(String typeCode, String id) {
        TypeCorbeille type = type(typeCode);
        String ecole = ecoleCourante();
        Map<String, Object> ligne = ligneSupprimee(type, id, ecole);
        String auteur = (String) ligne.get("supprime_par");
        if (!administration()) {
            String moi = CompteCourant.id();
            if (moi == null || !moi.equals(auteur)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Seul l'auteur de la suppression ou l'administration de l'école peut restaurer " + type.demonstratif() + ".");
            }
            exigerDroitsDuPersonnel(type);
            exigerEnseignantDuCours(type, id, ecole, moi);
        }
        String conflit = conflit(type, id, ecole);
        if (conflit != null) throw new ResponseStatusException(HttpStatus.CONFLICT, conflit);

        int n = jdbc.update("UPDATE " + type.table() + " SET supprime_le = NULL, supprime_par = NULL, supprime_libelle = NULL "
                + "WHERE id = ? AND organization_id = ? AND supprime_le IS NOT NULL", id, ecole);
        if (n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, type.libelle() + " introuvable dans la corbeille.");
        String libelle = (String) ligne.get("supprime_libelle");
        inscrire(ecole, CompteCourant.id(), nomActeur(), roleActeur(), "UPDATE", type, id, "Restauration depuis la corbeille : " + libelle);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", type.code());
        r.put("id", id);
        r.put("libelle", libelle);
        r.put("restaure", true);
        r.put("message", libelle + " a été " + type.accorde("restauré") + ".");
        return r;
    }

    // ================================================================ purge

    /** Purge manuelle par l'administration de l'école, avec confirmation explicite. */
    public Map<String, Object> purger(String typeCode, String id, boolean confirme) {
        TypeCorbeille type = type(typeCode);
        String ecole = ecoleCourante();
        if (!administration()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seule l'administration de l'école peut vider la corbeille.");
        }
        Map<String, Object> ligne = ligneSupprimee(type, id, ecole);
        if (!confirme) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmez la suppression définitive de « "
                    + ligne.get("supprime_libelle") + " » ({\"confirmer\": true}) : elle ne pourra plus être restaurée.");
        }
        int lies = purgerLigne(type, id, ecole, (String) ligne.get("supprime_libelle"), CompteCourant.id(), nomActeur(), roleActeur(),
                "Suppression définitive (vidée de la corbeille par l'administration)");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", type.code());
        r.put("id", id);
        r.put("libelle", ligne.get("supprime_libelle"));
        r.put("purge", true);
        r.put("elementsLies", lies);
        r.put("message", ligne.get("supprime_libelle") + " a été " + type.accorde("supprimé") + " définitivement"
                + (lies > 0 ? " avec " + lies(type, lies) + "." : "."));
        return r;
    }

    /** Purge automatique : tout ce qui est dans la corbeille depuis plus de {@code conservationJours} jours, toutes
     *  écoles confondues (chaque ligne garde son école dans le journal). */
    public int purgerExpires() {
        Timestamp limite = Timestamp.valueOf(LocalDateTime.now().minusDays(conservationJours));
        int total = 0;
        for (TypeCorbeille type : TypeCorbeille.values()) {
            List<Map<String, Object>> lignes = jdbc.queryForList("SELECT id, organization_id, supprime_libelle FROM " + type.table()
                    + " WHERE supprime_le IS NOT NULL AND supprime_le < ?", limite);
            for (Map<String, Object> l : lignes) {
                try {
                    purgerLigne(type, (String) l.get("id"), (String) l.get("organization_id"), (String) l.get("supprime_libelle"),
                            null, SYSTEME, null, "Suppression définitive (" + conservationJours + " jours dans la corbeille)");
                    total++;
                } catch (RuntimeException e) {
                    log.warn("Purge de la corbeille impossible pour {} {} : {}", type.code(), l.get("id"), e.getMessage());
                }
            }
        }
        return total;
    }

    /** Supprime la ligne et ses éléments liés, dans une transaction ; renvoie le nombre d'éléments liés supprimés. */
    private int purgerLigne(TypeCorbeille type, String id, String ecole, String libelle, String acteurId, String acteurNom,
                            String role, String motif) {
        Integer lies = transaction.execute(statut -> {
            int n = switch (type) {
                case QUIZ -> jdbc.update("DELETE FROM quiz_tentatives WHERE quiz_id = ? AND organization_id = ?", id, ecole);
                case DEVOIR -> jdbc.update("DELETE FROM rendus_devoirs WHERE homework_id = ? AND organization_id = ?", id, ecole);
                default -> 0;
            };
            int supprimees = jdbc.update("DELETE FROM " + type.table() + " WHERE id = ? AND organization_id = ? AND supprime_le IS NOT NULL",
                    id, ecole);
            if (supprimees == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, type.libelle() + " introuvable dans la corbeille.");
            return n;
        });
        int n = lies == null ? 0 : lies;
        inscrire(ecole, acteurId, acteurNom, role, "DELETE", type, id,
                motif + " : " + libelle + (n > 0 ? " — avec " + lies(type, n) : ""));
        return n;
    }

    /** « 1 tentative d'élève », « 3 tentatives d'élèves », « 2 rendus d'élèves » (pluriels réels, charte d'écriture). */
    static String lies(TypeCorbeille type, int n) {
        boolean un = n <= 1;
        return n + " " + switch (type) {
            case QUIZ -> un ? "tentative d'élève" : "tentatives d'élèves";
            case DEVOIR -> un ? "rendu d'élève" : "rendus d'élèves";
            default -> un ? "donnée liée" : "données liées";
        };
    }

    // ================================================================ conflits et droits

    /** Motif qui empêche la restauration (409), ou null. */
    String conflit(TypeCorbeille type, String id, String ecole) {
        if (type.contenuDeCours()) {
            List<String> cours = jdbc.queryForList("SELECT c.id FROM " + type.table() + " t JOIN courses c ON c.id = t.course_id "
                    + "AND c.organization_id = t.organization_id WHERE t.id = ? AND t.organization_id = ?", String.class, id, ecole);
            if (cours.isEmpty()) return "Le cours de " + type.demonstratif() + " n'existe plus : " + type.pronom()
                    + " ne peut pas être " + type.accorde("restauré") + ".";
        }
        return switch (type) {
            case SUPPORT, QUIZ -> parentSupprime(type, id, ecole, "module_id", "modules", "Le module");
            case CLASSE -> parentSupprime(type, id, ecole, "filiere_id", "filieres", "La filière");
            case CRENEAU -> parentSupprime(type, id, ecole, "classe_id", "classes", "La classe");
            case MESSAGE -> parentSupprime(type, id, ecole, "group_id", "group_chats", "La discussion");
            case EVALUATION -> conflitEvaluation(id, ecole);
            case REFERENTIEL -> {
                Integer doublons = jdbc.queryForObject("SELECT COUNT(*) FROM reference_values v JOIN reference_values t "
                        + "ON v.list_code = t.list_code AND v.code = t.code AND v.organization_id = t.organization_id AND v.id <> t.id "
                        + "WHERE t.id = ? AND t.organization_id = ? AND v.supprime_le IS NULL", Integer.class, id, ecole);
                yield doublons != null && doublons > 0 ? "Une valeur active porte déjà ce code : restaurer créerait un doublon." : null;
            }
            default -> null;
        };
    }

    /** Parent nommé par {@code colonne} : null s'il est visible (ou si la colonne est vide), sinon le motif du refus. */
    private String parentSupprime(TypeCorbeille type, String id, String ecole, String colonne, String tableParent, String nomParent) {
        List<String> parents = jdbc.queryForList("SELECT " + colonne + " FROM " + type.table() + " WHERE id = ? AND organization_id = ?",
                String.class, id, ecole);
        String parent = parents.isEmpty() ? null : parents.get(0);
        if (parent == null || parent.isBlank()) return null;
        boolean corbeille = !"group_chats".equals(tableParent);
        List<Map<String, Object>> p = jdbc.queryForList("SELECT " + (corbeille ? "supprime_le, supprime_libelle" : "id")
                + " FROM " + tableParent + " WHERE id = ? AND organization_id = ?", parent, ecole);
        if (p.isEmpty()) return nomParent + " de " + type.demonstratif() + " n'existe plus : " + type.pronom()
                + " ne peut pas être " + type.accorde("restauré") + ".";
        if (corbeille && p.get(0).get("supprime_le") != null) {
            // « La filière … » / « La classe … » : féminin ; « Le module … » : masculin
            return p.get(0).get("supprime_libelle") + " est dans la corbeille : "
                    + (nomParent.startsWith("La ") ? "restaurez-la" : "restaurez-le") + " d'abord.";
        }
        return null;
    }

    private String conflitEvaluation(String id, String ecole) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT date_evaluation, evaluation_rattrapee_id FROM evaluations_cours "
                + "WHERE id = ? AND organization_id = ?", id, ecole);
        if (l.isEmpty()) return null;
        Object rattrapee = l.get(0).get("evaluation_rattrapee_id");
        if (rattrapee != null) {
            List<Map<String, Object>> r = jdbc.queryForList("SELECT supprime_le, supprime_libelle FROM evaluations_cours "
                    + "WHERE id = ? AND organization_id = ?", rattrapee, ecole);
            if (r.isEmpty()) return "L'évaluation rattrapée n'existe plus : ce rattrapage ne peut pas être restauré.";
            if (r.get(0).get("supprime_le") != null) return r.get(0).get("supprime_libelle") + " est dans la corbeille : restaurez-la d'abord.";
        }
        LocalDate date = jour(l.get(0).get("date_evaluation"));
        PeriodesScolairesService p = periodes.getIfAvailable();
        if (p != null && date != null) {
            try {
                p.exigerOuverte(date, "ses évaluations ne peuvent plus être restaurées");
            } catch (ResponseStatusException e) {
                return e.getReason();
            }
        }
        return null;
    }

    private boolean peutRestaurerSansAdministration(TypeCorbeille type, String id, String ecole, String auteur) {
        String moi = CompteCourant.id();
        if (moi == null || !moi.equals(auteur)) return false;
        try {
            exigerDroitsDuPersonnel(type);
            exigerEnseignantDuCours(type, id, ecole, moi);
            return true;
        } catch (ResponseStatusException e) {
            return false;
        }
    }

    /** Personnel de l'école (matrice des droits) : droit d'écriture du module de l'élément. */
    private void exigerDroitsDuPersonnel(TypeCorbeille type) {
        String role = roles().stream().filter(CataloguePermissions.ROLES_PERSONNEL::contains).findFirst().orElse(null);
        if (role == null) return;
        PermissionsEcoleService d = droits.getIfAvailable();
        String permission = type.moduleDroits() == null ? null : CataloguePermissions.ecriture(type.moduleDroits());
        if (permission == null || d == null || !d.autorise(ecoleCourante(), role, permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le rôle " + CataloguePermissions.LIBELLES_ROLES.getOrDefault(role, role)
                    + (permission == null ? " ne peut pas restaurer " + type.demonstratif() + "." : " n'a pas la permission « " + permission + " » dans cette école."));
        }
    }

    /** Contenu de cours restauré par un professeur : il doit toujours enseigner ce cours. */
    private void exigerEnseignantDuCours(TypeCorbeille type, String id, String ecole, String moi) {
        if (!type.contenuDeCours()) return;
        List<String> profs = jdbc.queryForList("SELECT c.prof_id FROM " + type.table() + " t JOIN courses c ON c.id = t.course_id "
                + "AND c.organization_id = t.organization_id WHERE t.id = ? AND t.organization_id = ?", String.class, id, ecole);
        if (!profs.isEmpty() && !moi.equals(profs.get(0))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce cours n'est plus le vôtre : seule l'administration peut restaurer " + type.demonstratif() + ".");
        }
    }

    // ================================================================ outils

    private Map<String, Object> ligneSupprimee(TypeCorbeille type, String id, String ecole) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT supprime_par, supprime_libelle FROM " + type.table()
                + " WHERE id = ? AND organization_id = ? AND supprime_le IS NOT NULL", id, ecole);
        if (l.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, type.libelle() + " introuvable dans la corbeille.");
        return l.get(0);
    }

    private Optional<String> libelle(TypeCorbeille type, String id, String ecole, boolean supprime) {
        List<Map<String, Object>> l = jdbc.queryForList(type.selectLibelle() + " WHERE t.id = ? AND t.organization_id = ? AND t.supprime_le IS "
                + (supprime ? "NOT NULL" : "NULL"), id, ecole);
        if (l.isEmpty()) return Optional.empty();
        Map<String, Object> r = l.get(0);
        String titre = texte(r.get("titre"));
        String ctx1 = texte(r.get("ctx1"));
        String ctx2 = texte(r.get("ctx2"));
        StringBuilder b = new StringBuilder(type.libelle());
        switch (type) {
            case CRENEAU_HORAIRE -> b.append(" « ").append(joindre(" ", titre, joindre("-", ctx1, ctx2))).append(" »");
            case CRENEAU -> {
                if (titre != null) b.append(" “").append(court(titre)).append("”");
                String quand = joindre(" ", ctx1, joindre("-", texte(r.get("debut")), texte(r.get("fin"))));
                if (quand != null) b.append(" — ").append(quand);
                if (ctx2 != null) b.append(" (").append(ctx2).append(")");
            }
            case MESSAGE -> b.append(ctx1 != null ? " dans la discussion “" + court(ctx1) + "”" : " d'une discussion");
            case REFERENTIEL -> b.append(" “").append(court(titre)).append("” (").append(joindre(" / ", ctx1, ctx2)).append(")");
            default -> {
                if (titre != null) b.append(" “").append(court(titre)).append("”");
                if (ctx1 != null) b.append(" — ").append(ctx1);
                if (ctx2 != null) b.append(ctx1 != null ? " (" + ctx2 + ")" : " — " + ctx2);
            }
        }
        return Optional.of(b.toString());
    }

    private void nommerAuteurs(List<Map<String, Object>> elements) {
        Set<String> ids = new HashSet<>();
        elements.forEach(e -> { if (e.get("supprimePar") != null) ids.add((String) e.get("supprimePar")); });
        if (ids.isEmpty()) return;
        Map<String, String> noms = new HashMap<>();
        String marques = String.join(",", Collections.nCopies(ids.size(), "?"));
        jdbc.queryForList("SELECT id, prenom, nom FROM users WHERE id IN (" + marques + ")", ids.toArray()).forEach(u -> {
            String n = joindre(" ", texte(u.get("prenom")), texte(u.get("nom")));
            if (n != null) noms.put((String) u.get("id"), n);
        });
        elements.forEach(e -> e.put("supprimeParNom", noms.get(e.get("supprimePar"))));
    }

    private void inscrire(String ecole, String acteurId, String acteurNom, String role, String action, TypeCorbeille type,
                          String id, String description) {
        String ip = null, navigateur = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
            HttpServletRequest req = a.getRequest();
            ip = req.getRemoteAddr();
            navigateur = req.getHeader("User-Agent");
        }
        journal.inscrire(new JournalAudit.Entree(ecole, acteurId, acteurNom, role, action, "corbeille/" + type.code(), id,
                description, ip, navigateur, "corbeille"));
    }

    private static TypeCorbeille type(String code) {
        return TypeCorbeille.parCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Type d'élément inconnu dans la corbeille : " + code + "."));
    }

    private static String codes() {
        return String.join(", ", Arrays.stream(TypeCorbeille.values()).map(TypeCorbeille::code).toList());
    }

    static String ecoleCourante() {
        return TenantContext.getCurrentOrganizationId().filter(s -> !s.isBlank())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La corbeille se consulte depuis l'espace d'une école."));
    }

    private static Set<String> roles() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> r = new HashSet<>();
        if (auth != null) {
            for (GrantedAuthority a : auth.getAuthorities()) {
                if (a.getAuthority() != null && a.getAuthority().startsWith("ROLE_")) r.add(a.getAuthority().substring(5));
            }
        }
        return r;
    }

    /** Administration de l'école (tous les droits, non modifiables) : ADMIN, ADMINISTRATOR, SUPER_ADMIN, PLATFORM_ADMIN. */
    static boolean administration() {
        return roles().stream().anyMatch(CataloguePermissions.ROLES_ADMINISTRATION::contains);
    }

    private static String roleActeur() {
        return roles().stream().sorted().findFirst().orElse(null);
    }

    private static String nomActeur() {
        String n = CompteCourant.nomComplet();
        return n != null ? n : CompteCourant.id();
    }

    private static String texte(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String borne(String s) {
        return s == null || s.length() <= 255 ? s : s.substring(0, 254) + "…";
    }

    private static String court(String s) {
        if (s == null) return "";
        String u = s.replaceAll("\\s+", " ");
        return u.length() <= 80 ? u : u.substring(0, 79) + "…";
    }

    private static String joindre(String sep, String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return a + sep + b;
    }

    private static LocalDateTime dateHeure(Object o) {
        if (o instanceof Timestamp t) return t.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
        if (o instanceof LocalDateTime l) return l.truncatedTo(ChronoUnit.SECONDS);
        return null;
    }

    private static LocalDate jour(Object o) {
        if (o instanceof Date d) return d.toLocalDate();
        if (o instanceof LocalDate l) return l;
        if (o instanceof Timestamp t) return t.toLocalDateTime().toLocalDate();
        return null;
    }
}
