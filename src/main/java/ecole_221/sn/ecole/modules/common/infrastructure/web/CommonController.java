package ecole_221.sn.ecole.modules.common.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/common")
@CrossOrigin(origins = "*")
@Tag(name = "Common", description = "Endpoints communs - health check, notifications système")
public class CommonController {
    private static final Logger logger = LoggerFactory.getLogger(CommonController.class);

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Vérifie que le backend est opérationnel")
    public ResponseEntity<Map<String, String>> healthCheck() {
        logger.info("GET /common/health - Health check");
        return ResponseEntity.ok(Map.of(
            "status", "UP", 
            "message", "Backend École 221 is running !",
            "timestamp", java.time.Instant.now().toString()
        ));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Notifications système", description = "Retourne les notifications système (placeholder)")
    public ResponseEntity<List<Map<String, Object>>> getNotifications() {
        logger.info("GET /common/notifications - Récupération notifications système");
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/version")
    @Operation(summary = "Version API", description = "Retourne la version de l'API")
    public ResponseEntity<Map<String, String>> getVersion() {
        logger.info("GET /common/version - Récupération version API");
        return ResponseEntity.ok(Map.of(
            "version", "2026.08.12",
            "apiVersion", "v1",
            "description", "École 221 Backend API"
        ));
    }

    @GetMapping("/info")
    @Operation(summary = "Informations système", description = "Retourne les informations générales du système")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        logger.info("GET /common/info - Récupération informations système");
        Map<String, Object> info = Map.of(
            "application", "École 221",
            "version", "2026.08.12",
            "environment", System.getProperty("spring.profiles.active", "default"),
            "javaVersion", System.getProperty("java.version"),
            "operatingSystem", System.getProperty("os.name"),
            "uptime", ManagementFactory.getRuntimeMXBean().getUptime() + "ms"
        );
        return ResponseEntity.ok(info);
    }
}
