package ecole_221.sn.ecole.modules.sync.application.port.usecase;

import ecole_221.sn.ecole.modules.sync.domain.model.SyncAction;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncResult;

import java.util.List;

public interface SyncUseCase {
    List<SyncResult> synchronize(List<SyncAction> actions);
}
