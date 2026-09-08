package ecole_221.sn.ecole.modules.auth.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.application.port.dto.LoginResponseDto;
import ecole_221.sn.ecole.modules.auth.application.port.usecase.LoginUseCase;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private LoginUseCase loginUseCase;
    private ActionLogger actionLogger;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        loginUseCase = Mockito.mock(LoginUseCase.class);
        actionLogger = Mockito.mock(ActionLogger.class);
        
        AuthController controller = new AuthController(loginUseCase, actionLogger);
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void testLoginToken() throws Exception {
        LoginResponseDto mockResponse = new LoginResponseDto();
        mockResponse.setToken("test-jwt-token");
        mockResponse.setEmail("test@ecole221.sn");
        mockResponse.setRole(RoleUtilisateur.ETUDIANT);
        mockResponse.setNom("Test");
        mockResponse.setPrenom("User");

        Mockito.when(loginUseCase.login(any(LoginCommand.class))).thenReturn(mockResponse);
        Mockito.doNothing().when(actionLogger).logAuthAction(anyString(), anyString(), anyString(), anyBoolean(), anyString());

        mockMvc.perform(post("/auth/login-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@ecole221.sn\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@ecole221.sn"));

        Mockito.verify(loginUseCase, Mockito.times(1)).login(any(LoginCommand.class));
    }

    @Test
    void testLogout() throws Exception {
        Mockito.doNothing().when(actionLogger).logAuthAction(anyString(), anyString(), anyString(), anyBoolean(), anyString());

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Déconnexion réussie"));

        Mockito.verify(actionLogger, Mockito.times(1)).logAuthAction(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void testMeWithoutAuth() throws Exception {
        mockMvc.perform(get("/auth/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}