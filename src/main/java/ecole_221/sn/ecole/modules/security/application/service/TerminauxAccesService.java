package ecole_221.sn.ecole.modules.security.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.TerminalAccesJpaEntity;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.TerminalAccesJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static ecole_221.sn.ecole.partage.ChampsRequete.refus;
import static ecole_221.sn.ecole.partage.ChampsRequete.texte;
import static ecole_221.sn.ecole.partage.ChampsRequete.texteOptionnel;

/**
 * Terminaux d'accès de l'école (APP-BE-21, V9).
 *
 * <p>Avant : aucune source ; « Activer » et « Révoquer » ne changeaient que l'affichage. Désormais : terminaux
 * enregistrés par école, un vigile affecté par terminal (et un terminal par vigile), statut Actif / Inactif, révocation
 * définitive. Un scan fait par un vigile dont le terminal est inactif ou révoqué est refusé
 * ({@link #exigerTerminalUtilisable(String)}).</p>
 */
@Service
public class TerminauxAccesService {

    public static final String ACTIF = "Actif";
    public static final String INACTIF = "Inactif";
    public static final String REVOQUE = "Révoqué";

    private final TerminalAccesJpaRepository terminaux;
    private final ComptesEcoleService comptes;

    public TerminauxAccesService(TerminalAccesJpaRepository terminaux, ComptesEcoleService comptes) {
        this.terminaux = terminaux;
        this.comptes = comptes;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lister() {
        return terminaux.findAllByOrderByCreeLeAsc().stream().map(this::vue).toList();
    }

    @Transactional
    public Map<String, Object> creer(Map<String, Object> corps) {
        String nom = texte(corps, "name", "Le nom du terminal", 100);
        String emplacement = texteOptionnel(corps, "location", "L'emplacement", 200);
        if (terminaux.existsByNomIgnoreCase(nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un terminal porte déjà le nom « " + nom + " ».");
        }
        TerminalAccesJpaEntity t = new TerminalAccesJpaEntity();
        t.setId("term-" + UUID.randomUUID().toString().substring(0, 12));
        t.setNom(nom);
        t.setEmplacement(emplacement);
        t.setStatut(ACTIF);
        t.setCreeLe(LocalDateTime.now());
        t.setModifieLe(t.getCreeLe());
        return vue(terminaux.save(t));
    }

    /** Actif ↔ Inactif. Un terminal révoqué ne peut plus être réactivé. */
    @Transactional
    public Map<String, Object> changerStatut(String id, Map<String, Object> corps) {
        TerminalAccesJpaEntity t = trouver(id);
        String statut = texte(corps, "status", "Le statut", 20);
        if (!ACTIF.equals(statut) && !INACTIF.equals(statut)) {
            throw refus("Le statut doit être « Actif » ou « Inactif » (la révocation a sa propre action).");
        }
        exigerNonRevoque(t);
        t.setStatut(statut);
        t.setModifieLe(LocalDateTime.now());
        return vue(terminaux.save(t));
    }

    /** Révocation définitive : le vigile affecté ne peut plus scanner depuis ce terminal. */
    @Transactional
    public Map<String, Object> revoquer(String id) {
        TerminalAccesJpaEntity t = trouver(id);
        exigerNonRevoque(t);
        t.setStatut(REVOQUE);
        t.setRevoqueLe(LocalDateTime.now());
        t.setModifieLe(t.getRevoqueLe());
        return vue(terminaux.save(t));
    }

    /** Affecte un vigile de l'école (ou retire l'affectation si vigilId est vide). Un vigile n'a qu'un terminal. */
    @Transactional
    public Map<String, Object> affecterVigile(String id, Map<String, Object> corps) {
        TerminalAccesJpaEntity t = trouver(id);
        exigerNonRevoque(t);
        String vigile = texteOptionnel(corps, "vigilId", "Le vigile", 255);
        if (vigile != null) {
            comptes.membre(vigile, "VIGIL");   // 404 si ce n'est pas un vigile de cette école
            for (TerminalAccesJpaEntity autre : terminaux.findByVigileId(vigile)) {
                if (!autre.getId().equals(t.getId()) && !REVOQUE.equals(autre.getStatut())) {
                    autre.setVigileId(null);
                    autre.setModifieLe(LocalDateTime.now());
                    terminaux.save(autre);
                }
            }
        }
        t.setVigileId(vigile);
        t.setModifieLe(LocalDateTime.now());
        return vue(terminaux.save(t));
    }

    /**
     * Terminal du vigile qui scanne. Vide si aucun terminal ne lui est affecté (scan accepté, sans point de passage).
     * Refus 403 si son terminal est inactif ou révoqué.
     */
    @Transactional(readOnly = true)
    public Optional<String> exigerTerminalUtilisable(String vigileId) {
        if (vigileId == null) {
            return Optional.empty();
        }
        List<TerminalAccesJpaEntity> siens = terminaux.findByVigileId(vigileId);
        Optional<TerminalAccesJpaEntity> actif = siens.stream().filter(x -> ACTIF.equals(x.getStatut())).findFirst();
        if (actif.isPresent()) {
            return Optional.of(actif.get().getNom());
        }
        Optional<TerminalAccesJpaEntity> bloque = siens.stream().findFirst();
        if (bloque.isPresent()) {
            TerminalAccesJpaEntity b = bloque.get();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Terminal « " + b.getNom() + " » "
                    + (REVOQUE.equals(b.getStatut()) ? "révoqué" : "désactivé") + " par l'administration : scan refusé.");
        }
        return Optional.empty();
    }

    private TerminalAccesJpaEntity trouver(String id) {
        return terminaux.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Terminal introuvable."));
    }

    private static void exigerNonRevoque(TerminalAccesJpaEntity t) {
        if (REVOQUE.equals(t.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Terminal « " + t.getNom() + " » révoqué : il ne peut plus être modifié.");
        }
    }

    private Map<String, Object> vue(TerminalAccesJpaEntity t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getNom());
        m.put("location", t.getEmplacement() == null ? "" : t.getEmplacement());
        m.put("status", t.getStatut());
        m.put("vigilId", t.getVigileId());
        String nomVigile = null;
        if (t.getVigileId() != null) {
            try {
                UserJpaEntity u = comptes.membre(t.getVigileId(), "VIGIL");
                nomVigile = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
            } catch (ResponseStatusException e) {
                nomVigile = null;   // vigile retiré de l'école depuis : pas de nom inventé
            }
        }
        m.put("vigilName", nomVigile);
        return m;
    }
}
