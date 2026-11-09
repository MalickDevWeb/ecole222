package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.web.GlobalExceptionHandler;
import ecole_221.sn.ecole.modules.admin.application.service.PlanningService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.CreneauPlanningJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.CreneauPlanningJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.JpaPlanningRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.*;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /admin/planning/* : plus de salles / filières / classes / créneaux inventés, et les PUT sont réellement
 * enregistrés dans les tables de l'école (avant : mémoire seule, commune à toutes les écoles).
 * Le cloisonnement par école est assuré par @TenantId sur les entités (findAll() filtré par Hibernate).
 */
class AdminPlanningPersisteTest {

    private RoomJpaRepository rooms;
    private FiliereJpaRepository filieres;
    private ClasseJpaRepository classes;
    private CreneauPlanningJpaRepository creneaux;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        rooms = Mockito.mock(RoomJpaRepository.class);
        filieres = Mockito.mock(FiliereJpaRepository.class);
        classes = Mockito.mock(ClasseJpaRepository.class);
        creneaux = Mockito.mock(CreneauPlanningJpaRepository.class);
        JpaPlanningRepository depot = new JpaPlanningRepository(rooms, filieres, classes, creneaux);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPlanningController(new PlanningService(depot)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void ecoleSansDonneesNeVoitAucuneDonneeInventee() throws Exception {
        for (String chemin : List.of("rooms", "filieres", "classes", "slots")) {
            String corps = mockMvc.perform(get("/admin/planning/" + chemin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertFalse(corps.contains("Salle 101") || corps.contains("Génie Logiciel") || corps.contains("L3 GL"), corps);
        }
    }

    @Test
    void putSallesEnregistreRemplaceEtConserveLesColonnesNonGerees() throws Exception {
        RoomJpaEntity existante = new RoomJpaEntity();
        existante.setId("r1");
        existante.setName("Ancien nom");
        existante.setCapacity(10);
        existante.setLocation("Bâtiment B");
        RoomJpaEntity retiree = new RoomJpaEntity();
        retiree.setId("r-old");
        retiree.setName("À supprimer");
        Mockito.when(rooms.findAll()).thenReturn(List.of(existante, retiree));

        mockMvc.perform(put("/admin/planning/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"r1\",\"name\":\"Amphi Senghor\",\"capacity\":120,\"equipment\":[\"Projecteur\",\"Sono\"],\"status\":\"Disponible\"},"
                                + "{\"id\":\"r2\",\"name\":\"Salle TP\",\"capacity\":24,\"equipment\":[]}]"))
                .andExpect(status().isOk());

        ArgumentCaptor<RoomJpaEntity> captor = ArgumentCaptor.forClass(RoomJpaEntity.class);
        verify(rooms, Mockito.times(2)).save(captor.capture());
        RoomJpaEntity r1 = captor.getAllValues().get(0);
        assertEquals("Amphi Senghor", r1.getName());
        assertEquals(120, r1.getCapacity());
        assertEquals("Projecteur,Sono", r1.getEquipment());
        assertEquals("Bâtiment B", r1.getLocation());
        assertNull(captor.getAllValues().get(1).getEquipment());   // aucun équipement présumé
        verify(rooms).delete(retiree);
        verify(rooms, never()).delete(existante);
    }

    @Test
    void putCreneauxAuFormatDuFrontEstEnregistreSansValeurInventee() throws Exception {
        Mockito.when(creneaux.findAll()).thenReturn(List.of());
        mockMvc.perform(put("/admin/planning/slots").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"p-1\",\"day\":\"Lundi\",\"slot\":\"08h - 10h\",\"classeId\":\"c-1\",\"subject\":\"RDM\"}]"))
                .andExpect(status().isOk());

        ArgumentCaptor<CreneauPlanningJpaEntity> captor = ArgumentCaptor.forClass(CreneauPlanningJpaEntity.class);
        verify(creneaux).save(captor.capture());
        CreneauPlanningJpaEntity c = captor.getValue();
        assertEquals("Lundi", c.getJour());
        assertEquals("08h", c.getHeureDebut());
        assertEquals("10h", c.getHeureFin());
        assertEquals("RDM", c.getNom());
        assertEquals("c-1", c.getClasseId());
        assertNull(c.getProfesseur());   // avant : « Professeur »
        assertNull(c.getSalle());        // avant : « Salle »
        assertNull(c.getType());         // avant : « CM »
    }

    @Test
    void creneauRelusSansEnseignantResteNull() throws Exception {
        CreneauPlanningJpaEntity c = new CreneauPlanningJpaEntity();
        c.setId("s1");
        c.setJour("Mardi");
        c.setHeureDebut("08:00");
        c.setHeureFin("10:00");
        c.setClasseId("c-1");
        Mockito.when(creneaux.findAll()).thenReturn(List.of(c));
        mockMvc.perform(get("/admin/planning/slots"))
                .andExpect(jsonPath("$[0].jour").value("Mardi"))
                .andExpect(jsonPath("$[0].professeur").value(nullValue()))
                .andExpect(jsonPath("$[0].salle").value(nullValue()));
    }

    @Test
    void champObligatoireManquantDonne400EtRienNEstEcrit() throws Exception {
        mockMvc.perform(put("/admin/planning/slots").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"p-1\",\"day\":\"Lundi\",\"slot\":\"08h - 10h\"}]"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/admin/planning/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"r1\",\"capacity\":30}]"))
                .andExpect(status().isBadRequest());
        verify(creneaux, never()).save(any());
        verify(rooms, never()).save(any());
        verify(rooms, never()).delete(any());
    }

    @Test
    void putClassesEnregistreNiveauEtCapacite() throws Exception {
        Mockito.when(classes.findAll()).thenReturn(List.of());
        mockMvc.perform(put("/admin/planning/classes").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"c-1\",\"name\":\"L1 GC\",\"level\":\"L1\",\"filiereId\":\"f-1\",\"capacityMax\":35,\"price\":null}]"))
                .andExpect(status().isOk());
        ArgumentCaptor<ClasseJpaEntity> captor = ArgumentCaptor.forClass(ClasseJpaEntity.class);
        verify(classes).save(captor.capture());
        assertEquals("L1 GC", captor.getValue().getName());
        assertEquals("L1", captor.getValue().getLevel());
        assertEquals(35, captor.getValue().getCapacity());
        assertEquals("f-1", captor.getValue().getFiliereId());
    }

    @Test
    void putClassesEnregistreLesFraisAnnuels() throws Exception {
        // avant : frais saisis à l'écran (450 000) puis perdus — affichés « — » après rechargement
        Mockito.when(classes.findAll()).thenReturn(List.of());
        mockMvc.perform(put("/admin/planning/classes").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"c-2\",\"name\":\"L1 INFO\",\"level\":\"L1\",\"filiereId\":\"f-1\",\"capacityMax\":35,\"price\":450000}]"))
                .andExpect(status().isOk());
        ArgumentCaptor<ClasseJpaEntity> captor = ArgumentCaptor.forClass(ClasseJpaEntity.class);
        verify(classes).save(captor.capture());
        assertEquals(450000L, captor.getValue().getFraisAnnuels());
    }

    @Test
    void fraisNegatifsRefuses() throws Exception {
        mockMvc.perform(put("/admin/planning/classes").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"c-3\",\"name\":\"L1\",\"filiereId\":\"f-1\",\"price\":-5}]"))
                .andExpect(status().isBadRequest());
    }
}
