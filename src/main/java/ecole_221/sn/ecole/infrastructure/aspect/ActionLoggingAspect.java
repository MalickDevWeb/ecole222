package ecole_221.sn.ecole.infrastructure.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect AOP pour logger automatiquement toutes les actions métiers
 * (services, use cases, repositories, controllers) pour le débogage
 */
@Aspect
@Component
public class ActionLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ActionLoggingAspect.class);

    /**
     * Intercepte toutes les méthodes : application, domain, web controllers, persistence
     */
    @Around("execution(* ecole_221.sn.ecole.modules..application..*(..)) || " +
            "execution(* ecole_221.sn.ecole.modules..domain..*(..)) || " +
            "execution(* ecole_221.sn.ecole.modules..infrastructure.web..*(..)) || " +
            "execution(* ecole_221.sn.ecole.modules..infrastructure.persistence..*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String pkg        = joinPoint.getTarget().getClass().getPackage().getName();

        // Logger l'entrée
        logger.info("→ CALL  [{}.{}]  pkg={}", className, methodName, pkg);

        // Logger les paramètres (masque les mots de passe)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            String argsStr = Arrays.toString(args);
            // Masquer les mots de passe dans les logs
            argsStr = argsStr.replaceAll("(?i)(password|motDePasse|mdp)[^,\\]]*", "$1=***");
            logger.info("   args: {}", argsStr);
        }

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("← OK    [{}.{}]  {}ms  → {}",
                    className, methodName, duration,
                    result != null ? result.getClass().getSimpleName() : "void");
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("✗ ERROR [{}.{}]  {}ms  → {}: {}",
                    className, methodName, duration,
                    e.getClass().getSimpleName(), e.getMessage());
            logger.error("  Stack trace:", e);
            throw e;
        }
    }

    /**
     * Intercepte spécifiquement les use cases (niveau INFO avec double-cadre)
     */
    @Around("execution(* ecole_221.sn.ecole.modules..application.port.usecase..*(..))")
    public Object logUseCaseMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        logger.info("╔══ USE-CASE CALL  [{}.{}]", className, methodName);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("╚══ USE-CASE OK    [{}.{}]  {}ms", className, methodName, duration);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("╚══ USE-CASE ERROR [{}.{}]  {}ms  → {}: {}",
                    className, methodName, duration,
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
