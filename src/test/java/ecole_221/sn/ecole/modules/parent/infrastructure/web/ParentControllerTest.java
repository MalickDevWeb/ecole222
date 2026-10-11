package ecole_221.sn.ecole.modules.parent.infrastructure.web;

import ecole_221.sn.ecole.modules.parent.application.port.dto.ChildDto;
import ecole_221.sn.ecole.modules.parent.application.port.usecase.ParentUseCase;
import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour ParentController
 */
class ParentControllerTest {

    private ParentUseCase parentUseCase;
    private JwtService jwtService;
    private UserRepositoryPort userRepositoryPort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        parentUseCase = Mockito.mock(ParentUseCase.class);
        jwtService = Mockito.mock(JwtService.class);
        userRepositoryPort = Mockito.mock(UserRepositoryPort.class);

        ParentController controller = new ParentController(parentUseCase, jwtService, userRepositoryPort);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void testGetEnfantsWithValidToken() throws Exception {
        // Setup mock user with PARENT role
        User mockUser = UserFactory.reconstructSimple("usr-parent-01", "Diallo", "Mamadou", "parent221@gmail.com", 
                                                "ecole221", "PARENT-001", RoleUtilisateur.PARENT, null);
        
        // Mock JWT extraction for this specific test
        Mockito.when(jwtService.extractUsername(anyString())).thenReturn("parent221@gmail.com");
        Mockito.when(userRepositoryPort.findByEmail("parent221@gmail.com")).thenReturn(Optional.of(mockUser));
        
        // Mock use case
        List<ChildDto> mockChildren = List.of(
            new ChildDto("child-1", "Diallo", "Aminata", "E221-2024-001", 
                        "Informatique", "L2", "L2-INFO-A", null)
        );
        Mockito.when(parentUseCase.getEnfantsByParentId(anyString())).thenReturn(mockChildren);

        // Execute and verify
        mockMvc.perform(get("/parent/enfants")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("child-1"))
                .andExpect(jsonPath("$[0].nom").value("Diallo"));

        Mockito.verify(parentUseCase, Mockito.times(1)).getEnfantsByParentId(anyString());
    }

    @Test
    void testGetEnfantsWithoutToken() throws Exception {
        mockMvc.perform(get("/parent/enfants"))
                .andExpect(status().isBadRequest()); // Missing header = 400 Bad Request
    }

    @Test
    void testGetChildStats() throws Exception {
        // Setup mock user with PARENT role
        User mockUser = UserFactory.reconstructSimple("usr-parent-01", "Diallo", "Mamadou", "parent221@gmail.com", 
                                                "ecole221", "PARENT-001", RoleUtilisateur.PARENT, null);
        
        // Mock JWT extraction
        Mockito.when(jwtService.extractUsername(anyString())).thenReturn("parent221@gmail.com");
        Mockito.when(userRepositoryPort.findByEmail("parent221@gmail.com")).thenReturn(Optional.of(mockUser));
        Mockito.when(userRepositoryPort.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        
        // Mock use case
        ChildDto mockChild = new ChildDto("child-1", "Diallo", "Aminata", "E221-2024-001", 
                                       "Informatique", "L2", "L2-INFO-A", null);
        Mockito.when(parentUseCase.getEnfantsByParentId(anyString())).thenReturn(List.of(mockChild));

        mockMvc.perform(get("/parent/enfants/child-1/stats")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());

        Mockito.verify(parentUseCase, Mockito.times(1)).getChildStats("child-1");
    }
}
