package ecole_221.sn.ecole.modules.finance.domain.port;

import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseCategory;

/**
 * Catalogue des catégories de dépenses autorisées pour l'école courante.
 * Implémenté en infrastructure à partir du référentiel paramétrable.
 */
public interface ExpenseCategoryCatalog {

    /**
     * Convertit une saisie (code « MATERIEL » ou libellé « Matériel ») en catégorie active.
     *
     * @throws IllegalArgumentException (HTTP 400) si la catégorie est inconnue ou désactivée
     */
    ExpenseCategory resolve(String input);
}
