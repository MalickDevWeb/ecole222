package ecole_221.sn.ecole.modules.sync.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ecole_221.sn.ecole.modules.sync.application.port.usecase.SyncUseCase;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncAction;
import ecole_221.sn.ecole.modules.sync.domain.model.SyncResult;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sync")
@CrossOrigin(origins = "*")
public class SyncController {
    private static final Logger logger = LoggerFactory.getLogger(SyncController.class);

    private final SyncUseCase syncUseCase;
    private final ActionLogger actionLogger;

    public SyncController(SyncUseCase syncUseCase, ActionLogger actionLogger) {
        this.syncUseCase = syncUseCase;
        this.actionLogger = actionLogger;
    }

    @PostMapping
    public ResponseEntity<List<SyncResult>> synchronizeBatch(@RequestBody List<SyncAction> actions) {
        logger.info("POST /sync - Synchronisation de {} actions", actions.size());
        
        actionLogger.logSyncAction("SYNC_BATCH", "device-" + System.currentTimeMillis(), 
            "Synchronisation de " + actions.size() + " actions");
        
        List<SyncResult> results = syncUseCase.synchronize(actions);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, String>> checkVersion() {
        logger.info("GET /sync/check - Vérification version");
        return ResponseEntity.ok(Map.of("version", "2026.07.28"));
    }
}
