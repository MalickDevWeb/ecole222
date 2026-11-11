package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.parent.domain.model.CandidatureParent;
import ecole_221.sn.ecole.modules.parent.domain.repository.CandidaturesParentPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les dossiers déposés par un parent sont de vraies candidatures (table {@code candidates}) : ils
 * apparaissent dans les admissions de l'école, qui les traitent comme les autres. L'école est fixée par
 * {@code @TenantId} (école courante de la requête).
 */
@Component
public class CandidaturesParentAdapter implements CandidaturesParentPort {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final CandidateJpaRepository candidats;

    public CandidaturesParentAdapter(CandidateJpaRepository candidats) {
        this.candidats = candidats;
    }

    @Override
    public boolean emailDejaCandidat(String email) {
        return candidats.existsByEmailIgnoreCase(email);
    }

    @Override
    public String deposer(String parentId, String prenom, String nom, String email, String telephone,
                          String formation, Map<String, String> complement) {
        CandidateJpaEntity c = new CandidateJpaEntity();
        c.setId("CAND-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8));
        c.setParentId(parentId);
        c.setFirstName(prenom);
        c.setLastName(nom);
        c.setFullName(prenom + " " + nom);
        c.setEmail(email);
        c.setPhone(telephone);
        c.setCourseChoice(formation);
        c.setCurrentStep("new");
        c.setAdmissionStatus("EN_ATTENTE");
        c.setDiplomaUploaded(false);
        c.setIdCardUploaded(false);
        c.setMotivationLetterUploaded(false);
        c.setRegistrationFeePaid(false);
        c.setSubmittedAt(LocalDateTime.now());
        try {
            c.setAdditionalInfo(JSON.writeValueAsString(complement));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Informations complémentaires illisibles.", e);
        }
        return candidats.save(c).getId();
    }

    @Override
    public List<CandidatureParent> listerParParent(String parentId) {
        return candidats.findByParentIdOrderBySubmittedAtDesc(parentId).stream()
                .map(c -> new CandidatureParent(c.getId(), c.getFullName(), c.getCourseChoice(), niveau(c),
                        c.getSubmittedAt(), c.getAdmissionStatus(), c.getCurrentStep(), c.getRejectionReason()))
                .toList();
    }

    private static String niveau(CandidateJpaEntity c) {
        try {
            Object n = c.getAdditionalInfo() == null ? null : JSON.readValue(c.getAdditionalInfo(), Map.class).get("niveau");
            return n == null ? null : n.toString();
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
