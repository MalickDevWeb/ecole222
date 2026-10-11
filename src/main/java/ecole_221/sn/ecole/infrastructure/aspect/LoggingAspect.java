package ecole_221.sn.ecole.infrastructure.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * Aspect AOP pour logger automatiquement toutes les méthodes des contrôleurs REST.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("within(@org.springframework.web.bind.annotation.RestController *) || " +
            "within(@org.springframework.stereotype.Controller *)")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        String basePath  = getClassRequestMapping(joinPoint.getTarget().getClass());
        HttpMethodInfo httpInfo = getHttpMethodInfo(joinPoint.getTarget().getClass(), methodName, methodSignature.getParameterTypes());
        String fullPath  = basePath + httpInfo.path;
        if (fullPath.isEmpty()) fullPath = "/" + className.replace("Controller", "").toLowerCase();

        logger.info("► CTRL  [{} {}]  {}.{}", httpInfo.httpMethod, fullPath, className, methodName);

        // Logger les paramètres (masquer les mots de passe)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            String argsStr = Arrays.toString(args);
            argsStr = argsStr.replaceAll("(?i)(password|motDePasse|mdp)[^,\\]]*", "$1=***");
            logger.debug("   args: {}", argsStr);
        }

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("◄ CTRL  [{} {}]  {}ms  → {}",
                    httpInfo.httpMethod, fullPath, duration,
                    result != null ? result.getClass().getSimpleName() : "void");
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("✗ CTRL  [{} {}]  {}ms  → {}: {}",
                    httpInfo.httpMethod, fullPath, duration,
                    e.getClass().getSimpleName(), e.getMessage());
            logger.error("  Stack trace:", e);
            throw e;
        }
    }

    private String getClassRequestMapping(Class<?> clazz) {
        RequestMapping m = clazz.getAnnotation(RequestMapping.class);
        if (m != null && m.value().length > 0) return m.value()[0];
        return "";
    }

    private HttpMethodInfo getHttpMethodInfo(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        try {
            java.lang.reflect.Method method = clazz.getMethod(methodName, paramTypes);
            GetMapping    gm = method.getAnnotation(GetMapping.class);    if (gm  != null) return new HttpMethodInfo("GET",    first(gm.value(),  gm.path()));
            PostMapping   pm = method.getAnnotation(PostMapping.class);   if (pm  != null) return new HttpMethodInfo("POST",   first(pm.value(),  pm.path()));
            PutMapping    pu = method.getAnnotation(PutMapping.class);    if (pu  != null) return new HttpMethodInfo("PUT",    first(pu.value(),  pu.path()));
            PatchMapping  pa = method.getAnnotation(PatchMapping.class);  if (pa  != null) return new HttpMethodInfo("PATCH",  first(pa.value(),  pa.path()));
            DeleteMapping dm = method.getAnnotation(DeleteMapping.class); if (dm  != null) return new HttpMethodInfo("DELETE", first(dm.value(),  dm.path()));
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            if (rm != null && rm.method().length > 0) {
                return new HttpMethodInfo(rm.method()[0].name(), first(rm.value(), rm.path()));
            }
        } catch (NoSuchMethodException ignored) {}
        return new HttpMethodInfo("?", "");
    }

    private String first(String[] a, String[] b) {
        if (a != null && a.length > 0) return a[0];
        if (b != null && b.length > 0) return b[0];
        return "";
    }

    private static class HttpMethodInfo {
        final String httpMethod, path;
        HttpMethodInfo(String httpMethod, String path) { this.httpMethod = httpMethod; this.path = path; }
    }
}
