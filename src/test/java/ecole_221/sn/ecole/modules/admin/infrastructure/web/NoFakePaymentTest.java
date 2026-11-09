package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.admissions.application.port.usecase.AdmissionUseCase;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.admissions.infrastructure.web.AdmissionController;
import ecole_221.sn.ecole.modules.ai.application.port.usecase.AIUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** APP-BE-04 : le backend ne renvoie jamais de fausse transaction ni de faux reçu. */
class NoFakePaymentTest {

    private static final List<String> FAUSSES_VALEURS = List.of("WAVE-SN-7382", "Orange Money", "REC-2023-001");
    private static final ObjectMapper JSON = new ObjectMapper();

    private CandidateJpaRepository repo;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(CandidateJpaRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdmissionController(Mockito.mock(AdmissionUseCase.class), repo,
                        Mockito.mock(ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaRepository.class)),
                new CandidaturesController(repo, Mockito.mock(ecole_221.sn.ecole.modules.admin.application.service.ParametresCandidaturesService.class)),
                new AdminStudentProfileController(
                        Mockito.mock(ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService.class),
                        Mockito.mock(ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ProfilEleveJpaRepository.class),
                        Mockito.mock(ecole_221.sn.ecole.modules.admin.infrastructure.persistence.DocumentEleveJpaRepository.class),
                        Mockito.mock(ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository.class),
                        Mockito.mock(ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository.class))).build();
    }

    private static void aucuneFausseValeur(String body) {
        for (String faux : FAUSSES_VALEURS) {
            assertFalse(body.contains(faux), "La réponse ne doit pas contenir « " + faux + " » : " + body);
        }
    }

    @Test
    void admissionsDbSansPaiementConnuRenvoieNullEtPasDeDonneesInventees() throws Exception {
        CandidateJpaEntity e = new CandidateJpaEntity("c-1", "Awa", "Diop", "awa@test.sn", "770000000", "Licence");
        e.setRegistrationFeePaid(true);
        Mockito.when(repo.findAll()).thenReturn(List.of(e));

        String body = mockMvc.perform(get("/admissions/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].details.paymentMethod").value(nullValue()))
                .andExpect(jsonPath("$.candidates[0].details.paymentTransactionRef").value(nullValue()))
                .andExpect(jsonPath("$.candidates[0].details.cniUrl").value(nullValue()))
                .andExpect(jsonPath("$.candidates[0].details.cniStatus").value("unknown"))
                .andExpect(jsonPath("$.candidates[0].docs.transcripts").value(false))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        aucuneFausseValeur(body);
        assertFalse(body.contains("cloudinary"), body);
        assertFalse(body.contains("verified"), body);
    }

    @Test
    void admissionsDbRenvoieLesVraiesDonneesDePaiement() throws Exception {
        CandidateJpaEntity e = new CandidateJpaEntity("c-2", "Moussa", "Ba", "moussa@test.sn", "770000001", "Master");
        e.setRegistrationFeePaid(true);
        e.setRegistrationFeeReference("FM-REF-123");
        e.setAdditionalInfo("{\"paymentMethod\":\"FREE_MONEY\"}");
        Mockito.when(repo.findAll()).thenReturn(List.of(e));

        String body = mockMvc.perform(get("/admissions/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].details.paymentMethod").value("FREE_MONEY"))
                .andExpect(jsonPath("$.candidates[0].details.paymentTransactionRef").value("FM-REF-123"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        aucuneFausseValeur(body);
    }

    @Test
    void admissionsDbAdditionalInfoIllisibleDonneNull() throws Exception {
        CandidateJpaEntity e = new CandidateJpaEntity("c-3", "Fatou", "Sall", "fatou@test.sn", "770000002", "Licence");
        e.setAdditionalInfo("pas du json");
        e.setRegistrationFeeReference("   ");
        Mockito.when(repo.findAll()).thenReturn(List.of(e));

        mockMvc.perform(get("/admissions/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].details.paymentMethod").value(nullValue()))
                .andExpect(jsonPath("$.candidates[0].details.paymentTransactionRef").value(nullValue()));
    }

    @Test
    void soumissionSansModePaiementNInventePasWave() throws Exception {
        String body = mockMvc.perform(post("/candidatures/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Diop\",\"prenom\":\"Awa\",\"modePaiement\":\"\",\"referencePaiement\":\"\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        ArgumentCaptor<CandidateJpaEntity> captor = ArgumentCaptor.forClass(CandidateJpaEntity.class);
        verify(repo).save(captor.capture());
        assertNull(captor.getValue().getAdditionalInfo());
        assertNull(captor.getValue().getRegistrationFeeReference());
        aucuneFausseValeur(body);
        assertFalse(body.contains("WAVE"), body);
    }

    @Test
    void soumissionModePaiementAbsentDonneNull() throws Exception {
        mockMvc.perform(post("/candidatures/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Ba\",\"prenom\":\"Moussa\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CandidateJpaEntity> captor = ArgumentCaptor.forClass(CandidateJpaEntity.class);
        verify(repo).save(captor.capture());
        assertNull(captor.getValue().getAdditionalInfo());
        assertNull(captor.getValue().getRegistrationFeeReference());
    }

    @Test
    void soumissionModePaiementMalveillantNInjectePasDeJson() throws Exception {
        String malveillant = "WAVE\",\"admin\":true,\"x\":\"";
        String payload = JSON.writeValueAsString(Map.of("nom", "Sall", "prenom", "Fatou", "modePaiement", malveillant));
        mockMvc.perform(post("/candidatures/submit").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        ArgumentCaptor<CandidateJpaEntity> captor = ArgumentCaptor.forClass(CandidateJpaEntity.class);
        verify(repo).save(captor.capture());
        JsonNode info = JSON.readTree(captor.getValue().getAdditionalInfo());
        assertEquals(1, info.size(), "additionalInfo ne doit contenir que paymentMethod");
        assertFalse(info.has("admin"));
        assertEquals(malveillant, info.get("paymentMethod").asText());
    }

    @Test
    void profil360SansPaiementFictif() throws Exception {
        String body = mockMvc.perform(get("/admin/students/s-1/profile360"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments", hasSize(0)))
                .andExpect(jsonPath("$.paymentsStatus").value("non_disponible"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        aucuneFausseValeur(body);
    }

    @Test
    void readPaymentMethodCasLimites() {
        assertNull(CandidaturesController.textOrNull(null));
        assertNull(CandidaturesController.textOrNull("  "));
        assertEquals("WAVE", CandidaturesController.textOrNull("WAVE"));
    }
}
