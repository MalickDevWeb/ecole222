package ecole_221.sn.ecole.modules.finance.domain.valueobject;

/**
 * Value Object : code de la catégorie d'une dépense (ex. MATERIEL).
 *
 * <p>La liste des catégories n'est plus écrite dans le code : elle est paramétrable dans le
 * référentiel {@code expense-category} (plateforme + surcharges par école). La validation se fait
 * à la CRÉATION via {@link ecole_221.sn.ecole.modules.finance.domain.port.ExpenseCategoryCatalog} ;
 * à la relecture, une catégorie depuis renommée ou désactivée reste lisible.</p>
 */
public record ExpenseCategory(String value) {

    public ExpenseCategory {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("La catégorie ne peut pas être vide");
        }
        value = value.trim();
    }
}
