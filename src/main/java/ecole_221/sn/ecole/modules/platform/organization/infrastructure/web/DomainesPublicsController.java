package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolutionHoteEcole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domaine propre d'une école (décisions du Tech Lead du 15/11 : pointage DNS + déclaration) — routes publiques.
 *
 * <ul>
 *   <li>{@code GET /public/tenants/par-hote?host=} : école SERVIE par une adresse (sous-domaine d'une école active,
 *       ou domaine propre déclaré par une école active et pointé vers elle) ;</li>
 *   <li>{@code GET /public/domaines/autorisation?domain=} : point « ask » du TLS à la demande du relais HTTPS (Caddy) —
 *       200 seulement pour un nom EXACT servable, sinon 404. Sans ce contrôle, tout nom pointé vers le serveur ferait
 *       émettre un certificat (abus des quotas de l'autorité, hameçonnage). Freiné globalement, par nom et par IP,
 *       journalisé. À n'exposer qu'au relais (réseau interne) : voir docs/guides/deploiement.md et Caddyfile.exemple.</li>
 * </ul>
 */
@RestController
@Tag(name = "Public", description = "Adresse et domaine propre des écoles")
public class DomainesPublicsController {

    private static final Logger log = LoggerFactory.getLogger(DomainesPublicsController.class);
    static final String AUCUNE_ECOLE = "Aucun établissement n'est rattaché à cette adresse.";
    /** Autorisations de certificat : au plus 60 demandes par minute et par IP, 10 par nom, 300 au total. */
    static final int DEMANDES_MAX = 60;
    static final int DEMANDES_MAX_PAR_NOM = 10;
    static final int DEMANDES_MAX_GLOBAL = 300;
    static final Duration FENETRE = Duration.ofMinutes(1);
    private static final String GLOBAL = "*";

    private final ResolutionHoteEcole resolution;
    private final Map<String, Deque<Instant>> parIp = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> parNom = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> global = new ConcurrentHashMap<>();
    Clock horloge = Clock.systemUTC();

    public DomainesPublicsController(ResolutionHoteEcole resolution) {
        this.resolution = resolution;
    }

    @GetMapping("/public/tenants/par-hote")
    @Operation(summary = "École désignée par une adresse",
            description = "Public. 200 {slug, nom} si l'hôte sert une école ACTIVE (son sous-domaine, ou un domaine propre "
                    + "déclaré et pointé vers elle) ; 404 sinon.")
    public ResponseEntity<Map<String, Object>> parHote(@RequestParam(value = "host", required = false) String host) {
        if (host == null || host.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", "Indiquez l'adresse à vérifier (paramètre host)."));
        }
        return resolution.ecoleServieParHote(host)
                .<ResponseEntity<Map<String, Object>>>map(ecole -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("slug", ecole.getSlug());
                    r.put("nom", ecole.getName());
                    return ResponseEntity.ok(r);
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("success", false, "message", AUCUNE_ECOLE)));
    }

    @GetMapping("/public/domaines/autorisation")
    @Operation(summary = "Autorisation d'émettre un certificat (TLS à la demande)",
            description = "Appelé par le relais HTTPS (réseau interne). 200 si le nom EXACT est le sous-domaine d'une école "
                    + "active ou un domaine propre déclaré et pointé vers une école active ; 404 sinon ; 429 si freiné.")
    public ResponseEntity<Map<String, Object>> autorisation(@RequestParam(value = "domain", required = false) String domaine,
                                                            HttpServletRequest requete) {
        String ip = requete.getRemoteAddr();
        String nom = ResolutionHoteEcole.normaliser(domaine);
        // nom EXACT : pas de port, pas de point final, pas de majuscule ni de joker (Caddy envoie le nom SNI tel quel)
        boolean exact = nom != null && domaine != null && nom.equals(domaine.trim());
        if (!dansLaLimite(global, GLOBAL, DEMANDES_MAX_GLOBAL) || !dansLaLimite(parIp, ip == null ? "?" : ip, DEMANDES_MAX)
                || (exact && !dansLaLimite(parNom, nom, DEMANDES_MAX_PAR_NOM))) {
            log.warn("[CERTIFICAT] Demande freinée pour « {} » depuis {}", nom, ip);
            return ResponseEntity.status(429).body(Map.of("success", false,
                    "message", "Trop de demandes d'autorisation : réessayez dans une minute."));
        }
        Organization ecole = exact ? resolution.ecoleServieParHote(nom).orElse(null) : null;
        if (ecole == null) {
            log.info("[CERTIFICAT] Refusé pour « {} » (depuis {}) : aucun établissement servi à cette adresse exacte",
                    nom != null ? nom : "(nom invalide)", ip);
            return ResponseEntity.status(404).body(Map.of("success", false, "message", AUCUNE_ECOLE));
        }
        log.info("[CERTIFICAT] Autorisé pour « {} » (école « {} », depuis {})", nom, ecole.getSlug(), ip);
        return ResponseEntity.ok(Map.of("success", true, "slug", ecole.getSlug()));
    }

    /** Fenêtre glissante d'une minute (clé : IP de la connexion — jamais X-Forwarded-For —, nom demandé, ou global). */
    private boolean dansLaLimite(Map<String, Deque<Instant>> compteurs, String cle, int max) {
        Instant t = horloge.instant();
        if (compteurs.size() > 10_000) {
            compteurs.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || e.getValue().peekLast().isBefore(t.minus(FENETRE));
                }
            });
        }
        Deque<Instant> liste = compteurs.computeIfAbsent(cle, k -> new ArrayDeque<>());
        synchronized (liste) {
            while (!liste.isEmpty() && liste.peekFirst().isBefore(t.minus(FENETRE))) {
                liste.pollFirst();
            }
            if (liste.size() >= max) {
                return false;
            }
            liste.addLast(t);
            return true;
        }
    }
}
