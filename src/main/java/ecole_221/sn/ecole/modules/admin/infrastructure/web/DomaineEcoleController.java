package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.platform.organization.application.DomainesEcoleService;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolutionHoteEcole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Domaine propre de l'école (décision du Tech Lead du 15/11 : POINTAGE + DÉCLARATION).
 *
 * <ul>
 *   <li>{@code GET /admin/domaines} : adresse de l'école, cible du CNAME et domaines déclarés ;</li>
 *   <li>{@code POST /admin/domaines} {domaine} : déclaration, pointage vérifié aussitôt (201) ;</li>
 *   <li>{@code POST /admin/domaines/{domaine}/verifier} : revérification (200) ;</li>
 *   <li>{@code DELETE /admin/domaines/{domaine}} : retrait, le domaine cesse d'être servi aussitôt (204) ;</li>
 *   <li>{@code GET /admin/domaine?domaine=} : vérification d'un pointage sans rien déclarer.</li>
 * </ul>
 * <p>Droits : module « ecole » (Établissement : identité visuelle, coordonnées) de la matrice du personnel — lecture
 * pour GET, écriture pour POST et DELETE. Un domaine d'une autre école : 404. Écritures inscrites au journal d'audit
 * (JournalAuditInterceptor).</p>
 */
@RestController
@Tag(name = "Administration", description = "Domaine propre de l'école")
public class DomaineEcoleController {

    private final ResolutionHoteEcole resolution;
    private final DomainesEcoleService domaines;
    /** N3 (revue phase 2) : vérifications sans déclaration freinées — {@value #VERIFICATIONS_MAX} par minute et par école. */
    static final int VERIFICATIONS_MAX = 20;
    private final java.util.Map<String, java.util.Deque<java.time.Instant>> verifications = new java.util.concurrent.ConcurrentHashMap<>();
    java.time.Clock horloge = java.time.Clock.systemUTC();

    public DomaineEcoleController(ResolutionHoteEcole resolution, DomainesEcoleService domaines) {
        this.resolution = resolution;
        this.domaines = domaines;
    }

    private static Organization ecoleCourante() {
        return TenantContext.getCurrentOrganization()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun établissement sélectionné."));
    }

