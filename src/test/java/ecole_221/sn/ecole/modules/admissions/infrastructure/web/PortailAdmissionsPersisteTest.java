package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.admissions.application.port.usecase.AdmissionUseCase;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /admissions/db : plus de campagnes inventées (Unsplash), plus de formation « Licence Informatique » par
 * défaut, et campagnes / messages / journal enregistrés en base (table @TenantId) au lieu de la mémoire.
 */
class PortailAdmissionsPersisteTest {

    private CandidateJpaRepository candidats;
    private ElementPortailAdmissionJpaRepository elements;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        candidats = Mockito.mock(CandidateJpaRepository.class);
        elements = Mockito.mock(ElementPortailAdmissionJpaRepository.class);
        Mockito.when(candidats.findById(anyString())).thenReturn(java.util.Optional.empty());
        mockMvc = MockMvcBuilders.standaloneSetup(new AdmissionController(Mockito.mock(AdmissionUseCase.class), candidats, elements))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void ecoleSansDonneesNAucuneCampagneInventee() throws Exception {
        String corps = mockMvc.perform(get("/admissions/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaigns", hasSize(0)))
                .andExpect(jsonPath("$.messages", hasSize(0)))
                .andExpect(jsonPath("$.auditLogs", hasSize(0)))
                .andExpect(jsonPath("$.candidates", hasSize(0)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(corps.contains("unsplash") || corps.contains("Licence Informatique"), corps);
    }

    @Test
    void candidatSansFormationNAPasDeFormationParDefaut() throws Exception {
        CandidateJpaEntity e = new CandidateJpaEntity("c-1", "Awa", "Diop", "awa@test.sn", "770000000", null);
        Mockito.when(candidats.findAll()).thenReturn(List.of(e));
        mockMvc.perform(get("/admissions/db"))
                .andExpect(jsonPath("$.candidates[0].course").value(nullValue()))
                .andExpect(jsonPath("$.candidates[0].type").value(nullValue()));
    }

    @Test
    void campagnesEtMessagesEnregistresEnBase() throws Exception {
        mockMvc.perform(post("/admissions/db").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaigns\":[{\"id\":\"camp-x\",\"title\":\"BTS Génie Civil\"}],\"messages\":[{\"id\":\"m1\",\"text\":\"Bonjour\"}]}"))
                .andExpect(status().isOk());
        ArgumentCaptor<ElementPortailAdmissionJpaEntity> captor = ArgumentCaptor.forClass(ElementPortailAdmissionJpaEntity.class);
        verify(elements, Mockito.times(2)).save(captor.capture());
        assertEquals(ElementPortailAdmissionJpaEntity.CAMPAGNE, captor.getAllValues().get(0).getNature());
        assertTrue(captor.getAllValues().get(0).getContenuJson().contains("BTS Génie Civil"));
        assertEquals(ElementPortailAdmissionJpaEntity.MESSAGE, captor.getAllValues().get(1).getNature());
    }

    @Test
    void campagnesRelues() throws Exception {
        ElementPortailAdmissionJpaEntity e = new ElementPortailAdmissionJpaEntity();
        e.setId("x");
        e.setNature(ElementPortailAdmissionJpaEntity.CAMPAGNE);
        e.setContenuJson("{\"id\":\"camp-x\",\"title\":\"BTS Génie Civil\"}");
        Mockito.when(elements.findByNatureOrderByPositionAsc(ElementPortailAdmissionJpaEntity.CAMPAGNE)).thenReturn(List.of(e));
        mockMvc.perform(get("/admissions/db"))
                .andExpect(jsonPath("$.campaigns[0].title").value("BTS Génie Civil"));
    }

    @Test
    void candidatSansFormationRefuseEtRienNEstEcrit() throws Exception {
        mockMvc.perform(post("/admissions/db").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidates\":[{\"id\":\"c-9\",\"name\":\"Moussa Ba\"}],\"campaigns\":[]}"))
                .andExpect(status().isBadRequest());
        verify(candidats, never()).save(any());
        verify(elements, never()).save(any());
        verify(elements, never()).deleteAll(any());
    }
}
