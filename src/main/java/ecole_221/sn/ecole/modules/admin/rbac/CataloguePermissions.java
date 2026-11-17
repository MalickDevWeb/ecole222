package ecole_221.sn.ecole.modules.admin.rbac;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalogue des permissions de l'espace administration, calqué sur les VRAIES routes du serveur.
 *
 * <p>Chaque module de l'espace administration a une permission de lecture et une d'écriture, rattachées aux
 * préfixes de routes qu'il utilise ; le serveur les applique (PermissionsInterceptor) aux rôles du personnel.</p>
 */
public final class CataloguePermissions {

    private CataloguePermissions() {
    }

    /** Module de l'espace administration. */
    public record Module(String code, String libelle, String description, List<String> prefixes) {
    }

    public static final List<Module> MODULES = List.of(
            new Module("eleves", "Élèves", "Annuaire, dossiers 360° et synthèse du suivi, justificatifs d'absence, messages des parents",
                    List.of("/admin/students", "/admin/justificatifs-absence", "/admin/messages-parents")),
            new Module("admissions", "Admissions", "Campagnes et dossiers de candidature",
                    List.of("/admissions", "/v1/admissions", "/candidatures", "/admin/candidatures")),
            new Module("finances", "Finances", "Frais, paiements, dépenses et rapports", List.of("/finance")),
            new Module("planning", "Classes et planning", "Classes, cours, emploi du temps, salles, demandes de report",
                    List.of("/admin/promotions", "/admin/courses", "/admin/schedule", "/admin/planning", "/admin/demandes-report", "/planning")),
            new Module("personnel", "Personnel", "Comptes du personnel et des enseignants, émargement",
                    List.of("/admin/staff", "/admin/professors", "/admin/attendance", "/admin/users")),
            new Module("securite", "Sécurité et audit", "Terminaux d'accès, journal d'audit, journal en direct, pointages d'arrivée et de départ",
                    List.of("/admin/terminaux", "/admin/gates", "/audit", "/admin/realtime-logs", "/admin/pointages")),
            new Module("communication", "Communication", "Annonces, notifications et e-mails de l'école, groupes WhatsApp",
                    List.of("/v1/notifications", "/notifications", "/email", "/admin/groupes-whatsapp")),
            new Module("ecole", "Établissement", "Tableau de bord, abonnement, coordonnées, identité visuelle, domaine propre, référentiels, trimestres et clôture des notes",
                    List.of("/admin/abonnement", "/admin/coordonnees", "/admin/identite-visuelle", "/admin/domaine", "/admin/domaines", "/admin/stats", "/dashboard", "/v1/referentiels",
                            "/admin/periodes")),
            new Module("collaboration", "Collaboration", "Groupes de travail, wiki, réunions", List.of("/collaboration", "/wiki", "/meet")),
            new Module("transferts", "Transferts d'élèves", "Demandes de transfert vers une autre école et décisions sur les demandes reçues",
                    List.of("/admin/transferts")));

    /** Rôles du personnel soumis à la matrice (l'administrateur a tous les droits ; enseignants, vigiles, élèves et
     *  parents ont leur propre espace). */
    public static final List<String> ROLES_PERSONNEL = List.of("DIRECTEUR", "SECRETAIRE", "COMPTABLE", "SURVEILLANT", "RH");

    public static final Map<String, String> LIBELLES_ROLES = Map.of("DIRECTEUR", "Directeur", "SECRETAIRE", "Secrétaire",
            "COMPTABLE", "Comptable", "SURVEILLANT", "Surveillant", "RH", "Ressources humaines");

    /** Rôles qui ont tous les droits de l'espace administration (non modifiables). */
    public static final Set<String> ROLES_ADMINISTRATION = Set.of("ADMIN", "ADMINISTRATOR", "SUPER_ADMIN", "PLATFORM_ADMIN");

