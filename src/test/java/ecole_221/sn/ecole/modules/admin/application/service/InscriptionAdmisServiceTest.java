package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.admin.rbac.PermissionsEcoleService;
import ecole_221.sn.ecole.modules.admissions.application.service.EtatsCandidature;
import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.finance.application.service.EcolagesService;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** APP-TL-23 : inscription d'un candidat admis en élève (une transaction, un seul compte, dossier figé). */
class InscriptionAdmisServiceTest {

    private CandidateJpaRepository candidats;
    private ComptesEcoleService comptes;
    private UserJpaRepository utilisateurs;
    private ParentsEleveService parents;
    private ParentEnfantJpaRepository liens;
    private ClasseJpaRepository classes;
    private EcolagesService ecolages;
    private PermissionsEcoleService droits;
    private InscriptionAdmisService service;
    private CandidateJpaEntity dossier;

    @BeforeEach
    void preparer() {
        candidats = mock(CandidateJpaRepository.class);
        comptes = mock(ComptesEcoleService.class);
        utilisateurs = mock(UserJpaRepository.class);
        parents = mock(ParentsEleveService.class);
        liens = mock(ParentEnfantJpaRepository.class);
        classes = mock(ClasseJpaRepository.class);
        ecolages = mock(EcolagesService.class);
        droits = mock(PermissionsEcoleService.class);
        service = new InscriptionAdmisService(candidats, comptes, utilisateurs, parents, liens, classes,
                mock(InscriptionClasseJpaRepository.class), ecolages, droits);

        dossier = new CandidateJpaEntity("CAND-2026-0001", "Awa", "Diop", "awa.diop@exemple.sn", "771234567", "Génie Logiciel — L1");
        dossier.setAdmissionStatus(CandidateStatus.ACCEPTED.name());
        dossier.setCurrentStep("admitted");
        dossier.setRegistrationFeePaid(true);
        when(candidats.findPourInscription("CAND-2026-0001")).thenReturn(Optional.of(dossier));
        when(utilisateurs.findByEmail(anyString())).thenReturn(Optional.empty());
        when(comptes.ecoleCourante()).thenReturn("ecole-a");
        when(comptes.nouveauMatriculeEleve()).thenReturn("ETU-2026-ABC234");
        when(comptes.creer(anyString(), anyString(), anyString(), isNull(), anyString(), eq("ETUDIANT"), isNull()))
                .thenAnswer(i -> {
                    UserJpaEntity u = new UserJpaEntity();
                    u.setId("usr-eleve");
                    u.setPrenom(i.getArgument(0));
                    u.setNom(i.getArgument(1));
                    u.setEmail(i.getArgument(2));
                    u.setMatricule(i.getArgument(4));
                    u.setRole("ETUDIANT");
                    return new ComptesEcoleService.CompteCree(u, "Provisoire-Une-Fois");
                });
        connecter("ADMIN");
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "agent", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static void refus(Runnable action, HttpStatus statut, String extrait) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class, e -> {
            assertThat(e.getStatusCode()).isEqualTo(statut);
            assertThat(e.getReason()).contains(extrait);
        });
    }

    @Test
    void inscriptionCreeLeCompteEleveEtFigeLeDossier() {
        Map<String, Object> res = service.inscrire("CAND-2026-0001", Map.of());

        // e-mail du dossier comme identifiant ; téléphone du dossier NON copié ; mot de passe généré
        verify(comptes).creer("Awa", "Diop", "awa.diop@exemple.sn", null, "ETU-2026-ABC234", "ETUDIANT", null);
        assertThat(dossier.getEleveId()).isEqualTo("usr-eleve");
        assertThat(dossier.getAdmissionStatus()).isEqualTo("FINALISE");
        assertThat(dossier.getCurrentStep()).isEqualTo("admitted");
        assertThat(dossier.getInscritLe()).isNotNull();
        verify(candidats).save(dossier);
        assertThat(res.get("motDePasseProvisoire")).isEqualTo("Provisoire-Une-Fois");
        @SuppressWarnings("unchecked") Map<String, Object> eleve = (Map<String, Object>) res.get("eleve");
        assertThat(eleve.get("matricule")).isEqualTo("ETU-2026-ABC234");
        assertThat(eleve.get("sansEmail")).isEqualTo(false);
        assertThat(eleve.get("identifiant")).isEqualTo("awa.diop@exemple.sn");
        assertThat(res.get("parent")).isNull();
        assertThat(res.get("dossierFinancier")).isNull();
    }

    @Test
    void dossierNonAdmisRefuse() {
        dossier.setAdmissionStatus(CandidateStatus.EN_COURS.name());
        refus(() -> service.inscrire("CAND-2026-0001", Map.of()), HttpStatus.CONFLICT, "Seul un dossier admis");
        verify(comptes, never()).creer(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void dossierDejaInscritJamaisUnSecondCompte() {
        dossier.setEleveId("usr-existant");
        UserJpaEntity existant = new UserJpaEntity();
        existant.setMatricule("ETU-2026-XYZ789");
        when(utilisateurs.findById("usr-existant")).thenReturn(Optional.of(existant));
        refus(() -> service.inscrire("CAND-2026-0001", Map.of()), HttpStatus.CONFLICT, "Dossier déjà inscrit : élève ETU-2026-XYZ789");
        verify(comptes, never()).creer(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void candidatureInconnue404() {
        when(candidats.findPourInscription("CAND-INCONNUE")).thenReturn(Optional.empty());
        refus(() -> service.inscrire("CAND-INCONNUE", Map.of()), HttpStatus.NOT_FOUND, "Candidature introuvable");
    }

    @Test
    void fraisDeDossierNonConfirmesRefuses() {
        dossier.setRegistrationFeePaid(false);
        refus(() -> service.inscrire("CAND-2026-0001", Map.of()), HttpStatus.CONFLICT, "Frais de dossier non confirmés");
        verify(comptes, never()).creer(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void fraisDeDossierConfirmesAuGuichetDansLaMemeDemande() {
        dossier.setRegistrationFeePaid(false);
        service.inscrire("CAND-2026-0001", Map.of("fraisDossier", Map.of("moyen", "Wave", "reference", "WV-8842")));
        assertThat(dossier.getRegistrationFeePaid()).isTrue();
        assertThat(dossier.getRegistrationFeeReference()).isEqualTo("WV-8842");
        assertThat(dossier.getAdditionalInfo()).contains("\"paymentMethod\":\"Wave\"");
    }

    @Test
    void moyenDePaiementInconnuRefuse400() {
        dossier.setRegistrationFeePaid(false);
        refus(() -> service.inscrire("CAND-2026-0001", Map.of("fraisDossier", Map.of("moyen", "Carte cadeau"))),
                HttpStatus.BAD_REQUEST, "Le moyen de paiement des frais de dossier doit être l'une des valeurs");
    }

    @Test
    void eleveSansEmailConnexionParMatricule() {
        Map<String, Object> res = service.inscrire("CAND-2026-0001", Map.of("sansEmail", true));
        verify(comptes).creer("Awa", "Diop", "etu-2026-abc234@sans-email.invalid", null, "ETU-2026-ABC234", "ETUDIANT", null);
        @SuppressWarnings("unchecked") Map<String, Object> eleve = (Map<String, Object>) res.get("eleve");
        assertThat(eleve.get("sansEmail")).isEqualTo(true);
        assertThat(eleve.get("email")).isNull();
        assertThat(eleve.get("identifiant")).isEqualTo("ETU-2026-ABC234");
    }

    @Test
    void dossierDeposeAvecLAdresseDuParentEleveSansEmailEtParentLie() {
        UserJpaEntity parent = new UserJpaEntity();
        parent.setId("usr-parent");
        parent.setEmail("awa.diop@exemple.sn");   // le parent a saisi sa propre adresse
        parent.setRole("PARENT");
        dossier.setParentId("usr-parent");
        when(utilisateurs.findById("usr-parent")).thenReturn(Optional.of(parent));
        when(utilisateurs.findByEmail("awa.diop@exemple.sn")).thenReturn(Optional.of(parent));
        when(liens.findByParentIdAndEnfantId("usr-parent", "usr-eleve")).thenReturn(Optional.empty());
        when(parents.lier(eq("usr-eleve"), anyMap())).thenReturn(new HashMap<>(Map.of("relation", "AUTRE")));

        service.inscrire("CAND-2026-0001", Map.of());

        verify(comptes).creer("Awa", "Diop", "etu-2026-abc234@sans-email.invalid", null, "ETU-2026-ABC234", "ETUDIANT", null);
        @SuppressWarnings({"unchecked", "rawtypes"}) ArgumentCaptor<Map<String, Object>> demande = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
        verify(parents).lier(eq("usr-eleve"), demande.capture());
        assertThat(demande.getValue()).containsEntry("email", "awa.diop@exemple.sn").containsEntry("relation", "AUTRE");
    }

    @Test
    void adresseDejaPriseParUnAutreCompteRefusee() {
        UserJpaEntity autre = new UserJpaEntity();
        autre.setId("usr-autre");
        when(utilisateurs.findByEmail("awa.diop@exemple.sn")).thenReturn(Optional.of(autre));
        refus(() -> service.inscrire("CAND-2026-0001", Map.of()), HttpStatus.CONFLICT, "déjà utilisée par un autre compte");
    }

    @Test
    void ineDejaAttribueRefuse() {
        when(utilisateurs.existsByIneIgnoreCase("SN-12345")).thenReturn(true);
        refus(() -> service.inscrire("CAND-2026-0001", Map.of("ine", "SN-12345")), HttpStatus.CONFLICT, "INE est déjà attribué");
    }

    @Test
    void classeEtDossierFinancierAvecLesFraisAnnuelsDeLaClasse() {
        ClasseJpaEntity classe = new ClasseJpaEntity();
        classe.setId("cl-l1");
        classe.setName("L1 Génie Logiciel");
        classe.setFraisAnnuels(450_000L);
        when(classes.findById("cl-l1")).thenReturn(Optional.of(classe));
        when(ecolages.ouvrirDossier(anyMap())).thenReturn(Map.of("id", "fin-1"));

        Map<String, Object> res = service.inscrire("CAND-2026-0001",
                Map.of("classeId", "cl-l1", "dossierFinancier", Map.of()));

        @SuppressWarnings({"unchecked", "rawtypes"}) ArgumentCaptor<Map<String, Object>> demande = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
        verify(ecolages).ouvrirDossier(demande.capture());
        assertThat(demande.getValue()).containsEntry("eleveId", "usr-eleve").containsEntry("montantDu", 450_000L)
                .containsEntry("classe", "L1 Génie Logiciel");
        assertThat(res.get("dossierFinancier")).isEqualTo(Map.of("id", "fin-1"));
    }

    @Test
    void classeInconnueRefusee400() {
        when(classes.findById("cl-autre-ecole")).thenReturn(Optional.empty());
        refus(() -> service.inscrire("CAND-2026-0001", Map.of("classeId", "cl-autre-ecole")), HttpStatus.BAD_REQUEST, "Classe inconnue");
    }

    @Test
    void secretaireSansDroitElevesRefusee403() {
        connecter("SECRETAIRE");
        when(droits.autorise("ecole-a", "SECRETAIRE", "admissions.ecrire")).thenReturn(true);
        when(droits.autorise("ecole-a", "SECRETAIRE", "eleves.ecrire")).thenReturn(false);
        refus(() -> service.inscrire("CAND-2026-0001", Map.of()), HttpStatus.FORBIDDEN, "« Admissions » et « Élèves »");
        verify(candidats, never()).findPourInscription(anyString());
    }

    @Test
    void dossierFinancierDemandeAussiLeDroitFinances() {
        connecter("SECRETAIRE");
        when(droits.autorise("ecole-a", "SECRETAIRE", "admissions.ecrire")).thenReturn(true);
        when(droits.autorise("ecole-a", "SECRETAIRE", "eleves.ecrire")).thenReturn(true);
        when(droits.autorise("ecole-a", "SECRETAIRE", "finances.ecrire")).thenReturn(false);
        refus(() -> service.inscrire("CAND-2026-0001", Map.of("dossierFinancier", Map.of("montantDu", 100_000))),
                HttpStatus.FORBIDDEN, "« Finances »");
    }

    @Test
    void dossierInscritFigeEtFinaliseManuelInterdit() {
        CandidateJpaEntity inscrit = new CandidateJpaEntity();
        inscrit.setEleveId("usr-eleve");
        refus(() -> EtatsCandidature.exigerModifiable(inscrit), HttpStatus.CONFLICT, "Dossier inscrit");
        EtatsCandidature.exigerModifiable(new CandidateJpaEntity());   // dossier non inscrit : aucune erreur
        refus(() -> EtatsCandidature.refuserFinaliseManuel(CandidateStatus.FINALISE), HttpStatus.CONFLICT, "bouton « Inscrire »");
        EtatsCandidature.refuserFinaliseManuel(CandidateStatus.ACCEPTED);
    }

    @Test
    void identifiantTechniqueReconnu() {
        assertThat(InscriptionAdmisService.sansEmail("etu-2026-abc234@sans-email.invalid")).isTrue();
        assertThat(InscriptionAdmisService.sansEmail("awa.diop@exemple.sn")).isFalse();
        assertThat(InscriptionAdmisService.sansEmail(null)).isFalse();
    }
}
