package ecole_221.sn.ecole.modules.superadmin.application.port.usecase;

import ecole_221.sn.ecole.modules.superadmin.application.port.dto.TenantDto;

public interface GetTenantUseCase {
    TenantDto execute(String tenantId);
}
