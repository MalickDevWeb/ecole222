package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ObservationEleveJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ObservationEleveJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Journal d'observations d'un élève, tenu par l'administration (APP-BE-10).
 * L'élève doit appartenir à l'école courante (404 sinon) ; tout est limité à l'école par @TenantId.
 */
@Service
public class ObservationsElevesService {

    /** Catégories proposées par l'écran de suivi (libellés affichés tels quels). */
    public static final List<String> CATEGORIES = List.of("Comportement", "Notes", "Retards/Absences", "Frais Scolaires", "Général");
    public static final int TEXTE_MAX = 1000;

    private final ObservationEleveJpaRepository observations;
    private final ComptesEcoleService comptesEcole;
    private final UserJpaRepository comptes;
    private final Clock horloge;

    @org.springframework.beans.factory.annotation.Autowired
    public ObservationsElevesService(ObservationEleveJpaRepository observations, ComptesEcoleService comptesEcole, UserJpaRepository comptes) {
        this(observations, comptesEcole, comptes, Clock.systemDefaultZone());
    }

    ObservationsElevesService(ObservationEleveJpaRepository observations, ComptesEcoleService comptesEcole,
                              UserJpaRepository comptes, Clock horloge) {
        this.observations = observations;
        this.comptesEcole = comptesEcole;
        this.comptes = comptes;
        this.horloge = horloge;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> liste(String eleveId) {
        comptesEcole.membre(eleveId, "ETUDIANT");
        Map<String, String> noms = new HashMap<>();
        return observations.findByEleveIdOrderByCreeLeDesc(eleveId).stream().map(o -> vue(o, noms)).toList();
    }

    @Transactional
    public Map<String, Object> ajouter(String eleveId, String auteurId, String categorieBrute, String texteBrut) {
        comptesEcole.membre(eleveId, "ETUDIANT");
        String categorie = categorieBrute == null ? "" : categorieBrute.trim();
        String trouvee = CATEGORIES.stream().filter(c -> c.equalsIgnoreCase(categorie)).findFirst().orElse(null);
        if (trouvee == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (categorie.isEmpty() ? "Choisissez une catégorie"
                    : "Catégorie inconnue : « " + categorie + " »") + " (" + String.join(", ", CATEGORIES) + ").");
        }
        String texte = texteBrut == null ? "" : texteBrut.trim();
        if (texte.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'observation est vide : décrivez ce qui a été constaté.");
        }
        if (texte.length() > TEXTE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'observation fait " + texte.length() + " caractères ; " + TEXTE_MAX + " au plus.");
        }
        ObservationEleveJpaEntity o = new ObservationEleveJpaEntity();
        o.setId("obs-" + UUID.randomUUID());
        o.setEleveId(eleveId);
        o.setCategorie(trouvee);
        o.setTexte(texte);
        o.setAuteurId(auteurId);
        o.setCreeLe(LocalDateTime.now(horloge));
        return vue(observations.save(o), new HashMap<>());
    }

    private Map<String, Object> vue(ObservationEleveJpaEntity o, Map<String, String> noms) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", o.getId());
        v.put("eleveId", o.getEleveId());
        v.put("type", o.getCategorie());
        v.put("text", o.getTexte());
        v.put("auteur", noms.computeIfAbsent(o.getAuteurId(), k -> comptes.findById(k).map(ObservationsElevesService::nomComplet).orElse(null)));
        v.put("date", o.getCreeLe().toLocalDate().toString());
        v.put("creeLe", o.getCreeLe().toString());
        return v;
    }

    private static String nomComplet(UserJpaEntity u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? null : n;
    }
}
