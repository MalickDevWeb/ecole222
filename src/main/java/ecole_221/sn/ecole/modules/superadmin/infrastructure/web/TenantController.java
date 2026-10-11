package ecole_221.sn.ecole.modules.superadmin.infrastructure.web;

import ecole_221.sn.ecole.modules.superadmin.application.port.command.CreateTenantCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.command.UpdateTenantCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.TenantDto;
import ecole_221.sn.ecole.modules.superadmin.application.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour la gestion des tenants (établissements scolaires).
 * Accessible uniquement par SUPER_ADMIN et PLATFORM_ADMIN.
 * Route : /api/v1/super-admin/tenants
 */
@RestController
@RequestMapping({"/v1/super-admin/tenants", "/api/v1/super-admin/tenants"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * GET /api/v1/super-admin/tenants
     * Liste tous les établissements, avec filtre optionnel par statut.
     * Exemple : ?status=PENDING
     */
    @GetMapping
    public ResponseEntity<List<TenantDto>> listTenants(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(tenantService.listTenants(status));
    }

    /**
     * GET /api/v1/super-admin/tenants/{id}
     * Détails d'un établissement.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TenantDto> getTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.getTenant(id));
    }

    /**
     * POST /api/v1/super-admin/tenants
     * Crée un nouvel établissement + son administrateur initial.
     */
    @PostMapping
    public ResponseEntity<TenantDto> createTenant(
            @Valid @RequestBody CreateTenantCommand command) {
        TenantDto created = tenantService.createTenant(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH /api/v1/super-admin/tenants/{id}
     * Mise à jour partielle d'un établissement (seuls les champs non-null sont modifiés).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TenantDto> updateTenant(
            @PathVariable String id,
            @RequestBody UpdateTenantCommand command) {
        return ResponseEntity.ok(tenantService.updateTenant(id, command));
    }

    /**
     * DELETE /api/v1/super-admin/tenants/{id}
     * Supprime un établissement définitivement.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/super-admin/tenants/{id}/approve
     * Approuve un établissement en statut PENDING → ACTIVE.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<TenantDto> approveTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.approveTenant(id));
    }

    /**
     * POST /api/v1/super-admin/tenants/{id}/suspend
     * Suspend un établissement ACTIVE → SUSPENDED.
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<TenantDto> suspendTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.suspendTenant(id));
    }

    /**
     * POST /api/v1/super-admin/tenants/{id}/activate
     * Réactive un établissement SUSPENDED → ACTIVE.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<TenantDto> activateTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.activateTenant(id));
    }
}
