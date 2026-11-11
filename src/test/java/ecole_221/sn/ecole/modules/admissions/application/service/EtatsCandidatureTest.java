package ecole_221.sn.ecole.modules.admissions.application.service;

import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** APP-BE-24 : la décision et l'étape du Kanban ne se contredisent plus. */
class EtatsCandidatureTest {

    private static CandidateJpaEntity dossier(String statut, String etape) {
        CandidateJpaEntity c = new CandidateJpaEntity("CAND-1", "Awa", "Diop", "awa@exemple.sn", null, "Informatique");
        c.setAdmissionStatus(statut);
        c.setCurrentStep(etape);
        return c;
    }

    @Test
    void laDecisionEntraineLEtape() {
        CandidateJpaEntity c = dossier("ACCEPTED", "new");
        EtatsCandidature.apresDecision(c);
        assertThat(c.getCurrentStep()).isEqualTo("admitted");
        c.setAdmissionStatus("REJECTED"); EtatsCandidature.apresDecision(c);
        assertThat(c.getCurrentStep()).isEqualTo("rejected");
        c.setAdmissionStatus("EN_COURS"); EtatsCandidature.apresDecision(c);   // décision rouverte
        assertThat(c.getCurrentStep()).isEqualTo("new");
        CandidateJpaEntity enExamen = dossier("EN_ATTENTE", "interview");
        EtatsCandidature.apresDecision(enExamen);
        assertThat(enExamen.getCurrentStep()).isEqualTo("interview");          // pas de recul inutile
    }

    @Test
    void lEtapeEntraineLaDecision() {
        CandidateJpaEntity c = dossier("EN_ATTENTE", "admitted");
        EtatsCandidature.apresEtape(c);
        assertThat(c.getAdmissionStatus()).isEqualTo("ACCEPTED");
        c.setCurrentStep("interview"); EtatsCandidature.apresEtape(c);         // admis remis en examen : décision retirée
        assertThat(c.getAdmissionStatus()).isEqualTo("EN_COURS");
        c.setCurrentStep("rejected"); EtatsCandidature.apresEtape(c);
        assertThat(c.getAdmissionStatus()).isEqualTo("REJECTED");
        c.setCurrentStep("new"); EtatsCandidature.apresEtape(c);
        assertThat(c.getAdmissionStatus()).isEqualTo("EN_ATTENTE");
        CandidateJpaEntity finalise = dossier("FINALISE", "admitted");
        EtatsCandidature.apresEtape(finalise);
        assertThat(finalise.getAdmissionStatus()).isEqualTo("FINALISE");       // une inscription finalisée le reste
    }

    @Test
    void etapeInconnueRefusee() {
        assertThat(EtatsCandidature.etape(" Docs ")).isEqualTo("docs");
        assertThatThrownBy(() -> EtatsCandidature.etape("ACCEPTED")).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Étape inconnue : « ACCEPTED »");
    }
}
