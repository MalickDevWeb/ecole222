package ecole_221.sn.ecole.infrastructure.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect pour logger les méthodes des services et use cases
 * Permet de déboguer toutes les actions métier avec détails complets
 */
@Aspect
@Component
public class MethodLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(MethodLoggingAspect.class);

    // Pointcuts pour les différents couches
    @Pointcut("within(ecole_221.sn.ecole.modules..application..*)")
    public void applicationLayer() {}

    @Pointcut("within(ecole_221.sn.ecole.modules..domain..*)")
    public void domainLayer() {}

    // Exclure l'infrastructure pour éviter les conflits avec les filtres
    // @Pointcut("within(ecole_221.sn.ecole.modules..infrastructure..*)")
    // public void infrastructureLayer() {}

    // Exclure l'infrastructure de configuration
    // @Pointcut("within(ecole_221.sn.ecole.infrastructure..*)")
    // public void infrastructureConfig() {}

    // Combiner tous les pointcuts
    @Pointcut("applicationLayer() || domainLayer()")
    public void allApplicationLayers() {}

    // Logger l'entrée et la sortie de toutes les méthodes
    @Around("allApplicationLayers()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Logger l'entrée de la méthode
        logger.info("========== METHOD ENTRY ==========");
        logger.info("Class: {}", className);
        logger.info("Method: {}", methodName);
        logger.info("Arguments: {}", formatArguments(args));
        logger.info("=================================");

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            // Exécuter la méthode
            result = joinPoint.proceed();
            
            // Logger le succès
            long duration = System.currentTimeMillis() - startTime;
            logger.info("========== METHOD EXIT (SUCCESS) ==========");
            logger.info("Class: {}", className);
            logger.info("Method: {}", methodName);
            logger.info("Duration: {}ms", duration);
            logger.info("Result: {}", formatResult(result));
            logger.info("==========================================");
            
            return result;
        } catch (Throwable e) {
            exception = e;
            long duration = System.currentTimeMillis() - startTime;
            
            // Logger l'erreur
            logger.error("========== METHOD EXIT (ERROR) ==========");
            logger.error("Class: {}", className);
            logger.error("Method: {}", methodName);
            logger.error("Duration: {}ms", duration);
            logger.error("Exception: {}", e.getClass().getSimpleName());
            logger.error("Message: {}", e.getMessage());
            logger.error("Stack Trace:", e);
            logger.error("=========================================");
            
            throw e;
        }
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatArgument(args[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatArgument(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        String argString = arg.toString();
        
        // Limiter la taille des arguments dans les logs
        if (argString.length() > 200) {
            return argString.substring(0, 200) + "... [TRUNCATED]";
        }
        
        // Masquer les données sensibles
        String lowerArg = argString.toLowerCase();
        if (lowerArg.contains("password") || lowerArg.contains("secret") || 
            lowerArg.contains("token") || lowerArg.contains("credential")) {
            return "[SENSITIVE_DATA_REDACTED]";
        }
        
        return argString;
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        String resultString = result.toString();
        
        // Limiter la taille du résultat dans les logs
        if (resultString.length() > 500) {
            return resultString.substring(0, 500) + "... [TRUNCATED]";
        }
        
        return resultString;
    }
}