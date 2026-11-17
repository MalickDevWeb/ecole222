package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.application.PlanArchivage.Categorie;
import ecole_221.sn.ecole.modules.archivage.application.PlanArchivage.TablePlan;
import ecole_221.sn.ecole.modules.archivage.application.format.TableArchivee;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Registre des tables de l'archivage (étude d'archivage, §§ 1.2, 4.4 et 9.2 ; décisions du Tech Lead du 16/11 :
 * seules les données métier partent ; comptes, audit, domaines et transferts restent ; pour un compte rattaché à
 * plusieurs écoles, seul le rattachement part).
 *
 * <p>Complétude garantie par construction : toute table qui porte organization_id part, sauf celles nommées dans
 * {@link #GARDEES} ; une table SANS organization_id qui n'est ni gardée ni rattachée par une table parente connue
 * BLOQUE l'archivage (elle resterait orpheline). Une clé étrangère d'une table qui reste vers une table qui part
 * bloque aussi (la purge échouerait ou effacerait en cascade des lignes non archivées).</p>
 */
public final class RegistreTables {

    /** Restent dans la base principale (étude, § 9.2). */
    public static final Set<String> GARDEES = Set.of(
            "organizations",                   // pierre tombale : réserve le slug, porte le statut ARCHIVED
            "users",                           // comptes : unicité e-mail, téléphone, matricule, INE ; communs à plusieurs écoles
            "domaines_ecoles",                 // unicité du nom d'hôte
            "demandes_transfert",              // concerne deux écoles
            "audit_logs",                      // inaltérable (V11, V14), soumis à sa propre conservation
            "subscriptions",                   // facturation de la plateforme
            "reinitialisations_mot_de_passe",  // liées aux comptes, courte durée de vie
            "invalidations_jetons",
            "jetons_revoques",
            "annonces_ecole",                  // étude § 9.2 : petites tables liées à organizations
            "demandes_aide_connexion",
            "plans", "entitlements", "permissions", "parametres_plateforme",
            "flyway_schema_history", "archivages_ecoles", "archivages_ecoles_journal");

    /** Rattachements : seules les appartenances des comptes qui ont aussi une autre école partent. */
    public static final String APPARTENANCES = "organization_users";

    /** Tables sans organization_id, rattachées à l'école par une table parente. */
    public static final Map<String, Parent> INDIRECTES = Map.of(
            "group_chat_admins", new Parent("chat_id", "group_chats", "id"),
            "group_chat_participants", new Parent("chat_id", "group_chats", "id"),
            "role_permissions", new Parent("role_id", "roles", "id"));

    public record Parent(String colonne, String table, String colonneParente) {
    }

    private RegistreTables() {
    }

    static boolean colonneMedia(String nom) {
        return nom.equals("url") || nom.endsWith("_url") || nom.equals("logo");
    }

    /** Plan d'archivage sur le catalogue réel ; refus explicite (REFUS) si une table n'est pas classée. */
    public static PlanArchivage plan(CatalogueBase catalogue) {
        List<String> nonClassees = new ArrayList<>();
        List<String> gardees = new ArrayList<>();
        Map<String, TablePlan> partent = new LinkedHashMap<>();
        for (String table : new TreeSet<>(catalogue.colonnes().keySet())) {
            List<CatalogueBase.Colonne> cols = catalogue.colonnes().get(table);
            boolean ecole = cols.stream().anyMatch(c -> c.nom().equals("organization_id"));
            if (GARDEES.contains(table)) {
                gardees.add(table);
            } else if (table.equals(APPARTENANCES)) {
                if (!ecole || catalogue.colonne(table, "user_id") == null) {
                    throw ArchivageException.refus("Table organization_users inattendue (organization_id ou user_id absent).");
                }
                partent.put(table, tablePlan(catalogue, table, Categorie.APPARTENANCES,
                        "t.organization_id = ? AND EXISTS (SELECT 1 FROM public.organization_users o "
                                + "WHERE o.user_id = t.user_id AND o.organization_id <> t.organization_id)", 1));
            } else if (INDIRECTES.containsKey(table)) {
                Parent p = INDIRECTES.get(table);
                if (catalogue.colonne(table, p.colonne()) == null || catalogue.colonne(p.table(), "organization_id") == null) {
                    nonClassees.add(table);
                    continue;
                }
                partent.put(table, tablePlan(catalogue, table, Categorie.INDIRECTE,
                        "t." + ident(p.colonne()) + " IN (SELECT p." + ident(p.colonneParente()) + " FROM public."
                                + ident(p.table()) + " p WHERE p.organization_id = ?)", 1));
            } else if (ecole) {
                partent.put(table, tablePlan(catalogue, table, Categorie.DIRECTE, "t.organization_id = ?", 1));
            } else {
                nonClassees.add(table);
            }
        }
        if (!nonClassees.isEmpty()) {
            throw ArchivageException.refus("Archivage impossible : table(s) non classée(s) " + String.join(", ", nonClassees)
                    + ". À ajouter au registre de l'archivage.");
        }
        // Une table qui reste (ou ne part qu'en partie) ne doit pas pointer vers une table qui part.
        List<String> bloquantes = new ArrayList<>();
        Map<String, Set<String>> parents = new HashMap<>();
        for (CatalogueBase.CleEtrangere fk : catalogue.clesEtrangeres()) {
            if (!partent.containsKey(fk.tableParente()) || fk.table().equals(fk.tableParente())) continue;
            TablePlan enfant = partent.get(fk.table());
            if (enfant == null || enfant.categorie() == Categorie.APPARTENANCES) {
                bloquantes.add(fk.table() + " → " + fk.tableParente());
            } else {
                parents.computeIfAbsent(fk.table(), k -> new HashSet<>()).add(fk.tableParente());
            }
        }
        if (!bloquantes.isEmpty()) {
            throw ArchivageException.refus("Archivage impossible : clé(s) étrangère(s) d'une table gardée vers une table archivée ("
                    + String.join(", ", bloquantes) + ").");
        }
        for (Map.Entry<String, Parent> e : INDIRECTES.entrySet()) {
            if (partent.containsKey(e.getKey()) && partent.containsKey(e.getValue().table())) {
                parents.computeIfAbsent(e.getKey(), k -> new HashSet<>()).add(e.getValue().table());
            }
        }
        return new PlanArchivage(ordreEnfantsDabord(partent, parents), gardees);
    }

    /** Tri topologique : une table passe avant ses tables parentes (ordre de la purge ; l'insertion le renverse). */
    static List<TablePlan> ordreEnfantsDabord(Map<String, TablePlan> tables, Map<String, Set<String>> parents) {
        Map<String, Integer> enfantsRestants = new HashMap<>();
        for (String t : tables.keySet()) enfantsRestants.put(t, 0);
        for (Map.Entry<String, Set<String>> e : parents.entrySet()) {
            for (String p : e.getValue()) enfantsRestants.merge(p, 1, Integer::sum);
        }
        List<TablePlan> ordre = new ArrayList<>();
        TreeSet<String> prets = new TreeSet<>();
        enfantsRestants.forEach((t, n) -> { if (n == 0) prets.add(t); });
        while (!prets.isEmpty()) {
            String t = prets.pollFirst();
            ordre.add(tables.get(t));
            for (String p : parents.getOrDefault(t, Set.of())) {
                if (enfantsRestants.merge(p, -1, Integer::sum) == 0) prets.add(p);
            }
        }
        if (ordre.size() != tables.size()) {
            throw ArchivageException.refus("Archivage impossible : clés étrangères circulaires entre tables archivées.");
        }
        return ordre;
    }

    private static TablePlan tablePlan(CatalogueBase catalogue, String table, Categorie categorie, String selection, int parametres) {
        List<TableArchivee.Colonne> colonnes = new ArrayList<>();
        List<String> medias = new ArrayList<>();
        for (CatalogueBase.Colonne c : catalogue.colonnes().get(table)) {
            if (c.generee()) continue;   // recalculée par la base, jamais réécrite
            colonnes.add(new TableArchivee.Colonne(c.nom(), c.type()));
            if (colonneMedia(c.nom())) medias.add(c.nom());
        }
        List<String> cle = catalogue.clesPrimaires().getOrDefault(table, List.of());
        Map<String, String> types = new LinkedHashMap<>();
        for (String k : cle) types.put(k, catalogue.colonne(table, k).type());
        return new TablePlan(table, categorie, List.copyOf(colonnes), List.copyOf(cle), types, selection, parametres, List.copyOf(medias));
    }

    /** Identifiant SQL cité (noms issus du catalogue, jamais d'une requête). */
    static String ident(String nom) {
        return "\"" + nom.replace("\"", "\"\"") + "\"";
    }
}
