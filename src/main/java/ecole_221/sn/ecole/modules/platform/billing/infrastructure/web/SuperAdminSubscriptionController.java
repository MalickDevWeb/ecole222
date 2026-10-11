package ecole_221.sn.ecole.modules.platform.billing.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.billing.application.port.command.AssignPlanCommand;
import ecole_221.sn.ecole.modules.platform.billing.application.port.dto.SubscriptionDto;
import ecole_221.sn.ecole.modules.platform.billing.application.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/super-admin/subscriptions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
public class SuperAdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    public SuperAdminSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionDto> assignPlanToTenant(@Valid @RequestBody AssignPlanCommand command) {
        SubscriptionDto subscription = subscriptionService.subscribeTenant(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionDto>> listSubscriptions() {
        return ResponseEntity.ok(subscriptionService.listSubscriptions());
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<SubscriptionDto> getTenantSubscription(@PathVariable String tenantId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionForTenant(tenantId));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendSubscription(@PathVariable String id) {
        subscriptionService.suspendSubscription(id);
        return ResponseEntity.ok().build();
    }
}
