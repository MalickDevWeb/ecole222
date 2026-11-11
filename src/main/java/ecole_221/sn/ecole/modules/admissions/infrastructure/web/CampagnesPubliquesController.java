package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Campagnes d'admission visibles par le public (portail candidat, sans compte).
 * Avant : le portail les lisait dans GET /admissions/db (toute la base, fermé par APP-TL-10) ; depuis cette
 * fermeture, aucune campagne ne s'affichait et plus personne ne pouvait postuler.
 * Seules les campagnes publiées (ni brouillon ni archivée) sont renvoyées, avec une liste blanche de champs.
 */
@RestController
@Tag(name = "Portail candidat", description = "Lecture publique des campagnes d'admission")
public class CampagnesPubliquesController {

    private static final Logger logger = LoggerFactory.getLogger(CampagnesPubliquesController.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    static final Set<String> CHAMPS = Set.of("id", "title", "code", "state", "deadline", "fees", "requirements",
            "imageUrl", "level", "duration", "intake", "requiredDocs");
    static final Set<String> NON_PUBLIEES = Set.of("Brouillon", "Archivée");

    private final ElementPortailAdmissionJpaRepository elements;

    public CampagnesPubliquesController(ElementPortailAdmissionJpaRepository elements) {
        this.elements = elements;
    }

    @GetMapping("/candidatures/campagnes")
    @Operation(summary = "Campagnes publiées", description = "Campagnes de l'école courante, hors brouillons et archives")
    public ResponseEntity<List<Map<String, Object>>> campagnes() {
        List<Map<String, Object>> res = new ArrayList<>();
        for (ElementPortailAdmissionJpaEntity e : elements.findByNatureOrderByPositionAsc(ElementPortailAdmissionJpaEntity.CAMPAGNE)) {
            try {
                Map<String, Object> c = JSON.readValue(e.getContenuJson(), new TypeReference<Map<String, Object>>() {});
                if (NON_PUBLIEES.contains(String.valueOf(c.get("state")))) continue;
                Map<String, Object> publique = new LinkedHashMap<>();
                for (String k : CHAMPS) if (c.containsKey(k)) publique.put(k, c.get(k));
                res.add(publique);
            } catch (Exception ex) {
                logger.warn("Campagne {} illisible, ignorée", e.getId());
            }
        }
        return ResponseEntity.ok(res);
    }
}
