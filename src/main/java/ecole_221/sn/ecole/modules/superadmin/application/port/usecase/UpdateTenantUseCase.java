package ecole_221.sn.ecole.modules.superadmin.application.port.usecase;

import ecole_221.sn.ecole.modules.superadmin.application.port.command.UpdateTenantCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.TenantDto;

public interface UpdateTenantUseCase {
    TenantDto execute(String tenantId, UpdateTenantCommand command);
}
