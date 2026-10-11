package ecole_221.sn.ecole.modules.platform.billing.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.billing.application.port.command.AddEntitlementCommand;
import ecole_221.sn.ecole.modules.platform.billing.application.port.command.CreatePlanCommand;
import ecole_221.sn.ecole.modules.platform.billing.application.port.dto.PlanDto;
import ecole_221.sn.ecole.modules.platform.billing.application.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/super-admin/plans")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
public class SuperAdminPlanController {

    private final PlanService planService;

    public SuperAdminPlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ResponseEntity<PlanDto> createPlan(@Valid @RequestBody CreatePlanCommand command) {
        PlanDto plan = planService.createPlan(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    @GetMapping
    public ResponseEntity<List<PlanDto>> listPlans() {
        return ResponseEntity.ok(planService.listPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanDto> getPlan(@PathVariable String id) {
        return ResponseEntity.ok(planService.getPlan(id));
    }

    @PostMapping("/{id}/entitlements")
    public ResponseEntity<PlanDto> addEntitlement(@PathVariable String id, @Valid @RequestBody AddEntitlementCommand command) {
        PlanDto plan = planService.addEntitlement(id, command);
        return ResponseEntity.ok(plan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable String id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
