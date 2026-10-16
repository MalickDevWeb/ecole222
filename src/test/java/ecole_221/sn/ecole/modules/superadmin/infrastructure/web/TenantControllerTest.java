package ecole_221.sn.ecole.modules.superadmin.infrastructure.web;

import ecole_221.sn.ecole.modules.superadmin.application.port.command.CreateTenantCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.command.UpdateTenantCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.TenantDto;
import ecole_221.sn.ecole.modules.superadmin.application.service.TenantService;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.TenantEmailAlreadyExistsException;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.TenantNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TenantController — Tests d'intégration")
class TenantControllerTest {

    private TenantService tenantService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tenantService = Mockito.mock(TenantService.class);
        TenantController controller = new TenantController(tenantService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new TenantExceptionHandler())
                .build();
    }

    private TenantDto createDummyDto(String id, String name, String status) {
        return new TenantDto(
                id, name, "ecole-test", "ECOLE", status, "Senegal", "Dakar", "Fann", "+221770000000",
                "contact@test.com", "www.test.com", null, LocalDateTime.now(), LocalDateTime.now(), null, null
        );
    }

    // ── GET /api/v1/super-admin/tenants ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/super-admin/tenants")
    class ListTenants {

        @Test
        @DisplayName("200 — Retourne la liste de tous les tenants")
        void shouldReturn200WithTenantList() throws Exception {
            TenantDto dto = createDummyDto("id-1", "Ecole A", "ACTIVE");
            Mockito.when(tenantService.listTenants(null)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/super-admin/tenants")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Ecole A"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        }
        
        @Test
        @DisplayName("200 — Retourne la liste filtrée par statut")
        void shouldReturn200WithFilteredList() throws Exception {
            TenantDto dto = createDummyDto("id-1", "Ecole P", "PENDING");
            Mockito.when(tenantService.listTenants("PENDING")).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/super-admin/tenants?status=PENDING")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }
    }

    // ── GET /api/v1/super-admin/tenants/{id} ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/super-admin/tenants/{id}")
    class GetTenant {

        @Test
        @DisplayName("200 — Retourne le tenant par son ID")
        void shouldReturn200WithTenant() throws Exception {
            TenantDto dto = createDummyDto("id-99", "Ecole Specifique", "ACTIVE");
            Mockito.when(tenantService.getTenant("id-99")).thenReturn(dto);

            mockMvc.perform(get("/api/v1/super-admin/tenants/id-99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("id-99"))
                    .andExpect(jsonPath("$.name").value("Ecole Specifique"));
        }

        @Test
        @DisplayName("404 — Tenant inexistant")
        void shouldReturn404WhenTenantNotFound() throws Exception {
            Mockito.when(tenantService.getTenant("unknown-id"))
                    .thenThrow(new TenantNotFoundException("unknown-id"));

            mockMvc.perform(get("/api/v1/super-admin/tenants/unknown-id")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ── POST /api/v1/super-admin/tenants ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/super-admin/tenants")
    class CreateTenant {

        @Test
        @DisplayName("201 — Crée un nouveau tenant avec des données valides")
        void shouldReturn201WhenValidData() throws Exception {
            TenantDto createdDto = createDummyDto("id-2", "Nouvelle Ecole", "PENDING");
            Mockito.when(tenantService.createTenant(any(CreateTenantCommand.class))).thenReturn(createdDto);

            String body = """
                    {
                        "name": "Nouvelle Ecole",
                        "email": "contact@ecole.sn",
                        "adminNom": "Diop",
                        "adminPrenom": "Moussa",
                        "adminEmail": "moussa@ecole.sn"
                    }
                    """;

            mockMvc.perform(post("/api/v1/super-admin/tenants")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Nouvelle Ecole"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("409 — Email déjà utilisé")
        void shouldReturn409WhenEmailExists() throws Exception {
            Mockito.when(tenantService.createTenant(any()))
                    .thenThrow(new TenantEmailAlreadyExistsException("contact@ecole.sn"));

            String body = """
                    {
                        "name": "Ecole",
                        "email": "contact@ecole.sn",
                        "adminNom": "Admin",
                        "adminPrenom": "Test",
                        "adminEmail": "admin@ecole.sn"
                    }
                    """;

            mockMvc.perform(post("/api/v1/super-admin/tenants")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        @DisplayName("400 — Champs obligatoires manquants")
        void shouldReturn400WhenFieldsMissing() throws Exception {
            String body = "{\"name\":\"\"}";
            mockMvc.perform(post("/api/v1/super-admin/tenants")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── PATCH /api/v1/super-admin/tenants/{id} ─────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/super-admin/tenants/{id}")
    class UpdateTenant {

        @Test
        @DisplayName("200 — Mise à jour partielle réussie")
        void shouldReturn200OnSuccess() throws Exception {
            TenantDto updatedDto = createDummyDto("id-1", "Ecole Modifiée", "ACTIVE");
            Mockito.when(tenantService.updateTenant(eq("id-1"), any(UpdateTenantCommand.class)))
                    .thenReturn(updatedDto);

            String body = "{\"name\":\"Ecole Modifiée\"}";

            mockMvc.perform(patch("/api/v1/super-admin/tenants/id-1")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Ecole Modifiée"));
        }
    }

    // ── DELETE /api/v1/super-admin/tenants/{id} ─────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/super-admin/tenants/{id}")
    class DeleteTenant {

        @Test
        @DisplayName("204 — Suppression réussie")
        void shouldReturn204OnSuccess() throws Exception {
            Mockito.doNothing().when(tenantService).deleteTenant("id-1");

            mockMvc.perform(delete("/api/v1/super-admin/tenants/id-1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }
    }

    // ── ACTIONS /api/v1/super-admin/tenants/{id}/* ──────────────────────────────

    @Nested
    @DisplayName("State transitions: approve, suspend, activate")
    class StateTransitions {

        @Test
        @DisplayName("200 — Approve tenant")
        void shouldReturn200OnApprove() throws Exception {
            TenantDto dto = createDummyDto("id-1", "Ecole", "ACTIVE");
            Mockito.when(tenantService.approveTenant("id-1")).thenReturn(dto);

            mockMvc.perform(post("/api/v1/super-admin/tenants/id-1/approve")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
        
        @Test
        @DisplayName("422 — Invalid state transition (e.g., approving an already active tenant)")
        void shouldReturn422OnInvalidState() throws Exception {
            Mockito.when(tenantService.approveTenant("id-2"))
                    .thenThrow(new IllegalStateException("Seules les organizations en statut PENDING peuvent être validées"));

            mockMvc.perform(post("/api/v1/super-admin/tenants/id-2/approve")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").exists());
        }
    }
}
