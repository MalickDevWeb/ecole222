package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.application.OrganizationApplicationService;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/organizations")
public class SuperAdminController {

    private final OrganizationApplicationService organizationService;

    public SuperAdminController(OrganizationApplicationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<List<Organization>> listAllTenants() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getTenantDetails(@PathVariable String id) {
        return organizationService.getOrganizationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<Organization> suspendTenant(@PathVariable String id) {
        try {
            return ResponseEntity.ok(organizationService.suspendOrganization(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Organization> activateTenant(@PathVariable String id) {
        try {
            return ResponseEntity.ok(organizationService.activateOrganization(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<Organization> approveTenant(@PathVariable String id) {
        try {
            return ResponseEntity.ok(organizationService.approveSchool(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

