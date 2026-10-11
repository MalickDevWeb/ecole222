package ecole_221.sn.ecole.modules.superadmin.application.port.usecase;

import ecole_221.sn.ecole.modules.superadmin.application.port.dto.TenantDto;

import java.util.List;

public interface ListTenantsUseCase {
    List<TenantDto> execute(String statusFilter);
}
