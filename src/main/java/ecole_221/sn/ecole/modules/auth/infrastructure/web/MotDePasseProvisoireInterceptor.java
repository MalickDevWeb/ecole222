package ecole_221.sn.ecole.modules.auth.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

/**
 * Tant que le compte connecté porte un mot de passe provisoire (drapeau {@code doitChangerMotDePasse}),
 * toute route métier répond 403 « Changez votre mot de passe provisoire ». Restent permis : le changement
 * de mot de passe, la connexion / déconnexion et la lecture du profil ({@code GET /auth/me}).
 */
@Component
public class MotDePasseProvisoireInterceptor implements HandlerInterceptor {

    public static final String CODE = "MOT_DE_PASSE_PROVISOIRE";
    public static final String MESSAGE = "Changez votre mot de passe provisoire";

    static final Set<String> ROUTES_PERMISES = Set.of(
            "/auth/changer-mot-de-passe", "/auth/logout", "/auth/me", "/auth/login", "/auth/login-token", "/error");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user) || !user.isDoitChangerMotDePasse()) {
            return true;
        }
        String chemin = request.getRequestURI().substring(request.getContextPath().length());
        if (chemin.length() > 1 && chemin.endsWith("/")) chemin = chemin.substring(0, chemin.length() - 1);
        if (ROUTES_PERMISES.contains(chemin)) return true;

        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"status\":403,\"code\":\"" + CODE + "\",\"message\":\""
                + MESSAGE + "\",\"error\":\"" + MESSAGE + "\",\"doitChangerMotDePasse\":true}");
        return false;
    }
}
