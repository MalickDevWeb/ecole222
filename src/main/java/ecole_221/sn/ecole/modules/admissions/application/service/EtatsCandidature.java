package ecole_221.sn.ecole.modules.admissions.application.service;

import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Cohérence entre la DÉCISION (admission_status) et l'ÉTAPE du Kanban (current_step) d'une candidature
 * (APP-BE-24).
 * <ul>
 *   <li>décision ACCEPTED / FINALISE → étape admitted ; REJECTED → rejected ; décision rouverte → étape new ;</li>
 *   <li>étape admitted → ACCEPTED ; rejected → REJECTED ; new → EN_ATTENTE ; docs / interview → EN_COURS
 *       (une décision déjà prise est alors retirée).</li>
 * </ul>
 */
public final class EtatsCandidature {

    public static final List<String> ETAPES = List.of("new", "docs", "interview", "admitted", "rejected");

    private EtatsCandidature() {}

    /** Étape valide, normalisée ; 400 sinon. */
    public static String etape(String brute) {
        String e = brute == null ? "" : brute.trim().toLowerCase(Locale.ROOT);
        if (!ETAPES.contains(e)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Étape inconnue : « " + brute + " » (attendu : " + String.join(", ", ETAPES) + ").");
        }
        return e;
    }

    public static final String DOSSIER_INSCRIT = "Dossier inscrit : l'élève a été créé, le dossier ne peut plus changer d'état ni être supprimé.";
    public static final String FINALISE_MANUEL = "Le statut « Finalisé » est posé uniquement par l'inscription de l'admis (bouton « Inscrire »).";

    /** 409 si le dossier est déjà inscrit (APP-TL-23) : son état et son existence sont figés. */
    public static void exigerModifiable(CandidateJpaEntity c) {
        if (c != null && c.estInscrit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DOSSIER_INSCRIT);
        }
    }

    /** 409 si une décision manuelle tente de poser FINALISE : seule l'inscription le fait (APP-TL-23). */
    public static void refuserFinaliseManuel(CandidateStatus statut) {
        if (statut == CandidateStatus.FINALISE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, FINALISE_MANUEL);
        }
    }

    /** À appeler après avoir changé la décision : l'étape suit. */
    public static void apresDecision(CandidateJpaEntity c) {
        CandidateStatus s = statut(c.getAdmissionStatus());
        String etape = c.getCurrentStep();
        switch (s) {
            case ACCEPTED, FINALISE -> c.setCurrentStep("admitted");
            case REJECTED -> c.setCurrentStep("rejected");
            case EN_ATTENTE, EN_COURS -> {
                if (etape == null || "admitted".equals(etape) || "rejected".equals(etape)) c.setCurrentStep("new");
            }
        }
    }

    /** À appeler après avoir changé l'étape : la décision suit. */
    public static void apresEtape(CandidateJpaEntity c) {
        CandidateStatus s = statut(c.getAdmissionStatus());
        switch (c.getCurrentStep() == null ? "new" : c.getCurrentStep()) {
            case "admitted" -> { if (s != CandidateStatus.FINALISE) c.setAdmissionStatus(CandidateStatus.ACCEPTED.name()); }
            case "rejected" -> c.setAdmissionStatus(CandidateStatus.REJECTED.name());
            case "new" -> c.setAdmissionStatus(CandidateStatus.EN_ATTENTE.name());
            default -> c.setAdmissionStatus(CandidateStatus.EN_COURS.name());   // docs, interview : examen en cours
        }
    }

    private static CandidateStatus statut(String brut) {
        try {
            return brut == null || brut.isBlank() ? CandidateStatus.EN_ATTENTE : CandidateStatus.from(brut);
        } catch (IllegalArgumentException e) {
            return CandidateStatus.EN_ATTENTE;
        }
    }
}
