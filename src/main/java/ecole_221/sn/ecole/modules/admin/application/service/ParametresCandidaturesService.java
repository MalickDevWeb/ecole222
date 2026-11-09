package ecole_221.sn.ecole.modules.admin.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ParametresCandidaturesJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ParametresCandidaturesJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Paramètres de candidature de l'école courante, persistés (table parametres_candidatures).
 * Utilisé par /candidatures/settings et par POST /admin/candidatures/settings (AdminService) : un seul stockage.
 * Valeurs par défaut tant que l'école n'a rien enregistré : fermée, aucune date, aucun message, aucune pièce.
 */
@Service
public class ParametresCandidaturesService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final ParametresCandidaturesJpaRepository depot;

    public ParametresCandidaturesService(ParametresCandidaturesJpaRepository depot) {
        this.depot = depot;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lire() {
        return TenantContext.getCurrentOrganizationId()
                .flatMap(depot::findById)
                .map(ParametresCandidaturesService::versVue)
                .orElseGet(ParametresCandidaturesService::nonConfigure);
    }

    /** Met à jour les champs présents dans le corps (les autres sont conservés). */
    @Transactional
    public Map<String, Object> enregistrer(Map<String, Object> corps) {
        if (corps == null) throw mauvais("Corps de requête manquant.");
        ParametresCandidaturesJpaEntity p = chargerOuCreer();
        if (corps.containsKey("ouvert") || corps.containsKey("enabled")) {
            p.setOuvert(booleen(corps.containsKey("ouvert") ? corps.get("ouvert") : corps.get("enabled")));
        }
        if (corps.containsKey("dateOuverture")) p.setDateOuverture(date(corps.get("dateOuverture"), "d'ouverture"));
        if (corps.containsKey("dateFermeture")) p.setDateFermeture(date(corps.get("dateFermeture"), "de fermeture"));
        if (p.getDateOuverture() != null && p.getDateFermeture() != null
                && p.getDateFermeture().compareTo(p.getDateOuverture()) < 0) {
            throw mauvais("La date de fermeture doit être postérieure à la date d'ouverture.");
        }
        if (corps.containsKey("messageAvis")) p.setMessageAvis(texte(corps.get("messageAvis")));
        if (corps.containsKey("showRegisterButton")) p.setShowRegisterButton(booleen(corps.get("showRegisterButton")));
        if (corps.containsKey("docsRequis")) {
            Object docs = corps.get("docsRequis");
            if (docs != null && !(docs instanceof Map<?, ?>)) throw mauvais("docsRequis doit être un objet {cni: true, …}.");
            try {
                p.setDocsRequisJson(docs == null ? null : JSON.writeValueAsString(docs));
            } catch (Exception e) {
                throw mauvais("docsRequis illisible.");
            }
        }
        p.setUpdatedAt(LocalDateTime.now());
        return versVue(depot.save(p));
    }

    /** Ouverture / fermeture seule (POST /admin/candidatures/settings). */
    @Transactional
    public void definirOuverture(boolean ouvert) {
        ParametresCandidaturesJpaEntity p = chargerOuCreer();
        p.setOuvert(ouvert);
        p.setUpdatedAt(LocalDateTime.now());
        depot.save(p);
    }

    private ParametresCandidaturesJpaEntity chargerOuCreer() {
        String ecole = TenantContext.getCurrentOrganizationId()
                .orElseThrow(() -> mauvais("Aucune école sélectionnée : paramètres de candidature non enregistrés."));
        return depot.findById(ecole).orElseGet(() -> {
            ParametresCandidaturesJpaEntity n = new ParametresCandidaturesJpaEntity();
            n.setId(ecole);
            return n;
        });
    }

    static Map<String, Object> nonConfigure() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ouvert", false);
        m.put("dateOuverture", null);
        m.put("dateFermeture", null);
        m.put("messageAvis", null);
        m.put("showRegisterButton", false);
        m.put("docsRequis", Map.of());
        m.put("configure", false);
        return m;
    }

    private static Map<String, Object> versVue(ParametresCandidaturesJpaEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ouvert", p.isOuvert());
        m.put("dateOuverture", p.getDateOuverture());
        m.put("dateFermeture", p.getDateFermeture());
        m.put("messageAvis", p.getMessageAvis());
        m.put("showRegisterButton", p.isShowRegisterButton());
        Map<String, Object> docs = Map.of();
        if (p.getDocsRequisJson() != null) {
            try {
                docs = JSON.readValue(p.getDocsRequisJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                docs = Map.of();
            }
        }
        m.put("docsRequis", docs);
        m.put("configure", true);
        return m;
    }

    private static boolean booleen(Object v) {
        if (v instanceof Boolean b) return b;
        String s = v == null ? "" : v.toString().trim().toLowerCase();
        if (s.equals("true")) return true;
        if (s.equals("false") || s.isEmpty()) return false;
        throw mauvais("Valeur booléenne invalide : " + v);
    }

    private static String date(Object v, String libelle) {
        String s = texte(v);
        if (s == null) return null;
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s).toString();
        } catch (DateTimeParseException e) {
            throw mauvais("Date " + libelle + " invalide (format attendu AAAA-MM-JJ) : " + s);
        }
    }

    private static String texte(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static ResponseStatusException mauvais(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
