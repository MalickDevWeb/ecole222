package ecole_221.sn.ecole.shared.infrastructure.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Réponse JSON d'erreur des filtres d'école. Défaut F12 / point 10 de la revue DevSecOps (15/11) : le corps est
 * produit par un sérialiseur JSON — un identifiant recopié de la requête (X-Tenant-Slug…) ne peut plus casser ni
 * enrichir le JSON. Avant : concaténation de chaînes (un guillemet dans l'en-tête ajoutait des champs).
 */
final class ReponseErreurTenant {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ReponseErreurTenant() {
    }

    static void ecrire(ServletResponse response, int statut, String code, String erreur, String message) throws IOException {
        HttpServletResponse http = (HttpServletResponse) response;
        if (http.isCommitted()) {
            return;
        }
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("success", false);
        corps.put("status", statut);
        corps.put("code", code);
        corps.put("error", erreur);
        corps.put("message", message);
        http.setStatus(statut);
        http.setContentType("application/json;charset=UTF-8");
        http.getWriter().write(JSON.writeValueAsString(corps));
    }
}
