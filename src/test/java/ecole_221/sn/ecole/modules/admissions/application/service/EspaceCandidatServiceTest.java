package ecole_221.sn.ecole.modules.admissions.application.service;

import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaRepository;
import ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Espace du candidat — jeton de suivi, pièces, échanges avec l'école. */
class EspaceCandidatServiceTest {

    private final Map<String, ElementPortailAdmissionJpaEntity> stockage = new LinkedHashMap<>();
    private CandidateJpaRepository candidats;
    private EmailUseCase emails;
    private CandidateJpaEntity dossier;
    private Instant maintenant = Instant.parse("2026-11-17T10:00:00Z");

    private EspaceCandidatService service(String secret) {
        ElementPortailAdmissionJpaRepository elements = mock(ElementPortailAdmissionJpaRepository.class);
        when(elements.findByNatureOrderByPositionAsc(anyString())).thenAnswer(i -> stockage.values().stream()
                .filter(e -> e.getNature().equals(i.getArgument(0))).sorted(Comparator.comparingInt(ElementPortailAdmissionJpaEntity::getPosition)).toList());
        when(elements.save(any())).thenAnswer(i -> { ElementPortailAdmissionJpaEntity e = i.getArgument(0); stockage.put(e.getId(), e); return e; });
        Clock horloge = new Clock() {
            public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId z) { return Clock.fixed(maintenant, z); }
            public Instant instant() { return maintenant; }
        };
        EspaceCandidatService s = new EspaceCandidatService(candidats, elements, secret, "https://ecole.exemple", horloge);
        s.setEmails(emails);
        s.setEnvoiEmails(Runnable::run);   // envoi immédiat pour vérifier l'e-mail
        return s;
    }

    private static void ecole(String id) {
        Organization o = mock(Organization.class);
        when(o.getId()).thenReturn(id);
        when(o.getName()).thenReturn("Lycée de Thiès");
        when(o.getSlug()).thenReturn("thies");
        TenantContext.setCurrentOrganization(o);
    }

    @BeforeEach
    void preparer() {
        ecole("org-a");
        candidats = mock(CandidateJpaRepository.class);
        emails = mock(EmailUseCase.class);
        dossier = new CandidateJpaEntity();
        dossier.setId("CAND-1");
        dossier.setFirstName("Awa");
        dossier.setLastName("Diop");
        dossier.setEmail("awa@exemple.sn");
        dossier.setAdmissionStatus("EN_ATTENTE");
        dossier.setAdditionalInfo("{\"pieceIdentiteUrl\":\"https://x/cni.pdf\",\"pieceSuppLibelle.passeport\":\"Passeport\"}");
        when(candidats.findById("CAND-1")).thenReturn(Optional.of(dossier));
    }

    @AfterEach
    void nettoyer() {
        TenantContext.setCurrentOrganization(null);
    }

    @Test
    void jetonDeSuivi() {
        EspaceCandidatService s = service("secret-de-test");
        String jeton = s.emettreJeton("CAND-1");
        assertEquals("CAND-1", s.idDuJeton(jeton));
        // falsifié, absent, autre école, autre clé, expiré : 401
        assertThrows(ResponseStatusException.class, () -> s.idDuJeton(jeton.substring(0, jeton.length() - 2) + "xx"));
        assertThrows(ResponseStatusException.class, () -> s.idDuJeton(null));
        assertThrows(ResponseStatusException.class, () -> s.idDuJeton(Base64.getUrlEncoder().withoutPadding()
                .encodeToString("CAND-2|org-a|9999999999".getBytes()) + "." + jeton.split("\\.")[1]));
        assertThrows(ResponseStatusException.class, () -> service("autre-secret").idDuJeton(jeton));
        ecole("org-b");
        assertThrows(ResponseStatusException.class, () -> s.idDuJeton(jeton));
        ecole("org-a");
        maintenant = maintenant.plusSeconds(EspaceCandidatService.DUREE_JETON_SECONDES + 1);
        assertThrows(ResponseStatusException.class, () -> s.idDuJeton(jeton));
    }

    @Test
    void piecesDuDossierEtAjout() {
        EspaceCandidatService s = service("k");
        List<Map<String, Object>> pieces = s.pieces(dossier);
        assertEquals(List.of("pieceIdentite", "diplome", "releves", "photo", "passeport"), pieces.stream().map(p -> p.get("code")).toList());
        assertEquals(true, pieces.get(0).get("jointe"));
        assertEquals(false, pieces.get(4).get("jointe"));

        assertThrows(ResponseStatusException.class, () -> s.deposerPiece(dossier, "salaire", "https://x/a.pdf"));
        assertThrows(ResponseStatusException.class, () -> s.deposerPiece(dossier, "passeport", "javascript:alert(1)"));
        List<Map<String, Object>> apres = s.deposerPiece(dossier, "passeport", "https://x/passeport.pdf");
        assertEquals(true, apres.get(4).get("jointe"));
        assertEquals("https://x/passeport.pdf", s.lienPiece(dossier, "passeport"));
        s.deposerPiece(dossier, "diplome", "https://x/diplome.pdf");
        assertTrue(dossier.getDiplomaUploaded());
        // l'école est informée dans les échanges
        assertEquals(List.of("J'ai ajouté la pièce « Passeport ».", "J'ai ajouté la pièce « Diplôme ou attestation »."),
                s.messages("CAND-1").stream().map(m -> m.get("texte")).toList());

        dossier.setAdmissionStatus("ACCEPTED");
        var refus = assertThrows(ResponseStatusException.class, () -> s.deposerPiece(dossier, "photo", "https://x/p.png"));
        assertEquals(409, refus.getStatusCode().value());
    }

    @Test
    void echangesEtLecture() {
        EspaceCandidatService s = service("k");
        s.messageCandidat(dossier, "Bonjour, mon relevé arrive demain.");
        s.messageEcole("CAND-1", "Mme Sarr", "Merci, nous attendons votre relevé.");
        verify(emails).sendStudentNotification(eq("awa@exemple.sn"), contains("nouveau message"),
                argThat(t -> t.contains("Merci, nous attendons") && t.contains("https://ecole.exemple/t/thies/candidature")));
        assertEquals(Map.of("CAND-1", 1L), s.nonLusParEcole());
        assertEquals(1L, s.enrichir(new LinkedHashMap<>(Map.of("id", "CAND-1"))).get("messagesNonLus"));

        List<Map<String, Object>> vusParEcole = s.lireMessages("CAND-1", "ECOLE");
        assertEquals(2, vusParEcole.size());
        assertEquals(Map.of(), s.nonLusParEcole());                         // message du candidat lu par l'école
        assertEquals(1L, s.enrichir(new LinkedHashMap<>(Map.of("id", "CAND-1"))).get("messagesNonLus"));   // pas encore lu par le candidat
        s.lireMessages("CAND-1", "CANDIDAT");
        assertEquals(0L, s.enrichir(new LinkedHashMap<>(Map.of("id", "CAND-1"))).get("messagesNonLus"));

        assertThrows(ResponseStatusException.class, () -> s.messageCandidat(dossier, "   "));
        assertThrows(ResponseStatusException.class, () -> s.messageCandidat(dossier, "x".repeat(2001)));
        assertTrue(s.messages("CAND-AUTRE").isEmpty());
    }
}
