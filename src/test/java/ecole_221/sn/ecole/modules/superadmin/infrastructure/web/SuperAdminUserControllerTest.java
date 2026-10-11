package ecole_221.sn.ecole.modules.superadmin.infrastructure.web;

import ecole_221.sn.ecole.modules.superadmin.application.port.command.CreateGlobalUserCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.SuperAdminUserDto;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.CreateGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.DeleteGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.GetGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.ListGlobalUsersUseCase;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.EmailAlreadyExistsException;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.GlobalUserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SuperAdminUserController — Tests d'intégration")
class SuperAdminUserControllerTest {

    private ListGlobalUsersUseCase listUseCase;
    private CreateGlobalUserUseCase createUseCase;
    private GetGlobalUserUseCase getUseCase;
    private DeleteGlobalUserUseCase deleteUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        listUseCase = Mockito.mock(ListGlobalUsersUseCase.class);
        createUseCase = Mockito.mock(CreateGlobalUserUseCase.class);
        getUseCase = Mockito.mock(GetGlobalUserUseCase.class);
        deleteUseCase = Mockito.mock(DeleteGlobalUserUseCase.class);
        SuperAdminUserController controller = new SuperAdminUserController(
                listUseCase, createUseCase, getUseCase, deleteUseCase);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new SuperAdminExceptionHandler())
                .build();
    }

    // ── GET /api/v1/super-admin/users ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/super-admin/users")
    class ListGlobalUsers {

        @Test
        @DisplayName("200 — Retourne la liste des utilisateurs globaux")
        void shouldReturn200WithUserList() throws Exception {
            SuperAdminUserDto dto = new SuperAdminUserDto("id-1", "Diallo", "Mamadou",
                    "mamadou@ecole221.sn", "SUPER_ADMIN", "SA-221-0001");
            Mockito.when(listUseCase.execute()).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/super-admin/users")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("mamadou@ecole221.sn"))
                    .andExpect(jsonPath("$[0].role").value("SUPER_ADMIN"));
        }

        @Test
        @DisplayName("200 — Liste vide si aucun utilisateur global")
        void shouldReturn200WithEmptyList() throws Exception {
            Mockito.when(listUseCase.execute()).thenReturn(List.of());
            mockMvc.perform(get("/api/v1/super-admin/users").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ── POST /api/v1/super-admin/users ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/super-admin/users")
    class CreateGlobalUser {

        @Test
        @DisplayName("201 — Crée un nouvel admin avec des données valides")
        void shouldReturn201WhenValidData() throws Exception {
            SuperAdminUserDto createdDto = new SuperAdminUserDto("id-2", "Fall", "Fatou",
                    "fatou@ecole221.sn", "ADMIN", "GA-001");
            Mockito.when(createUseCase.execute(any(CreateGlobalUserCommand.class))).thenReturn(createdDto);

            String body = """
                    {"nom":"Fall","prenom":"Fatou","email":"fatou@ecole221.sn",
                     "password":"Admin@123!","role":"ADMIN"}
                    """;

            mockMvc.perform(post("/api/v1/super-admin/users")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("fatou@ecole221.sn"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("409 — Email déjà utilisé")
        void shouldReturn409WhenEmailExists() throws Exception {
            Mockito.when(createUseCase.execute(any())).thenThrow(new EmailAlreadyExistsException("fatou@ecole221.sn"));

            String body = """
                    {"nom":"Fall","prenom":"Fatou","email":"fatou@ecole221.sn",
                     "password":"Admin@123!","role":"ADMIN"}
                    """;

            mockMvc.perform(post("/api/v1/super-admin/users")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("400 — Champs obligatoires manquants")
        void shouldReturn400WhenFieldsMissing() throws Exception {
            String body = """
                    {"nom":"","prenom":"","email":"not-an-email","password":"","role":""}
                    """;
            mockMvc.perform(post("/api/v1/super-admin/users")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — Rôle non autorisé dans ce module")
        void shouldReturn400WhenInvalidRole() throws Exception {
            String body = """
                    {"nom":"Test","prenom":"User","email":"test@ecole221.sn",
                     "password":"Admin@123!","role":"ETUDIANT"}
                    """;
            mockMvc.perform(post("/api/v1/super-admin/users")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/v1/super-admin/users/{id} ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/super-admin/users/{userId}")
    class GetGlobalUser {

        @Test
        @DisplayName("200 — Retourne l'utilisateur par son ID")
        void shouldReturn200WithUser() throws Exception {
            SuperAdminUserDto dto = new SuperAdminUserDto("id-99", "Sow", "Aissatou",
                    "aissatou@ecole221.sn", "ADMIN", "GA-99");
            Mockito.when(getUseCase.execute("id-99")).thenReturn(dto);

            mockMvc.perform(get("/api/v1/super-admin/users/id-99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("id-99"))
                    .andExpect(jsonPath("$.email").value("aissatou@ecole221.sn"));
        }

        @Test
        @DisplayName("404 — Utilisateur inexistant")
        void shouldReturn404WhenUserNotFound() throws Exception {
            Mockito.when(getUseCase.execute("unknown-id"))
                    .thenThrow(new GlobalUserNotFoundException("unknown-id"));

            mockMvc.perform(get("/api/v1/super-admin/users/unknown-id")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ── DELETE /api/v1/super-admin/users/{id} ─────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/super-admin/users/{userId}")
    class DeleteGlobalUser {

        @Test
        @DisplayName("204 — Suppression réussie")
        void shouldReturn204OnSuccess() throws Exception {
            Mockito.doNothing().when(deleteUseCase).execute("id-1");

            mockMvc.perform(delete("/api/v1/super-admin/users/id-1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 — Utilisateur inexistant")
        void shouldReturn404WhenUserNotFound() throws Exception {
            Mockito.doThrow(new GlobalUserNotFoundException("unknown-id"))
                    .when(deleteUseCase).execute("unknown-id");

            mockMvc.perform(delete("/api/v1/super-admin/users/unknown-id")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }
    }
}
