package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.DocumentEleveJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ProfilEleveJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.finance.application.service.SituationFinanciereEleveService;
import ecole_221.sn.ecole.modules.finance.application.service.SituationFinanciereEleveService.Situation;
import ecole_221.sn.ecole.modules.finance.application.service.SituationFinanciereEleveService.Versement;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fiche 360°, onglet Finances (2026-11-14) : versements réels du module Finances ; « non_disponible » sans dossier. */
class Profile360PaiementsReelsTest {

    private MockMvc fiche(SituationFinanciereEleveService situations) {
        ComptesEcoleService comptes = Mockito.mock(ComptesEcoleService.class);
        Mockito.when(comptes.membre("e42", "ETUDIANT")).thenReturn(new UserJpaEntity());
        ProfilEleveJpaRepository profils = Mockito.mock(ProfilEleveJpaRepository.class);
        Mockito.when(profils.findFirstByUserId("e42")).thenReturn(Optional.empty());
        AdminStudentProfileController c = new AdminStudentProfileController(comptes, profils,
                Mockito.mock(DocumentEleveJpaRepository.class), Mockito.mock(CourseJpaRepository.class),
                Mockito.mock(ScheduleSessionJpaRepository.class));
        c.setSituationsFinancieres(situations);
        return MockMvcBuilders.standaloneSetup(c).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void versementsReelsEtResumeDuDossier() throws Exception {
        SituationFinanciereEleveService situations = Mockito.mock(SituationFinanciereEleveService.class);
        Mockito.when(situations.situation("e42")).thenReturn(Optional.of(new Situation("e42", "L1", 300_000, 150_000, 150_000,
                "Partiel", LocalDateTime.of(2026, 10, 5, 10, 0),
                List.of(new Versement("t1", 100_000, LocalDateTime.of(2026, 10, 5, 10, 0), "Wave", "W-77", "Tranche 1")))));
        fiche(situations).perform(get("/admin/students/e42/profile360"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentsStatus").value("disponible"))
                .andExpect(jsonPath("$.payments[0].label").value("Tranche 1"))
                .andExpect(jsonPath("$.payments[0].amount").value(100_000))
                .andExpect(jsonPath("$.payments[0].date").value("05/10/2026"))
                .andExpect(jsonPath("$.payments[0].method").value("Wave"))
                .andExpect(jsonPath("$.payments[0].receiptNumber").value("W-77"))
                .andExpect(jsonPath("$.payments[1].label").value("Versements enregistrés sans détail"))
                .andExpect(jsonPath("$.situationFinanciere.resteDu").value(150_000))
                .andExpect(jsonPath("$.situationFinanciere.statut").value("Partiel"));
    }

    @Test
    void sansDossierFinancierNonDisponible() throws Exception {
        SituationFinanciereEleveService situations = Mockito.mock(SituationFinanciereEleveService.class);
        Mockito.when(situations.situation("e42")).thenReturn(Optional.empty());
        fiche(situations).perform(get("/admin/students/e42/profile360"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentsStatus").value("non_disponible"))
                .andExpect(jsonPath("$.payments").isEmpty());
    }
}
