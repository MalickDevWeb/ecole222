package ecole_221.sn.ecole.infrastructure.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Filtre pour logger TOUTES les requêtes HTTP entrantes et sortantes
 * Cela permet de voir toutes les actions avec détails complets pour le débogage
 */
@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    // Méthodes HTTP pour lesquelles on veut logger le body
    private static final Set<String> BODY_LOGGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    
    // Headers sensibles à masquer
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "authorization", "cookie", "set-cookie", "x-auth-token", "password"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        long startTime = System.currentTimeMillis();
        
        // Wrapper la requête pour permettre plusieurs lectures du corps
        HttpServletRequest wrappedRequest = httpRequest;
        if (shouldLogBody(httpRequest)) {
            wrappedRequest = new CachedBodyHttpServletRequest(httpRequest);
        }
        
        // Lire et logger la requête entrante
        String requestBody = null;
        if (shouldLogBody(wrappedRequest)) {
            requestBody = readRequestBody(wrappedRequest);
        }
        
        String method = wrappedRequest.getMethod();
        String uri = wrappedRequest.getRequestURI();
        String queryString = wrappedRequest.getQueryString();
        String fullPath = queryString != null ? uri + "?" + queryString : uri;
        
        // Récupérer l'utilisateur si disponible
        String user = getUserInfo(wrappedRequest);
        
        // Récupérer les headers (en masquant les sensibles)
        Map<String, String> headers = getSanitizedHeaders(wrappedRequest);
        
        // Logger la requête entrante
        logger.info("╔════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        logger.info("║                         📥 INCOMING HTTP REQUEST - {} {}                                   ║", method, fullPath);
        logger.info("╠════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        logger.info("║ 📋 DETAILS:                                                                                  ║");
        logger.info("║   • Method:        {}                                                                     ║", method);
        logger.info("║   • Path:          {}                                                                     ║", uri);
        logger.info("║   • Query String:  {}                                                                     ║", queryString != null ? queryString : "N/A");
        logger.info("║   • User/Auth:     {}                                                                     ║", user);
        logger.info("║   • Remote Addr:   {}                                                                     ║", request.getRemoteAddr());
        logger.info("║   • Content Type:  {}                                                                     ║", wrappedRequest.getContentType());
        logger.info("║   • Content Length:{}                                                                     ║", wrappedRequest.getContentLength());
        logger.info("╠════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        logger.info("║ 📤 HEADERS:                                                                                  ║");
        headers.forEach((key, value) -> logger.info("║   • {}: {}{}", String.format("%-20s", key), 
            String.format("%-40s", value.length() > 40 ? value.substring(0, 40) + "..." : value), "  ║"));
        logger.info("╠════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        if (requestBody != null && !requestBody.isEmpty()) {
            logger.info("║ 📝 REQUEST BODY:                                                                            ║");
            logger.info("║ {}                                                                                      ║", truncateBody(requestBody));
        } else {
            logger.info("║ 📝 REQUEST BODY: [EMPTY or GET request]                                                      ║");
        }
        logger.info("╚════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
        
        // Continuer la chaîne de filtres avec la requête wrappée
        chain.doFilter(wrappedRequest, response);
        
        // Logger la réponse sortante
        long duration = System.currentTimeMillis() - startTime;
        int status = httpResponse.getStatus();
        
        logger.info("╔════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        logger.info("║                         📤 OUTGOING HTTP RESPONSE - {} {}                                  ║", method, fullPath);
        logger.info("╠════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        logger.info("║ 📋 DETAILS:                                                                                  ║");
        logger.info("║   • Status:        {} {}                                                                  ║", status, getStatusText(status));
        logger.info("║   • Duration:      {}ms                                                                    ║", duration);
        logger.info("║   • User/Auth:     {}                                                                     ║", user);
        logger.info("║   • Content Type:  {}                                                                     ║", httpResponse.getContentType());
        logger.info("╠════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        logger.info("║ 📊 PERFORMANCE METRICS:                                                                      ║");
        logger.info("║   • Request Time:   {}ms                                                                   ║", duration);
        if (duration > 1000) {
            logger.warn("║   ⚠️  SLOW REQUEST DETECTED (>1s)                                                           ║");
        } else if (duration > 500) {
            logger.warn("║   ⚠️  MODERATE REQUEST TIME (>500ms)                                                        ║");
        } else {
            logger.info("║   ✓ Response time acceptable                                                                ║");
        }
        logger.info("╚════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }

    private boolean shouldLogBody(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();
        if (contentType == null) {
            contentType = request.getHeader("Content-Type");
        }
        boolean isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
        return BODY_LOGGING_METHODS.contains(method.toUpperCase()) && !isMultipart;
    }

    private String readRequestBody(HttpServletRequest request) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                request.getInputStream(), StandardCharsets.UTF_8
            ));
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IllegalStateException e) {
            // Le stream a déjà été lu, on ne peut pas le relire
            return "[Body already consumed]";
        }
    }

    private String getUserInfo(HttpServletRequest request) {
        String user = request.getRemoteUser();
        if (user == null || user.isEmpty()) {
            user = request.getHeader("Authorization");
            if (user != null && user.startsWith("Bearer ")) {
                user = "[JWT Token]";
            }
        }
        if (user == null || user.isEmpty()) {
            user = request.getHeader("X-Forwarded-For");
        }
        if (user == null || user.isEmpty()) {
            user = request.getRemoteAddr();
        }
        return user != null ? user : "[Unknown]";
    }

    private Map<String, String> getSanitizedHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames())
            .forEach(headerName -> {
                String headerValue = request.getHeader(headerName);
                String sanitizedHeaderName = headerName.toLowerCase();
                
                // Masquer les headers sensibles
                if (SENSITIVE_HEADERS.contains(sanitizedHeaderName)) {
                    headers.put(headerName, "[REDACTED]");
                } else {
                    headers.put(headerName, headerValue);
                }
            });
        return headers;
    }

    private String truncateBody(String body) {
        int maxLength = 2000; // Limiter la taille du body dans les logs
        if (body.length() > maxLength) {
            return body.substring(0, maxLength) + "... [TRUNCATED]";
        }
        return body;
    }

    private String getStatusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "CREATED";
            case 204 -> "NO CONTENT";
            case 400 -> "BAD REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT FOUND";
            case 500 -> "INTERNAL SERVER ERROR";
            default -> "UNKNOWN";
        };
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("╔════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        logger.info("║                    🔧 REQUEST LOGGING FILTER INITIALIZED - ALL HTTP REQUESTS WILL BE LOGGED          ║");
        logger.info("╚════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }

    @Override
    public void destroy() {
        logger.info("╔════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        logger.info("║                    🔧 REQUEST LOGGING FILTER DESTROYED                                               ║");
        logger.info("╚════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }
}
