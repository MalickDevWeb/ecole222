package ecole_221.sn.ecole.modules.admin.transferts;

import ecole_221.sn.ecole.modules.admin.application.service.ObservationsElevesService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.DocumentEleveJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ProfilEleveJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copie figée du dossier d'un élève, construite DANS L'ÉCOLE D'ORIGINE au moment où l'école d'accueil accepte le
 * transfert. L'appelant place le contexte sur l'école d'origine ; la transaction séparée garantit que les
 * lectures se font dans cette école (le filtre @TenantId est fixé à l'ouverture de la session).
 */
@Service
public class DossierTransfertService {

    private final UserJpaRepository comptes;
    private final StudentUseCase eleves;
    private final ProfilEleveJpaRepository profils;
    private final ObservationsElevesService observations;
    private final DocumentEleveJpaRepository documents;

    public DossierTransfertService(UserJpaRepository comptes, StudentUseCase eleves, ProfilEleveJpaRepository profils,
                                   ObservationsElevesService observations, DocumentEleveJpaRepository documents) {
        this.comptes = comptes;
        this.eleves = eleves;
        this.profils = profils;
        this.observations = observations;
        this.documents = documents;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Map<String, Object> construire(String eleveId, String nomEcoleSource) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("etabliLe", Instant.now().toString());
        d.put("ecoleOrigine", nomEcoleSource);
        comptes.findById(eleveId).ifPresent(u -> {
            Map<String, Object> id = new LinkedHashMap<>();
            id.put("nom", u.getNom());
            id.put("prenom", u.getPrenom());
            id.put("email", u.getEmail());
            id.put("matricule", u.getMatricule());
            d.put("identite", id);
        });
        List<Map<String, Object>> notes = new ArrayList<>();
        for (Map<String, Object> g : eleves.getStudentGrades(eleveId)) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("module", g.get("module"));
            n.put("enseignant", g.get("prof"));
            n.put("ects", g.get("ects"));
            n.put("cc", g.get("cc"));
            n.put("examen", g.get("examen"));
            notes.add(n);
        }
        d.put("notes", notes);
        profils.findFirstByUserId(eleveId).ifPresent(p -> {
            Map<String, Object> pr = new LinkedHashMap<>();
            pr.put("groupeSanguin", p.getGroupeSanguin());
            pr.put("allergies", p.getAllergies());
            d.put("profil", pr);
        });
        d.put("observations", observations.liste(eleveId));
        d.put("documents", documents.findByUserIdOrderByCreatedAtAsc(eleveId).stream().map(doc -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nom", doc.getName());
            m.put("taille", doc.getSize());
            m.put("deposeLe", doc.getCreatedAt() == null ? null : doc.getCreatedAt().toString());
            return m;
        }).toList());
        return d;
    }
}
