package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.DocumentEleveJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ProfilEleveJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ProfilEleveJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** APP-BE-17 : profil 360 sans données inventées, persisté, limité aux élèves de l'école. */
class Profile360SansDonneesInventeesTest {

    private ComptesEcoleService comptesEcole;
    private ProfilEleveJpaRepository profils;
    private CourseJpaRepository cours;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        comptesEcole = Mockito.mock(ComptesEcoleService.class);
        profils = Mockito.mock(ProfilEleveJpaRepository.class);
        cours = Mockito.mock(CourseJpaRepository.class);
        Mockito.when(comptesEcole.membre("e42", "ETUDIANT")).thenReturn(new UserJpaEntity());
        Mockito.when(comptesEcole.membre("autre", "ETUDIANT")).thenThrow(
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "introuvable"));
        mvc = MockMvcBuilders.standaloneSetup(new AdminStudentProfileController(comptesEcole, profils,
                        Mockito.mock(DocumentEleveJpaRepository.class), cours, Mockito.mock(ScheduleSessionJpaRepository.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void profilParDefautVideEtExplicite() throws Exception {
        Mockito.when(profils.findFirstByUserId("e42")).thenReturn(Optional.empty());
        mvc.perform(get("/admin/students/e42/profile360"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donneesStatus").value("non_disponible"))
                .andExpect(jsonPath("$.ueList").isEmpty())
                .andExpect(jsonPath("$.connectionLogs").isEmpty())
                .andExpect(content().string(not(containsString("O+"))))
                .andExpect(content().string(not(containsString("Seydou Nourou"))))
                .andExpect(content().string(not(containsString("MAT-2023"))));
    }

    @Test
    void groupeSanguinSaisiEstEnregistre() throws Exception {
        Mockito.when(profils.findFirstByUserId("e42")).thenReturn(Optional.empty());
        mvc.perform(post("/admin/students/e42/profile360").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bloodGroup\":\"A+\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extra.sante").value("A+"));
        ArgumentCaptor<ProfilEleveJpaEntity> saisi = ArgumentCaptor.forClass(ProfilEleveJpaEntity.class);
        Mockito.verify(profils).save(saisi.capture());
        assertEquals("A+", saisi.getValue().getGroupeSanguin());
    }

    @Test
    void eleveDUneAutreEcoleInaccessible() throws Exception {
        mvc.perform(get("/admin/students/autre/profile360")).andExpect(status().isNotFound());
        mvc.perform(post("/admin/students/autre/profile360").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bloodGroup\":\"B-\"}")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/students/autre/documents")).andExpect(status().isNotFound());
        Mockito.verify(profils, Mockito.never()).save(any());
    }

    /** APP-TL-24 : la synthèse du suivi remplace le « diagnostic IA » ; jamais pour un élève d'une autre école. */
    @Test
    void syntheseDuSuiviLimiteeAuxElevesDeLEcole() throws Exception {
        var syntheses = Mockito.mock(ecole_221.sn.ecole.modules.student.application.service.SyntheseSuiviEleveService.class);
        Mockito.when(syntheses.synthese("e42")).thenReturn(java.util.Map.of("niveau", "donnees_insuffisantes"));
        var controleur = new AdminStudentProfileController(comptesEcole, profils,
                Mockito.mock(DocumentEleveJpaRepository.class), cours, Mockito.mock(ScheduleSessionJpaRepository.class));
        controleur.setSyntheses(syntheses);
        MockMvc mvcSynthese = MockMvcBuilders.standaloneSetup(controleur).setControllerAdvice(new GlobalExceptionHandler()).build();

        mvcSynthese.perform(get("/admin/students/autre/synthese")).andExpect(status().isNotFound());
        Mockito.verify(syntheses, Mockito.never()).synthese("autre");
        mvcSynthese.perform(get("/admin/students/e42/synthese"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.niveau").value("donnees_insuffisantes"));
    }

    @Test
    void affectationDeCreneauSansCoursRefusee() throws Exception {
        Mockito.when(cours.findById("crs-x")).thenReturn(Optional.empty());
        mvc.perform(post("/admin/schedule/assign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseId\":\"crs-x\",\"day\":\"MONDAY\",\"time\":\"08:00\",\"room\":\"A1\"}"))
                .andExpect(status().isNotFound());
    }
}
