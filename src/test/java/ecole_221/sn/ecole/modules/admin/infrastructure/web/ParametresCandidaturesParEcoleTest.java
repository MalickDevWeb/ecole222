package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.admin.application.service.AdminService;
import ecole_221.sn.ecole.modules.admin.application.service.ParametresCandidaturesService;
import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ParametresCandidaturesJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ParametresCandidaturesJpaRepository;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paramètres de candidature : persistés PAR école (avant : map statique commune, ouverte du 2026-06-01 au
 * 2026-08-31 par défaut). Une école qui n'a rien enregistré est fermée, sans dates ni pièces exigées.
 */
class ParametresCandidaturesParEcoleTest {

    /** Table simulée : la clé est l'identifiant de l'école (id de la ligne), comme en base. */
    private final Map<String, ParametresCandidaturesJpaEntity> table = new HashMap<>();
    private ParametresCandidaturesService service;
    private MockMvc mockMvc;
    private Organization ecoleA;
    private Organization ecoleB;

    @BeforeEach
    void setUp() {
        ParametresCandidaturesJpaRepository depot = Mockito.mock(ParametresCandidaturesJpaRepository.class);
        Mockito.when(depot.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(table.get(inv.getArgument(0))));
        Mockito.when(depot.save(any(ParametresCandidaturesJpaEntity.class))).thenAnswer(inv -> {
            ParametresCandidaturesJpaEntity p = inv.getArgument(0);
            table.put(p.getId(), p);
            return p;
        });
        service = new ParametresCandidaturesService(depot);
        mockMvc = MockMvcBuilders.standaloneSetup(new CandidaturesController(Mockito.mock(CandidateJpaRepository.class), service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        ecoleA = OrganizationFactory.create("École A", OrganizationType.ECOLE, "a@exemple.sn");
        ecoleB = OrganizationFactory.create("École B", OrganizationType.ECOLE, "b@exemple.sn");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void ecoleSansParametresEstFermeeSansDateInventee() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        String corps = mockMvc.perform(get("/candidatures/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ouvert").value(false))
                .andExpect(jsonPath("$.dateOuverture").value(nullValue()))
                .andExpect(jsonPath("$.dateFermeture").value(nullValue()))
                .andExpect(jsonPath("$.messageAvis").value(nullValue()))
                .andExpect(jsonPath("$.docsRequis.cni").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(corps.contains("2026-06-01") || corps.contains("2026-08-31"), corps);
    }

    @Test
    void parametresEnregistresPourUneEcoleSeulement() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        mockMvc.perform(post("/candidatures/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ouvert\":true,\"dateOuverture\":\"2026-12-01\",\"dateFermeture\":\"2027-01-31\","
                                + "\"messageAvis\":\"Campagne 2027\",\"docsRequis\":{\"cni\":true,\"motivation\":false}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.ouvert").value(true));
        mockMvc.perform(get("/candidatures/settings"))
                .andExpect(jsonPath("$.dateOuverture").value("2026-12-01"))
                .andExpect(jsonPath("$.docsRequis.cni").value(true))
                .andExpect(jsonPath("$.messageAvis").value("Campagne 2027"));

        TenantContext.setCurrentOrganization(ecoleB);
        mockMvc.perform(get("/candidatures/settings"))
                .andExpect(jsonPath("$.ouvert").value(false))
                .andExpect(jsonPath("$.dateOuverture").value(nullValue()));
    }

    @Test
    void datesInvalidesOuInverseesRefusees() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        mockMvc.perform(post("/candidatures/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateOuverture\":\"demain\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/candidatures/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateOuverture\":\"2027-02-01\",\"dateFermeture\":\"2027-01-01\"}"))
                .andExpect(status().isBadRequest());
        assertTrue(table.isEmpty());
    }

    @Test
    void sansEcoleRienNEstEnregistre() throws Exception {
        mockMvc.perform(post("/candidatures/settings").contentType(MediaType.APPLICATION_JSON).content("{\"ouvert\":true}"))
                .andExpect(status().isBadRequest());
        assertTrue(table.isEmpty());
    }

    @Test
    void basculeAdminEcritLeMemeStockage() throws Exception {
        TenantContext.setCurrentOrganization(ecoleA);
        new AdminService(Mockito.mock(AdminRepositoryPort.class), service).updateCandidaturesSettings(true);
        mockMvc.perform(get("/candidatures/settings")).andExpect(jsonPath("$.ouvert").value(true));

        TenantContext.setCurrentOrganization(ecoleB);
        mockMvc.perform(get("/candidatures/settings")).andExpect(jsonPath("$.ouvert").value(false));
    }
}