    private static String compteCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof User u ? u.getId() : null;
    }

    @GetMapping("/admin/domaines")
    @Operation(summary = "Domaines propres de l'école", description = "{adresseEcole, cible, domaines: [{domaine, statut, pointeVersCetteEcole, verifieLe, message}]}")
    public ResponseEntity<Map<String, Object>> lister() {
        return ResponseEntity.ok(domaines.lister(ecoleCourante()));
    }

    @PostMapping("/admin/domaines")
    @Operation(summary = "Déclarer un domaine propre",
            description = "Corps {domaine}. 201 {domaine, statut, pointeVersCetteEcole, message} ; 400 adresse refusée ; "
                    + "409 déjà déclaré (sans dire par qui) ou quota de 3 domaines atteint.")
    public ResponseEntity<Map<String, Object>> declarer(@RequestBody(required = false) Map<String, Object> corps) {
        Object saisi = corps == null ? null : corps.get("domaine");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(domaines.declarer(ecoleCourante(), saisi instanceof String s ? s : null, compteCourant()));
    }

    @PostMapping("/admin/domaines/{domaine}/verifier")
    @Operation(summary = "Revérifier le pointage d'un domaine déclaré")
    public ResponseEntity<Map<String, Object>> verifier(@PathVariable("domaine") String domaine) {
        return ResponseEntity.ok(domaines.verifier(ecoleCourante(), domaine));
    }

    @DeleteMapping("/admin/domaines/{domaine}")
    @Operation(summary = "Retirer un domaine déclaré", description = "204 : le domaine cesse d'être servi aussitôt.")
    public ResponseEntity<Void> supprimer(@PathVariable("domaine") String domaine) {
        domaines.supprimer(ecoleCourante(), domaine);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/domaine")
    @Operation(summary = "Vérifier le pointage DNS d'un domaine (sans le déclarer)",
            description = "Paramètre domaine. {adresseEcole, cible, domaine, pointeVersCetteEcole, pointeVers, message} ; "
                    + "pointeVers = slug de CETTE école ou null. Lecture seule, 20 vérifications par minute et par école.")
    public ResponseEntity<Map<String, Object>> verifierSansDeclarer(@RequestParam(value = "domaine", required = false) String domaineSaisi) {
        Organization ecole = ecoleCourante();
        String slug = ecole.getSlug();
        String cible = resolution.cible(slug);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("adresseEcole", resolution.adresseEcole(slug));
        r.put("cible", cible);

        if (domaineSaisi == null || domaineSaisi.isBlank()) {
            r.put("domaine", null);
            r.put("pointeVersCetteEcole", false);
            r.put("pointeVers", null);
            r.put("message", "Chez votre registraire, créez un enregistrement CNAME de votre sous-domaine vers « " + cible
                    + " » (par exemple : portail.mon-ecole.sn CNAME " + cible + "), puis déclarez ce sous-domaine.");
            return ResponseEntity.ok(r);
        }

        if (!dansLaLimite(ecole.getId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de vérifications en une minute : réessayez dans un instant.");
        }
        String domaine = ResolutionHoteEcole.normaliser(DomainesEcoleService.sansSchema(domaineSaisi));
        if (domaine == null || !domaine.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Adresse invalide : saisissez un nom de domaine, par exemple portail.mon-ecole.sn.");
        }
        r.put("domaine", domaine);

        if (resolution.estHotePlateforme(domaine) || ResolutionHoteEcole.estAdresseIp(domaine)) {
            r.put("pointeVersCetteEcole", false);
            r.put("pointeVers", null);
            r.put("message", "« " + domaine + " » n'est pas un domaine d'école : saisissez le domaine de votre école "
                    + "(par exemple portail.mon-ecole.sn).");
            return ResponseEntity.ok(r);
        }

        Optional<String> trouve = resolution.slugDuPointage(domaine, false).map(s -> "ecole221".equals(s) ? "ecole-221" : s);
        boolean cetteEcole = trouve.isPresent() && trouve.get().equals(slug);
        r.put("pointeVersCetteEcole", cetteEcole);
        // N3 : l'identifiant d'une AUTRE école n'est jamais renvoyé (avant : « pointeVers » le révélait)
        r.put("pointeVers", cetteEcole ? slug : null);

        String message;
        if (resolution.estSousDomainePlateforme(domaine)) {
            message = cetteEcole
                    ? "« " + domaine + " » est l'adresse de votre école sur la plateforme : elle fonctionne sans aucun réglage."
                    : trouve.isPresent()
                        ? "« " + domaine + " » est l'adresse d'un autre établissement sur la plateforme."
                        : "« " + domaine + " » est un nom réservé par la plateforme.";
        } else if (cetteEcole) {
            message = "Le domaine « " + domaine + " » pointe bien vers votre établissement."
                    + (resolution.domaineAutorise(domaine, ecole) ? " Il est déclaré : il est servi en https://" + domaine + "."
                        : " Déclarez-le (« Ajouter ce domaine ») pour qu'il soit servi.");
        } else if (trouve.isPresent()) {
            message = "Le domaine « " + domaine + " » pointe vers un autre établissement. Chez votre registraire, "
                    + "remplacez la cible de l'enregistrement CNAME par « " + cible + " ».";
        } else {
            message = "Aucun enregistrement CNAME menant à « " + cible + " » n'a été trouvé pour « " + domaine + " ». Chez "
                    + "votre registraire, ajoutez : " + domaine + " CNAME " + cible + ". Un changement DNS peut mettre de "
                    + "quelques minutes à quelques heures à se propager." + DomainesEcoleService.conseilApex(domaine);
        }
        r.put("message", message);
        return ResponseEntity.ok(r);
    }

    private boolean dansLaLimite(String cle) {
        java.time.Instant t = horloge.instant();
        java.util.Deque<java.time.Instant> liste = verifications.computeIfAbsent(cle, k -> new java.util.ArrayDeque<>());
        synchronized (liste) {
            while (!liste.isEmpty() && liste.peekFirst().isBefore(t.minusSeconds(60))) {
                liste.pollFirst();
            }
            if (liste.size() >= VERIFICATIONS_MAX) {
                return false;
            }
            liste.addLast(t);
            return true;
        }
    }
}
