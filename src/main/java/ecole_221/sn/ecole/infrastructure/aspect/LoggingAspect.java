package ecole_221.sn.ecole.infrastructure.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Aspect AOP pour logger automatiquement toutes les méthodes des contrôleurs
 * Cela permet de voir toutes les actions avec détails complets pour le débogage
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Intercepte toutes les méthodes des classes annotées avec @RestController ou @Controller
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *) || " +
            "within(@org.springframework.stereotype.Controller *)")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        
        // Récupérer l'annotation RequestMapping de la classe
        String basePath = getClassRequestMapping(joinPoint.getTarget().getClass());
        
        // Récupérer l'annotation de la méthode (GetMapping, PostMapping, etc.)
        HttpMethodInfo httpMethodInfo = getHttpMethodInfo(joinPoint.getTarget().getClass(), methodName, methodSignature.getParameterTypes());
        
        String fullPath = basePath + httpMethodInfo.path;
        if (fullPath.isEmpty()) {
            fullPath = "/" + className.replace("Controller", "").toLowerCase();
        }
        
        // Logger l'entrée dans la méthode
        logger.info("\n" + 
                "╔════════════════════════════════════════════════════════════════╗\n" +
                "║ CONTROLLER METHOD CALL                                       ║\n" +
                "╠════════════════════════════════════════════════════════════════╣\n" +
                "║ Class:    {:<50} ║\n" +
                "║ Method:   {:<50} ║\n" +
                "║ HTTP:     {:<50} ║\n" +
                "║ Path:     {:<50} ║\n" +
                "╚════════════════════════════════════════════════════════════════╝",
                className, methodName, httpMethodInfo.httpMethod, fullPath);
        
        // Logger les paramètres
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            logger.debug("Method parameters: {}", Arrays.toString(args));
        }
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Logger la sortie
            logger.info("\n" + 
                    "╔════════════════════════════════════════════════════════════════╗\n" +
                    "║ CONTROLLER METHOD RETURN                                     ║\n" +
                    "╠════════════════════════════════════════════════════════════════╣\n" +
                    "║ Class:    {:<50} ║\n" +
                    "║ Method:   {:<50} ║\n" +
                    "║ Duration: {:<49}ms ║\n" +
                    "║ Return:   {:<50} ║\n" +
                    "╚════════════════════════════════════════════════════════════════╝",
                    className, methodName, duration, 
                    result != null ? result.getClass().getSimpleName() : "void");
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("\n" + 
                    "╔════════════════════════════════════════════════════════════════╗\n" +
                    "║ CONTROLLER METHOD EXCEPTION                                 ║\n" +
                    "╠════════════════════════════════════════════════════════════════╣\n" +
                    "║ Class:    {:<50} ║\n" +
                    "║ Method:   {:<50} ║\n" +
                    "║ Duration: {:<49}ms ║\n" +
                    "║ Error:    {:<50} ║\n" +
                    "╚════════════════════════════════════════════════════════════════╝",
                    className, methodName, duration, e.getClass().getSimpleName() + ": " + e.getMessage());
            logger.error("Full stack trace:", e);
            throw e;
        }
    }

    private String getClassRequestMapping(Class<?> clazz) {
        RequestMapping classMapping = clazz.getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            return classMapping.value()[0];
        }
        return "";
    }

    private HttpMethodInfo getHttpMethodInfo(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        try {
            java.lang.reflect.Method method = clazz.getMethod(methodName, parameterTypes);
            
            // Vérifier GetMapping
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                return new HttpMethodInfo("GET", getMapping.value().length > 0 ? getMapping.value()[0] : getMapping.path().length > 0 ? getMapping.path()[0] : "");
            }
            
            // Vérifier PostMapping
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                return new HttpMethodInfo("POST", postMapping.value().length > 0 ? postMapping.value()[0] : postMapping.path().length > 0 ? postMapping.path()[0] : "");
            }
            
            // Vérifier PutMapping
            PutMapping putMapping = method.getAnnotation(PutMapping.class);
            if (putMapping != null) {
                return new HttpMethodInfo("PUT", putMapping.value().length > 0 ? putMapping.value()[0] : putMapping.path().length > 0 ? putMapping.path()[0] : "");
            }
            
            // Vérifier PatchMapping
            PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
            if (patchMapping != null) {
                return new HttpMethodInfo("PATCH", patchMapping.value().length > 0 ? patchMapping.value()[0] : patchMapping.path().length > 0 ? patchMapping.path()[0] : "");
            }
            
            // Vérifier DeleteMapping
            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
            if (deleteMapping != null) {
                return new HttpMethodInfo("DELETE", deleteMapping.value().length > 0 ? deleteMapping.value()[0] : deleteMapping.path().length > 0 ? deleteMapping.path()[0] : "");
            }
            
            // Vérifier RequestMapping sur la méthode
            RequestMapping methodMapping = method.getAnnotation(RequestMapping.class);
            if (methodMapping != null && methodMapping.method().length > 0) {
                RequestMethod[] methods = methodMapping.method();
                String path = methodMapping.value().length > 0 ? methodMapping.value()[0] : methodMapping.path().length > 0 ? methodMapping.path()[0] : "";
                return new HttpMethodInfo(methods[0].name(), path);
            }
            
        } catch (NoSuchMethodException e) {
            logger.debug("Could not find method {}.{} for annotation inspection", clazz.getSimpleName(), methodName);
        }
        
        return new HttpMethodInfo("UNKNOWN", "");
    }

    private static class HttpMethodInfo {
        String httpMethod;
        String path;
        
        HttpMethodInfo(String httpMethod, String path) {
            this.httpMethod = httpMethod;
            this.path = path;
        }
    }
}
