package ecole_221.sn.ecole.modules.superadmin.application.port.usecase;

import ecole_221.sn.ecole.modules.superadmin.application.port.dto.TenantDto;

public interface ActivateTenantUseCase {
    TenantDto execute(String tenantId);
}
