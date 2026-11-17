package ecole_221.sn.ecole.modules.audit.infrastructure.web;

import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Set;

/**
 * Journal d'audit EXHAUSTIF des écritures HTTP (BR-05, ENF-07, 2026-11-14).
 *
 * <p>Avant : le « journal » ActionLogger n'écrivait que dans les traces de l'application ; la table {@code audit_logs},
 * rendue inaltérable par la base (V11), ne recevait que de rares entrées. Désormais toute écriture RÉUSSIE (POST, PUT,
 * PATCH, DELETE, statut &lt; 400) d'un compte connecté y est inscrite : auteur (compte, nom, rôle), école (ou
 * « plateforme »), route avec ses identifiants, adresse IP, navigateur. Le contenu de la requête n'est jamais copié
 * (mots de passe, données personnelles).</p>
 *
 * <p>Sans compte, seules les écritures publiques qui créent ou changent quelque chose de durable sont inscrites
 * ({@link #ANONYMES_JOURNALISEES} : candidature, inscription d'une école, nouveau mot de passe par lien) : un anonyme
 * ne doit pas pouvoir remplir à volonté une table qu'on ne peut plus vider. Ne sont pas inscrites : la saisie des
 * identifiants, la demande de lien « mot de passe oublié » (même réponse que le compte existe ou non), la consultation
 * d'un dossier de candidature, l'envoi des pièces avant la candidature (la candidature elle-même est inscrite).</p>
 */
@Component
public class JournalAuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JournalAuditInterceptor.class);
    static final Set<String> ECRITURES = Set.of("POST", "PUT", "PATCH", "DELETE");
    /** Jamais inscrites ici : identifiants de connexion, et entrées déjà écrites explicitement. */
    static final Set<String> EXCLUES = Set.of("/auth/login", "/auth/login-token", "/audit/logs",
            "/candidatures/regles/evaluer");   // 16/11 : lecture (pièces exigées), aucune écriture
    static final Set<String> ANONYMES_JOURNALISEES = Set.of("/candidatures/submit", "/register-school",
            "/auth/reinitialiser-mot-de-passe");

    private final JournalAudit journal;

    public JournalAuditInterceptor(JournalAudit journal) {
        this.journal = journal;
    }

    /** POST sur un élément existant (« /admin/periodes/{id}/cloturer ») : modification, pas création. */
    static String typeAction(String methode, String motif) {
        return switch (methode) {
            case "POST" -> motif.contains("{") ? "UPDATE" : "CREATE";
            case "DELETE" -> "DELETE";
            default -> "UPDATE";
        };
    }

    /** Description lisible : « Modification — PUT /admin/students/{id}/toggle-status ». */
    static String description(String methode, String motif) {
        String verbe = switch (methode) {
            case "POST" -> motif.contains("{") ? "Action" : "Création ou action";
            case "DELETE" -> "Suppression";
            default -> "Modification";
        };
        return verbe + " — " + methode + " " + motif;
    }

    /** « /admin/students/{id}/toggle-status » → « admin/students » : au plus deux segments, avant tout identifiant. */
    static String typeEntite(String motif) {
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (String s : motif.split("/")) {
            if (s.isEmpty()) continue;
            if (s.startsWith("{") || n == 2) break;
            if (n++ > 0) b.append('/');
            b.append(s);
        }
        return b.toString();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            String methode = request.getMethod();
            if (!ECRITURES.contains(methode) || response.getStatus() >= 400 || ex != null) return;
            String chemin = request.getServletPath() == null || request.getServletPath().isEmpty()
                    ? request.getRequestURI().substring(request.getContextPath().length()) : request.getServletPath();
            if (EXCLUES.contains(chemin)) return;

            String acteurId = null, acteurNom = "anonyme (formulaire public)", role = null;
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User u) {
                acteurId = u.getId();
                acteurNom = ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim();
                if (acteurNom.isEmpty()) acteurNom = u.getEmail();
                role = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(a -> a.startsWith("ROLE_"))
                        .map(a -> a.substring(5)).findFirst().orElse(null);
            } else if (!ANONYMES_JOURNALISEES.contains(chemin)) {
                return;
            }

            Object attribut = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String motif = attribut instanceof String m ? m : chemin;
            @SuppressWarnings("unchecked")
            Map<String, String> variables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            String identifiants = variables == null || variables.isEmpty() ? null : String.join(",", variables.values());
            String entite = typeEntite(motif);
            journal.inscrire(new JournalAudit.Entree(
                    TenantContext.getCurrentOrganizationId().orElse(null), acteurId, acteurNom, role,
                    typeAction(methode, motif), entite, identifiants, description(methode, motif),
                    request.getRemoteAddr(), request.getHeader("User-Agent"), entite.split("/")[0]));
        } catch (RuntimeException e) {
            log.warn("Écriture non inscrite au journal d'audit : {}", e.getMessage());
        }
    }
}
