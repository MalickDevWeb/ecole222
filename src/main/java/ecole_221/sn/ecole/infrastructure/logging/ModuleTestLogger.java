package ecole_221.sn.ecole.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger spécialisé pour les tests de modules avec écriture dans des fichiers .log
 */
@Component
public class ModuleTestLogger {
    
    private static final String LOG_DIR = "logs/module_tests/";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public void logModuleTest(String moduleName, String operation, boolean success, String details, Throwable error) {
        String logFileName = LOG_DIR + moduleName.toLowerCase() + ".log";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String status = success ? "✅ SUCCESS" : "❌ ERROR";
        
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("[").append(timestamp).append("] ");
        logEntry.append("[").append(status).append("] ");
        logEntry.append("[").append(operation).append("] ");
        logEntry.append(details);
        
        if (error != null) {
            logEntry.append("\nERROR: ").append(error.getMessage());
            logEntry.append("\nSTACK TRACE:\n");
            for (StackTraceElement element : error.getStackTrace()) {
                logEntry.append("\t").append(element.toString()).append("\n");
            }
        }
        
        writeToFile(logFileName, logEntry.toString());
        
        // Aussi logger dans SLF4J
        Logger logger = LoggerFactory.getLogger("ModuleTest." + moduleName);
        if (success) {
            logger.info("[{}] {} - {}", operation, status, details);
        } else {
            logger.error("[{}] {} - {}", operation, status, details, error);
        }
    }
    
    public void logCrudOperation(String moduleName, String entity, String operation, Object data, boolean success, String error) {
        String details = String.format("Entity: %s, Operation: %s, Data: %s", entity, operation, data);
        logModuleTest(moduleName, "CRUD_" + operation, success, details, success ? null : new RuntimeException(error));
    }
    
    public void logPerformance(String moduleName, String operation, long durationMs, boolean acceptable) {
        String details = String.format("Operation: %s, Duration: %dms, Acceptable: %s", 
                                     operation, durationMs, acceptable);
        logModuleTest(moduleName, "PERFORMANCE", acceptable, details, null);
    }
    
    public void logDataIntegrity(String moduleName, String check, boolean passed, String details) {
        String logDetails = String.format("Check: %s, Passed: %s, Details: %s", check, passed, details);
        logModuleTest(moduleName, "DATA_INTEGRITY", passed, logDetails, null);
    }
    
    private void writeToFile(String fileName, String content) {
        try {
            // Créer le répertoire si nécessaire
            java.io.File logDir = new java.io.File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // Écrire dans le fichier
            try (FileWriter fw = new FileWriter(fileName, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(content);
                pw.flush();
            }
        } catch (IOException e) {
            Logger logger = LoggerFactory.getLogger(ModuleTestLogger.class);
            logger.error("Impossible d'écrire dans le fichier de log: {}", fileName, e);
        }
    }
    
    public void clearModuleLogs(String moduleName) {
        String logFileName = LOG_DIR + moduleName.toLowerCase() + ".log";
        try {
            new java.io.File(logFileName).delete();
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(ModuleTestLogger.class);
            logger.warn("Impossible de supprimer le fichier de log: {}", logFileName);
        }
    }
}