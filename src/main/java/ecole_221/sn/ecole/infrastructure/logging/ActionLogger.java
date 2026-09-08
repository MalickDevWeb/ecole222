package ecole_221.sn.ecole.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Logger centralisé pour les actions métier importantes
 * Permet de tracer toutes les actions critiques avec contexte complet
 */
@Component
public class ActionLogger {

    private static final Logger logger = LoggerFactory.getLogger(ActionLogger.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log une action d'authentification
     */
    public void logAuthAction(String action, String email, String role, boolean success, String details) {
        logAction("AUTHENTICATION", action, Map.of(
            "email", email,
            "role", role,
            "success", success,
            "details", details
        ));
    }

    /**
     * Log une action d'étudiant
     */
    public void logStudentAction(String action, String studentId, String studentName, String details) {
        logAction("STUDENT", action, Map.of(
            "studentId", studentId,
            "studentName", studentName,
            "details", details
        ));
    }

    /**
     * Log une action de professeur
     */
    public void logProfessorAction(String action, String professorId, String professorName, String courseId, String details) {
        logAction("PROFESSOR", action, Map.of(
            "professorId", professorId,
            "professorName", professorName,
            "courseId", courseId,
            "details", details
        ));
    }

    /**
     * Log une action d'administration
     */
    public void logAdminAction(String action, String adminId, String target, String details) {
        logAction("ADMIN", action, Map.of(
            "adminId", adminId,
            "target", target,
            "details", details
        ));
    }

    /**
     * Log une action de sécurité
     */
    public void logSecurityAction(String action, String userId, String location, String details) {
        logAction("SECURITY", action, Map.of(
            "userId", userId,
            "location", location,
            "details", details
        ));
    }

    /**
     * Log une action de collaboration
     */
    public void logCollaborationAction(String action, String userId, String groupId, String details) {
        logAction("COLLABORATION", action, Map.of(
            "userId", userId,
            "groupId", groupId,
            "details", details
        ));
    }

    /**
     * Log une action de média/upload
     */
    public void logMediaAction(String action, String userId, String fileName, String details) {
        logAction("MEDIA", action, Map.of(
            "userId", userId,
            "fileName", fileName,
            "details", details
        ));
    }

    /**
     * Log une action de synchronisation
     */
    public void logSyncAction(String action, String deviceId, String details) {
        logAction("SYNC", action, Map.of(
            "deviceId", deviceId,
            "details", details
        ));
    }

    /**
     * Log une erreur système
     */
    public void logSystemError(String component, String errorType, String errorMessage, Throwable throwable) {
        logAction("SYSTEM_ERROR", errorType, Map.of(
            "component", component,
            "errorMessage", errorMessage,
            "stackTrace", throwable != null ? getStackTrace(throwable) : "N/A"
        ));
    }

    /**
     * Méthode générique pour logger une action
     */
    private void logAction(String category, String action, Map<String, Object> context) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        
        logger.info("╔════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        logger.info("║                           🎯 ACTION LOGGED - {} : {}                                         ║", category, action);
        logger.info("╠════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        logger.info("║ 📅 TIMESTAMP: {}                                                                           ║", timestamp);
        logger.info("║ 🏷️  CATEGORY:   {}                                                                         ║", category);
        logger.info("║ ⚡ ACTION:     {}                                                                         ║", action);
        logger.info("╠════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        logger.info("║ 📋 CONTEXT:                                                                                  ║");
        
        context.forEach((key, value) -> {
            String formattedValue = formatValue(value);
            logger.info("║   • {}: {}{}", String.format("%-15s", key), 
                String.format("%-40s", formattedValue.length() > 40 ? formattedValue.substring(0, 40) + "..." : formattedValue), "  ║");
        });
        
        logger.info("╚════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        String stringValue = value.toString();
        
        // Masquer les données sensibles
        String lowerValue = stringValue.toLowerCase();
        if (lowerValue.contains("password") || lowerValue.contains("secret") || 
            lowerValue.contains("token") || lowerValue.contains("credential")) {
            return "[SENSITIVE_DATA_REDACTED]";
        }
        
        // Limiter la taille
        if (stringValue.length() > 100) {
            return stringValue.substring(0, 100) + "... [TRUNCATED]";
        }
        
        return stringValue;
    }

    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 500) {
                sb.append("... [TRUNCATED]");
                break;
            }
        }
        return sb.toString();
    }
}