package ecole_221.sn.ecole.modules.archivage.application;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Photographie du catalogue PostgreSQL du schéma public (tables, colonnes et types, clés primaires, clés étrangères,
 * index uniques). L'archivage se règle sur la base RÉELLE, pas sur une liste figée : une base créée par Hibernate puis
 * marquée V1 par Flyway (base de développement du Tech Lead) peut différer d'une base neuve.
 */
public record CatalogueBase(Map<String, List<Colonne>> colonnes, Map<String, List<String>> clesPrimaires,
                            List<CleEtrangere> clesEtrangeres, Map<String, List<IndexUnique>> indexUniques) {

    public record Colonne(String nom, String type, boolean obligatoire, boolean aUnDefaut, boolean generee) {
    }

    public record CleEtrangere(String nom, String table, String tableParente, String suppression) {
    }

    /** Index unique (clé primaire comprise) ; {@code expression} : index sur expression, non comparable colonne à colonne. */
    public record IndexUnique(String nom, List<String> colonnes, String predicat, boolean expression) {
    }

    public boolean existe(String table) {
        return colonnes.containsKey(table);
    }

    public Colonne colonne(String table, String nom) {
        for (Colonne c : colonnes.getOrDefault(table, List.of())) {
            if (c.nom().equals(nom)) return c;
        }
        return null;
    }

    public static CatalogueBase lire(JdbcTemplate jdbc) {
        Map<String, List<Colonne>> colonnes = new LinkedHashMap<>();
        jdbc.query("SELECT c.relname, a.attname, format_type(a.atttypid, a.atttypmod) AS type, a.attnotnull, a.atthasdef, "
                        + "a.attgenerated <> '' AS generee "
                        + "FROM pg_class c JOIN pg_attribute a ON a.attrelid = c.oid "
                        + "WHERE c.relnamespace = 'public'::regnamespace AND c.relkind IN ('r', 'p') "
                        + "AND a.attnum > 0 AND NOT a.attisdropped ORDER BY c.relname, a.attnum",
                rs -> {
                    colonnes.computeIfAbsent(rs.getString(1), k -> new ArrayList<>())
                            .add(new Colonne(rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getBoolean(5), rs.getBoolean(6)));
                });
        Map<String, List<String>> cles = new HashMap<>();
        Map<String, List<IndexUnique>> uniques = new HashMap<>();
        jdbc.query("SELECT c.relname, ix.indexrelid::regclass::text, ix.indisprimary, pg_get_expr(ix.indpred, ix.indrelid), "
                        + "ix.indexprs IS NOT NULL, "
                        + "(SELECT string_agg(a.attname, ',' ORDER BY k.rang) FROM unnest(ix.indkey::int2[]) WITH ORDINALITY AS k(numero, rang) "
                        + " JOIN pg_attribute a ON a.attrelid = ix.indrelid AND a.attnum = k.numero WHERE k.rang <= ix.indnkeyatts) "
                        + "FROM pg_index ix JOIN pg_class c ON c.oid = ix.indrelid "
                        + "WHERE c.relnamespace = 'public'::regnamespace AND ix.indisunique",
                rs -> {
                    String table = rs.getString(1);
                    List<String> cols = rs.getString(6) == null ? List.of() : List.of(rs.getString(6).split(","));
                    if (rs.getBoolean(3)) cles.put(table, cols);
                    uniques.computeIfAbsent(table, k -> new ArrayList<>())
                            .add(new IndexUnique(rs.getString(2), cols, rs.getString(4), rs.getBoolean(5)));
                });
        List<CleEtrangere> fks = new ArrayList<>();
        jdbc.query("SELECT conname, conrelid::regclass::text, confrelid::regclass::text, confdeltype::text FROM pg_constraint "
                        + "WHERE contype = 'f' AND connamespace = 'public'::regnamespace",
                rs -> {
                    fks.add(new CleEtrangere(rs.getString(1), sansSchema(rs.getString(2)), sansSchema(rs.getString(3)), rs.getString(4)));
                });
        return new CatalogueBase(colonnes, cles, fks, uniques);
    }

    private static String sansSchema(String nom) {
        String n = nom.startsWith("public.") ? nom.substring(7) : nom;
        return n.startsWith("\"") && n.endsWith("\"") ? n.substring(1, n.length() - 1) : n;
    }
}
