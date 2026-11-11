package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.parent.application.port.dto.ApplicationDto;
import ecole_221.sn.ecole.modules.parent.application.port.dto.EnrollChildPayloadDto;
import ecole_221.sn.ecole.modules.parent.domain.model.CandidatureParent;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.CandidaturesParentPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Dépôt d'un dossier de candidature par un parent : enregistré pour de vrai, jamais inventé. */
class ParentServiceCandidaturesTest {

    /** Faux port en mémoire : garde les dossiers déposés, par parent. */
    static class PortMemoire implements CandidaturesParentPort {
        final List<CandidatureParent> dossiers = new ArrayList<>();
        final List<String> emails = new ArrayList<>();
        final List<Map<String, String>> complements = new ArrayList<>();
        final List<String> parents = new ArrayList<>();
        String formation;

        @Override public boolean emailDejaCandidat(String email) { return emails.contains(email); }

        @Override public String deposer(String parentId, String prenom, String nom, String email, String telephone,
                                        String formation, Map<String, String> complement) {
            String id = "CAND-T-" + (dossiers.size() + 1);
            this.formation = formation;
            emails.add(email);
            complements.add(complement);
            parents.add(parentId);
            dossiers.add(new CandidatureParent(id, prenom + " " + nom, formation, complement.get("niveau"),
                    LocalDateTime.of(2026, 11, 11, 10, 0), "EN_ATTENTE", "new", null));
            return id;
        }

        @Override public List<CandidatureParent> listerParParent(String parentId) {
            List<CandidatureParent> r = new ArrayList<>();
            for (int i = 0; i < dossiers.size(); i++) if (parents.get(i).equals(parentId)) r.add(dossiers.get(i));
            return r;
        }
    }

    private final PortMemoire port = new PortMemoire();
    private final ParentService service = new ParentService(Mockito.mock(ParentRepositoryPort.class),
            Mockito.mock(AppointmentRepositoryPort.class), null, port);

    private static EnrollChildPayloadDto dossier(String prenom, String nom, String email, String filiere, String niveau) {
        return new EnrollChildPayloadDto(prenom, nom, "2010-05-02", "Dakar", "Sénégalaise", "F", email, "770000000",
                filiere, niveau, "BFEM", "CEM Liberté 6", "Liberté 6, Dakar", "2026", "Bien");
    }

    @Test
    void depotEnregistreLeDossierEtRenvoieSonIdentifiant() {
        String id = service.enrollChild("parent-1", dossier("Awa", "Diop", "Awa.Diop@Exemple.sn", "Informatique", "L1"));

        assertThat(id).isEqualTo("CAND-T-1");
        assertThat(port.emails).containsExactly("awa.diop@exemple.sn");
        assertThat(port.formation).isEqualTo("Informatique — L1");
        assertThat(port.complements.get(0)).containsEntry("niveau", "L1").containsEntry("adresse", "Liberté 6, Dakar")
                .containsEntry("mentionBac", "Bien").containsEntry("deposePar", "parent");
    }

    @Test
    void champsObligatoiresManquantsRefuses() {
        assertThatThrownBy(() -> service.enrollChild("p", dossier(" ", "Diop", "a@b.sn", "Info", "L1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("prénom");
        assertThatThrownBy(() -> service.enrollChild("p", dossier("Awa", "Diop", "a@b.sn", null, "L1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("filière");
        assertThatThrownBy(() -> service.enrollChild("p", dossier("Awa", "Diop", "pas-un-email", "Info", "L1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pas valide");
        assertThat(port.dossiers).isEmpty();
    }

    @Test
    void emailDejaCandidatRefuseAvecMessageDeConflit() {
        service.enrollChild("p", dossier("Awa", "Diop", "awa@exemple.sn", "Info", "L1"));
        assertThatThrownBy(() -> service.enrollChild("p", dossier("Awa", "Diop", "AWA@exemple.sn", "Info", "L1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email").hasMessageContaining("déjà");   // → 409 (gestionnaire global)
        assertThat(port.dossiers).hasSize(1);
    }

    @Test
    void chaqueParentNeVoitQueSesDossiers() {
        service.enrollChild("parent-1", dossier("Awa", "Diop", "awa@exemple.sn", "Info", "L1"));
        service.enrollChild("parent-2", dossier("Modou", "Fall", "modou@exemple.sn", "Gestion", "L2"));

        List<ApplicationDto> p1 = service.getParentApplications("parent-1");
        assertThat(p1).hasSize(1);
        ApplicationDto d = p1.get(0);
        assertThat(d.enfant()).isEqualTo("Awa Diop");
        assertThat(d.niveau()).isEqualTo("L1");
        assertThat(d.statut()).isEqualTo("EN_ATTENTE");
        assertThat(d.dateDepot()).isEqualTo("2026-11-11");
        assertThat(service.getParentApplications("parent-3")).isEmpty();
    }

    @Test
    void candidaturesFermeesAucunDepot() {
        PortMemoire ferme = new PortMemoire() {
            @Override public void exigerOuverture() {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Les candidatures ne sont pas ouvertes dans cette école.");
            }
        };
        ParentService s = new ParentService(Mockito.mock(ParentRepositoryPort.class), Mockito.mock(AppointmentRepositoryPort.class), null, ferme);
        assertThatThrownBy(() -> s.enrollChild("p", dossier("Awa", "Diop", "a@b.sn", "Info", "L1")))
                .hasMessageContaining("pas ouvertes");
        assertThat(ferme.dossiers).isEmpty();
    }

    @Test
    void sansPortAucunDossierInventeNiFauxDepot() {
        ParentService sansPort = new ParentService(Mockito.mock(ParentRepositoryPort.class),
                Mockito.mock(AppointmentRepositoryPort.class));
        assertThat(sansPort.getParentApplications("p")).isEmpty();
        assertThatThrownBy(() -> sansPort.enrollChild("p", dossier("Awa", "Diop", "a@b.sn", "Info", "L1")))
                .isInstanceOf(IllegalStateException.class);
    }
}
