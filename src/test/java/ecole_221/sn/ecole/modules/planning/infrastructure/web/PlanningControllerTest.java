package ecole_221.sn.ecole.modules.planning.infrastructure.web;

import ecole_221.sn.ecole.modules.planning.infrastructure.web.PlanningController;
import ecole_221.sn.ecole.modules.planning.application.port.usecase.PlanningUseCase;
import ecole_221.sn.ecole.modules.planning.application.port.dto.*;
import ecole_221.sn.ecole.partage.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlanningControllerTest {

    private PlanningUseCase planningUseCase;

    private MockMvc mockMvc;

    private List<RoomDto> mockRooms;
    private List<FiliereDto> mockFilieres;
    private PageResult<ClasseDto> mockClasses;
    private List<SlotDto> mockSlots;

    @BeforeEach
    void setUp() {
        planningUseCase = Mockito.mock(PlanningUseCase.class);
        
        PlanningController controller = new PlanningController(planningUseCase);
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        // Mock data
        mockRooms = Arrays.asList(
            new RoomDto("S001", "Salle A", 30, "projecteur", "Bâtiment A"),
            new RoomDto("S002", "Salle B", 40, "tableau", "Bâtiment B")
        );

        mockFilieres = Arrays.asList(
            new FiliereDto("F001", "Génie Civil", "GC", "Description GC", "Licence"),
            new FiliereDto("F002", "Informatique", "INFO", "Description INFO", "Licence")
        );

        mockClasses = new PageResult<ClasseDto>(
            Arrays.asList(
                new ClasseDto("C001", "L1 GL", "L1-GL", "F001", "L1", 30, 25),
                new ClasseDto("C002", "L2 GL", "L2-GL", "F001", "L2", 30, 20)
            ),
            0, 2, 2, 1
        );

        mockSlots = Arrays.asList(
            new SlotDto("T001", "08:00", "10:00", "Lundi", "CM"),
            new SlotDto("T002", "10:00", "12:00", "Lundi", "TD")
        );
    }

    @Test
    void testGetRooms() throws Exception {
        Mockito.when(planningUseCase.getAllRooms()).thenReturn(mockRooms);

        mockMvc.perform(get("/planning/rooms")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("S001"))
                .andExpect(jsonPath("$[0].name").value("Salle A"))
                .andExpect(jsonPath("$[0].capacity").value(30));

        Mockito.verify(planningUseCase, Mockito.times(1)).getAllRooms();
    }

    @Test
    void testUpdateRooms() throws Exception {
        RoomDto updatedRoom = new RoomDto("S001", "Salle A Updated", 35, "projecteur", "Bâtiment A");
        Mockito.when(planningUseCase.saveRoom(any(RoomDto.class))).thenReturn(updatedRoom);

        mockMvc.perform(post("/planning/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"S001\",\"name\":\"Salle A Updated\",\"capacity\":35,\"equipment\":\"projecteur\",\"location\":\"Bâtiment A\"}"))
                .andExpect(status().isOk());

        Mockito.verify(planningUseCase, Mockito.times(1)).saveRoom(any(RoomDto.class));
    }

    @Test
    void testGetFilieres() throws Exception {
        Mockito.when(planningUseCase.getAllFilieres()).thenReturn(mockFilieres);

        mockMvc.perform(get("/planning/filieres")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("F001"))
                .andExpect(jsonPath("$[0].code").value("GC"))
                .andExpect(jsonPath("$[0].name").value("Génie Civil"));

        Mockito.verify(planningUseCase, Mockito.times(1)).getAllFilieres();
    }

    @Test
    void testUpdateFilieres() throws Exception {
        FiliereDto updatedFiliere = new FiliereDto("F001", "Génie Civil Updated", "GC", "Description GC", "Licence");
        Mockito.when(planningUseCase.saveFiliere(any(FiliereDto.class))).thenReturn(updatedFiliere);

        mockMvc.perform(post("/planning/filieres")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"F001\",\"name\":\"Génie Civil Updated\",\"code\":\"GC\",\"description\":\"Description GC\",\"level\":\"Licence\"}"))
                .andExpect(status().isOk());

        Mockito.verify(planningUseCase, Mockito.times(1)).saveFiliere(any(FiliereDto.class));
    }

    @Test
    void testGetClasses() throws Exception {
        Mockito.when(planningUseCase.getAllClasses(0, 10)).thenReturn(mockClasses);

        mockMvc.perform(get("/planning/classes")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("C001"))
                .andExpect(jsonPath("$.content[0].name").value("L1 GL"));

        Mockito.verify(planningUseCase, Mockito.times(1)).getAllClasses(0, 10);
    }

    @Test
    void testUpdateClasses() throws Exception {
        ClasseDto updatedClasse = new ClasseDto("C001", "L1 GL Updated", "L1-GL", "F001", "L1", 30, 25);
        Mockito.when(planningUseCase.saveClasse(any(ClasseDto.class))).thenReturn(updatedClasse);

        mockMvc.perform(post("/planning/classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"C001\",\"name\":\"L1 GL Updated\",\"code\":\"L1-GL\",\"filiereId\":\"F001\",\"level\":\"L1\",\"capacity\":30,\"currentSize\":25}"))
                .andExpect(status().isOk());

        Mockito.verify(planningUseCase, Mockito.times(1)).saveClasse(any(ClasseDto.class));
    }

    @Test
    void testGetSlots() throws Exception {
        Mockito.when(planningUseCase.getAllSlots()).thenReturn(mockSlots);

        mockMvc.perform(get("/planning/slots")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("T001"))
                .andExpect(jsonPath("$[0].dayOfWeek").value("Lundi"))
                .andExpect(jsonPath("$[0].startTime").value("08:00"))
                .andExpect(jsonPath("$[0].endTime").value("10:00"));

        Mockito.verify(planningUseCase, Mockito.times(1)).getAllSlots();
    }

    @Test
    void testUpdateSlots() throws Exception {
        SlotDto updatedSlot = new SlotDto("T001", "09:00", "11:00", "Lundi", "CM");
        Mockito.when(planningUseCase.saveSlot(any(SlotDto.class))).thenReturn(updatedSlot);

        mockMvc.perform(post("/planning/slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"T001\",\"startTime\":\"09:00\",\"endTime\":\"11:00\",\"dayOfWeek\":\"Lundi\",\"type\":\"CM\"}"))
                .andExpect(status().isOk());

        Mockito.verify(planningUseCase, Mockito.times(1)).saveSlot(any(SlotDto.class));
    }
}