    public static String lecture(String module) { return module + ".lire"; }
    public static String ecriture(String module) { return module + ".ecrire"; }

    /** Toutes les clés de permission, dans l'ordre d'affichage. */
    public static List<String> cles() {
        return MODULES.stream().flatMap(m -> java.util.stream.Stream.of(lecture(m.code()), ecriture(m.code()))).toList();
    }

    public static boolean cleConnue(String cle) {
        return cles().contains(cle);
    }

    /** Droits par défaut d'un rôle tant que l'école ne l'a pas configuré. */
    public static Set<String> parDefaut(String role) {
        return switch (role) {
            case "DIRECTEUR" -> Set.copyOf(cles());
            case "SECRETAIRE" -> Set.of("eleves.lire", "eleves.ecrire", "admissions.lire", "admissions.ecrire", "planning.lire",
                    "communication.lire", "communication.ecrire", "transferts.lire", "transferts.ecrire", "collaboration.lire", "collaboration.ecrire");
            case "COMPTABLE" -> Set.of("finances.lire", "finances.ecrire", "eleves.lire");
            case "SURVEILLANT" -> Set.of("eleves.lire", "securite.lire", "planning.lire");
            case "RH" -> Set.of("personnel.lire", "personnel.ecrire");
            default -> Set.of();
        };
    }

    /** Routes ouvertes à tout le personnel (lecture de ce qu'il faut pour afficher l'espace). */
    static boolean routeCommune(String methode, String chemin) {
        boolean lecture = "GET".equals(methode) || "HEAD".equals(methode);
        return chemin.startsWith("/auth/")
                || chemin.equals("/admin/rbac/moi")
                // Annonces : ses propres notifications, pour tout le personnel (sans droit « communication »)
                || (lecture && (chemin.equals("/v1/notifications/mes") || chemin.equals("/v1/notifications/mes/non-lues")))
                || ("PATCH".equals(methode) && chemin.matches("/v1/notifications/[^/]+/lue"))
                // Corbeille : ses propres suppressions ; chaque restauration exige ensuite le droit d'écriture
                // du module de l'élément (CorbeilleService), la purge reste réservée à l'administration
                || chemin.equals("/corbeille") || chemin.startsWith("/corbeille/")
                || (lecture && (chemin.startsWith("/planning") || chemin.startsWith("/v1/referentiels")
                    || chemin.startsWith("/admin/identite-visuelle") || chemin.startsWith("/admin/coordonnees")));
    }

    private static final List<Map.Entry<String, String>> PREFIXES_TRIES = MODULES.stream()
            .flatMap(m -> m.prefixes().stream().map(p -> Map.entry(p, m.code())))
            .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed())
            .toList();

    /** Permission exigée par une route (null si la route n'appartient à aucun module → refusée au personnel). */
    public static String permissionRequise(String methode, String chemin) {
        for (Map.Entry<String, String> e : PREFIXES_TRIES) {
            String p = e.getKey();
            if (chemin.equals(p) || chemin.startsWith(p + "/")) {
                boolean lecture = "GET".equals(methode) || "HEAD".equals(methode);
                return lecture ? lecture(e.getValue()) : ecriture(e.getValue());
            }
        }
        return null;
    }

    /** Description du catalogue pour l'écran « Droits d'accès ». */
    public static List<Map<String, Object>> description() {
        return MODULES.stream().flatMap(m -> java.util.stream.Stream.of(entree(m, false), entree(m, true))).toList();
    }

    private static Map<String, Object> entree(Module m, boolean ecriture) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("key", ecriture ? ecriture(m.code()) : lecture(m.code()));
        e.put("label", (ecriture ? "Modifier — " : "Consulter — ") + m.libelle());
        e.put("category", m.libelle());
        e.put("method", ecriture ? "ÉCRITURE" : "LECTURE");
        e.put("endpoint", String.join(", ", m.prefixes()));
        e.put("description", m.description());
        return e;
    }
}
