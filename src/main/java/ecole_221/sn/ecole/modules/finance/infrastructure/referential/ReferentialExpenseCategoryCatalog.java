package ecole_221.sn.ecole.modules.finance.infrastructure.referential;

import ecole_221.sn.ecole.modules.finance.domain.port.ExpenseCategoryCatalog;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseCategory;
import ecole_221.sn.ecole.modules.referential.application.service.ReferenceQueryService;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceList;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Component;

/** Adaptateur : les catégories de dépenses viennent du référentiel « expense-category ». */
@Component
public class ReferentialExpenseCategoryCatalog implements ExpenseCategoryCatalog {

    private final ReferenceQueryService referentials;

    public ReferentialExpenseCategoryCatalog(ReferenceQueryService referentials) {
        this.referentials = referentials;
    }

    @Override
    public ExpenseCategory resolve(String input) {
        String organizationId = TenantContext.getCurrentOrganizationId().orElse(null);
        return new ExpenseCategory(referentials.resolveCode(ReferenceList.EXPENSE_CATEGORY, input, organizationId));
    }
}
