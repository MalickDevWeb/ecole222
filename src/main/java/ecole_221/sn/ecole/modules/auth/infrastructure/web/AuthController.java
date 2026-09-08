package ecole_221.sn.ecole.modules.auth.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.application.port.dto.LoginResponseDto;
import ecole_221.sn.ecole.modules.auth.application.port.usecase.LoginUseCase;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentification — login, logout")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String COOKIE_NAME = "auth_token";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 7; // 7 jours

    private final LoginUseCase loginUseCase;
    private final ActionLogger actionLogger;

    public AuthController(LoginUseCase loginUseCase, ActionLogger actionLogger) {
        this.loginUseCase = loginUseCase;
        this.actionLogger = actionLogger;
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur (Web)", description = "Authentifie un utilisateur, place le JWT dans un cookie HTTP-only pour l'application web.")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginCommand command,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        logger.info("POST /auth/login - Tentative de connexion pour: {}", command.getEmail());
        
        LoginResponseDto dto = null;
        boolean success = false;
        String errorMessage = null;
        
        try {
            dto = loginUseCase.login(command);
            success = true;
            
            boolean secureCookie = isSecureRequest(request);

            // Placer le JWT dans un cookie HTTP-only (inaccessible au JavaScript)
            Cookie cookie = new Cookie(COOKIE_NAME, dto.getToken());
            cookie.setHttpOnly(true);
            cookie.setSecure(secureCookie);
            cookie.setPath("/");
            cookie.setMaxAge(COOKIE_MAX_AGE);
            cookie.setAttribute("SameSite", secureCookie ? "None" : "Lax");
            response.addCookie(cookie);

            // Logger l'action réussie
            actionLogger.logAuthAction("LOGIN", command.getEmail(), dto.getRole() != null ? dto.getRole().name() : "UNKNOWN", true, 
                "Connexion réussie depuis IP: " + request.getRemoteAddr());

            // Ne pas exposer le token dans le body
            dto.setToken(null); 
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            actionLogger.logAuthAction("LOGIN", command.getEmail(), "UNKNOWN", false, 
                "Échec connexion - Erreur: " + errorMessage);
            throw e;
        }
    }

    @PostMapping("/login-token")
    @Operation(summary = "Connexion utilisateur (Token)", description = "Authentifie un utilisateur et retourne le JWT dans le body pour Swagger/OpenAPI.")
    public ResponseEntity<LoginResponseDto> loginToken(
            @RequestBody LoginCommand command,
            HttpServletRequest request
    ) {
        logger.info("POST /auth/login-token - Tentative de connexion pour: {}", command.getEmail());
        
        LoginResponseDto dto = null;
        boolean success = false;
        String errorMessage = null;
        
        try {
            dto = loginUseCase.login(command);
            success = true;

            // Logger l'action réussie
            actionLogger.logAuthAction("LOGIN_TOKEN", command.getEmail(), dto.getRole() != null ? dto.getRole().name() : "UNKNOWN", true, 
                "Connexion réussie (Token) depuis IP: " + request.getRemoteAddr());

            // Exposer le token dans le body pour Swagger
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            actionLogger.logAuthAction("LOGIN_TOKEN", command.getEmail(), "UNKNOWN", false, 
                "Échec connexion (Token) - Erreur: " + errorMessage);
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Efface le cookie d'authentification.")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        logger.info("POST /auth/logout - Déconnexion demandée");
        
        String userEmail = "unknown";
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                var user = (org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal();
                userEmail = user.getUsername();
            }
        } catch (Exception e) {
            // Ignore si pas d'utilisateur authentifié
        }
        
        boolean secureCookie = isSecureRequest(request);
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(0); // suppression immédiate
        cookie.setAttribute("SameSite", secureCookie ? "None" : "Lax");
        response.addCookie(cookie);
        
        actionLogger.logAuthAction("LOGOUT", userEmail, "UNKNOWN", true, 
            "Déconnexion réussie depuis IP: " + request.getRemoteAddr());
        
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil utilisateur", description = "Retourne les infos de l'utilisateur connecté (via cookie).")
    public ResponseEntity<Map<String, Object>> me() {
        logger.info("GET /auth/me - Récupération des infos utilisateur");
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("nom", user.getNom());
        profile.put("prenom", user.getPrenom());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole() != null ? user.getRole().name() : null);
        return ResponseEntity.ok(profile);
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }
}
