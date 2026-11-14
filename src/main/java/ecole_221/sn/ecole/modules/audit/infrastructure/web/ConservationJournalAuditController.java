package ecole_221.sn.ecole.modules.audit.infrastructure.web;

import ecole_221.sn.ecole.modules.audit.application.service.ConservationJournalAuditService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Durée de conservation du journal d'audit (2026-11-14) — super-administrateur seulement (règle /v1/super-admin/**
 * de SecurityConfig). La modification est elle-même inscrite au journal (JournalAuditInterceptor).
 */
@RestController
@RequestMapping("/v1/super-admin/parametres/journal-audit")
public class ConservationJournalAuditController {

    private final ConservationJournalAuditService conservation;

    public ConservationJournalAuditController(ConservationJournalAuditService conservation) {
        this.conservation = conservation;
    }

    @Operation(summary = "Durée de conservation du journal d'audit",
            description = "Valeur, unité (minutes, heures, jours), date et auteur de la dernière modification.")
    @GetMapping
    public ResponseEntity<Map<String, Object>> lire() {
        return ResponseEntity.ok(conservation.lire());
    }

    @Operation(summary = "Modifier la durée de conservation",
            description = "Corps : {valeur, unite}. Entre 1 minute et 10 ans. Les entrées plus anciennes sont purgées à la minute suivante.")
    @PutMapping
    public ResponseEntity<Map<String, Object>> modifier(@RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(conservation.modifier(corps));
    }
}
