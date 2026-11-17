package ecole_221.sn.ecole.modules.archivage.application.format;

import java.util.List;

/**
 * Description d'une table dans l'archive : nom, catégorie (DIRECTE, APPARTENANCES, INDIRECTE), colonnes (nom et type
 * PostgreSQL) et clé primaire. Les valeurs sont rangées sous leur forme texte PostgreSQL : relues par
 * {@code CAST(valeur AS type)}, elles reviennent à l'identique (dates, tableaux, jsonb, nombres).
 */
public record TableArchivee(String nom, String categorie, List<Colonne> colonnes, List<String> cle) {

    public record Colonne(String nom, String type) {
    }

    public List<String> nomsColonnes() {
        return colonnes.stream().map(Colonne::nom).toList();
    }

    public int indexDe(String colonne) {
        for (int i = 0; i < colonnes.size(); i++) {
            if (colonnes.get(i).nom().equals(colonne)) return i;
        }
        return -1;
    }
}
