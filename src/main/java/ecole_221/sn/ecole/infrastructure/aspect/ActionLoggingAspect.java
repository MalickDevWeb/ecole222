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
 * (services, use cases, repositories) pour le débogage
 */
@Aspect
@Component
public class ActionLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ActionLoggingAspect.class);

    /**
     * Intercepte toutes les méthodes des classes dans les packages de service et use case
     */
    @Around("execution(* ecole_221.sn.ecole.modules..application..*(..)) || " +
            "execution(* ecole_221.sn.ecole.modules..domain..*(..)) || " +
            "execution(* ecole_221.sn.ecole.modules..infrastructure..persistence..*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String packageName = joinPoint.getTarget().getClass().getPackage().getName();
        
        // Logger l'entrée dans la méthode
        logger.debug("\n" + 
                "┌─────────────────────────────────────────────────────────────────┐\n" +
                "│ SERVICE/ACTION METHOD CALL                                    │\n" +
                "├─────────────────────────────────────────────────────────────────┤\n" +
                "│ Package:  {:<53} │\n" +
                "│ Class:    {:<53} │\n" +
                "│ Method:   {:<53} │\n" +
                "└─────────────────────────────────────────────────────────────────┘",
                packageName, className, methodName);
        
        // Logger les paramètres
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            logger.debug("Method parameters: {}", Arrays.toString(args));
        }
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Logger la sortie
            logger.debug("\n" + 
                    "┌─────────────────────────────────────────────────────────────────┐\n" +
                    "│ SERVICE/ACTION METHOD RETURN                                  │\n" +
                    "├─────────────────────────────────────────────────────────────────┤\n" +
                    "│ Class:    {:<53} │\n" +
                    "│ Method:   {:<53} │\n" +
                    "│ Duration: {:<52}ms │\n" +
                    "│ Return:   {:<53} │\n" +
                    "└─────────────────────────────────────────────────────────────────┘",
                    className, methodName, duration, 
                    result != null ? result.getClass().getSimpleName() : "void");
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("\n" + 
                    "┌─────────────────────────────────────────────────────────────────┐\n" +
                    "│ SERVICE/ACTION METHOD EXCEPTION                              │\n" +
                    "├─────────────────────────────────────────────────────────────────┤\n" +
                    "│ Class:    {:<53} │\n" +
                    "│ Method:   {:<53} │\n" +
                    "│ Duration: {:<52}ms │\n" +
                    "│ Error:    {:<53} │\n" +
                    "└─────────────────────────────────────────────────────────────────┘",
                    className, methodName, duration, e.getClass().getSimpleName() + ": " + e.getMessage());
            logger.error("Service method exception details:", e);
            throw e;
        }
    }

    /**
     * Intercepte spécifiquement les use cases (pour un logging plus détaillé)
     */
    @Around("execution(* ecole_221.sn.ecole.modules..application.port.usecase..*(..))")
    public Object logUseCaseMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        logger.info("\n" + 
                "╔════════════════════════════════════════════════════════════════╗\n" +
                "║ USE CASE METHOD CALL                                        ║\n" +
                "╠════════════════════════════════════════════════════════════════╣\n" +
                "║ Class:    {:<50} ║\n" +
                "║ Method:   {:<50} ║\n" +
                "╚════════════════════════════════════════════════════════════════╝",
                className, methodName);
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("\n" + 
                    "╔════════════════════════════════════════════════════════════════╗\n" +
                    "║ USE CASE METHOD RETURN                                      ║\n" +
                    "╠════════════════════════════════════════════════════════════════╣\n" +
                    "║ Class:    {:<50} ║\n" +
                    "║ Method:   {:<50} ║\n" +
                    "║ Duration: {:<49}ms ║\n" +
                    "╚════════════════════════════════════════════════════════════════╝",
                    className, methodName, duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("\n" + 
                    "╔════════════════════════════════════════════════════════════════╗\n" +
                    "║ USE CASE METHOD EXCEPTION                                  ║\n" +
                    "╠════════════════════════════════════════════════════════════════╣\n" +
                    "║ Class:    {:<50} ║\n" +
                    "║ Method:   {:<50} ║\n" +
                    "║ Duration: {:<49}ms ║\n" +
                    "║ Error:    {:<50} ║\n" +
                    "╚════════════════════════════════════════════════════════════════╝",
                    className, methodName, duration, e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        }
    }
}
