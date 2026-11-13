package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Identité visuelle de l'école (logo, couleur principale, fond de l'écran de connexion), enregistrée pour l'école
 * courante (2026-11-13). Avant : l'administration appelait une route réservée au super-administrateur (refusée) et
 * l'identité n'était gardée que dans le navigateur ; le logo partait en data URL de plusieurs Mo dans du JSON.
 */
@Service
public class IdentiteVisuelleService {

    private static final Pattern LIEN = Pattern.compile("^(?i)https?://\\S{1,1000}$");
    private static final Pattern COULEUR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final OrganizationRepositoryPort ecoles;

    public IdentiteVisuelleService(OrganizationRepositoryPort ecoles) {
        this.ecoles = ecoles;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lire() {
        return vers(ecoleCourante());
    }

    /** Champs présents seulement ; « » efface le logo ou le fond. 400 précis pour une valeur invalide. */
    @Transactional
    public Map<String, Object> enregistrer(Map<String, Object> corps) {
        if (corps == null || corps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune modification envoyée.");
        }
        // APP-QA-04 : aucun champ reconnu → refus (avant : « enregistré » sans rien changer)
        if (!corps.containsKey("logoUrl") && !corps.containsKey("backgroundUrl") && !corps.containsKey("primaryColor")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun champ reconnu : logoUrl, primaryColor ou backgroundUrl attendu.");
        }
        Organization ecole = ecoleCourante();
        Map<String, Object> reglages = new HashMap<>(ecole.getSettings() == null ? Map.of() : ecole.getSettings());
        if (corps.containsKey("logoUrl")) {
            ecole.setLogo(lien(corps.get("logoUrl"), "du logo"));
        }
        if (corps.containsKey("backgroundUrl")) {
            String fond = lien(corps.get("backgroundUrl"), "du fond d'écran");
            if (fond == null) reglages.remove("backgroundUrl"); else reglages.put("backgroundUrl", fond);
        }
        if (corps.containsKey("primaryColor")) {
            Object c = corps.get("primaryColor");
            if (!(c instanceof String s) || !COULEUR.matcher(s).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Couleur invalide : format attendu #RRGGBB (ex. #B3181C).");
            }
            reglages.put("primaryColor", s.toUpperCase(java.util.Locale.ROOT));
        }
        ecole.setSettings(reglages);
        return vers(ecoles.save(ecole));
    }

    private static String lien(Object valeur, String libelle) {
        if (valeur == null || (valeur instanceof String s && s.isBlank())) {
            return null;
        }
        if (!(valeur instanceof String s) || !LIEN.matcher(s.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lien " + libelle + " invalide : envoyez d'abord l'image (lien http(s), 1 000 caractères au maximum).");
        }
        return s.trim();
    }

    private Organization ecoleCourante() {
        String id = TenantContext.getCurrentOrganizationId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "École courante inconnue."));
        return ecoles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable."));
    }

    private static Map<String, Object> vers(Organization o) {
        Map<String, Object> r = new LinkedHashMap<>();
        Map<String, Object> s = o.getSettings() == null ? Map.of() : o.getSettings();
        r.put("logoUrl", o.getLogo());
        r.put("primaryColor", s.get("primaryColor"));
        r.put("backgroundUrl", s.get("backgroundUrl"));
        return r;
    }
}
