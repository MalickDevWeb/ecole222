package ecole_221.sn.ecole.modules.platform.billing.application.service;

import ecole_221.sn.ecole.modules.platform.billing.application.port.command.AssignPlanCommand;
import ecole_221.sn.ecole.modules.platform.billing.application.port.dto.SubscriptionDto;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Plan;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Subscription;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.PlanRepositoryPort;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.SubscriptionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {
    
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;
    private final ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort etablissements;

    public SubscriptionService(SubscriptionRepositoryPort subscriptionRepository, PlanRepositoryPort planRepository,
                               ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort etablissements) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.etablissements = etablissements;
    }

    private static org.springframework.web.server.ResponseStatusException erreur(org.springframework.http.HttpStatus s, String m) {
        return new org.springframework.web.server.ResponseStatusException(s, m);
    }

    @Transactional
    public SubscriptionDto subscribeTenant(AssignPlanCommand cmd) {
        // test du 2026-11-10 : un abonnement était créé pour un établissement INEXISTANT, et avec une date de fin passée
        etablissements.findById(cmd.organizationId())
                .orElseThrow(() -> erreur(org.springframework.http.HttpStatus.NOT_FOUND, "Établissement introuvable."));
        Plan plan = planRepository.findById(cmd.planId())
                .orElseThrow(() -> erreur(org.springframework.http.HttpStatus.NOT_FOUND, "Offre introuvable."));
        if (cmd.endDate() != null && cmd.endDate().isBefore(java.time.LocalDateTime.now())) {
            throw erreur(org.springframework.http.HttpStatus.BAD_REQUEST, "La date de fin d'abonnement est déjà passée.");
        }
        
        Optional<Subscription> existing = subscriptionRepository.findByOrganizationId(cmd.organizationId());
        Subscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.changePlan(plan.getId());
            subscription.activate();
        } else {
            subscription = Subscription.create(cmd.organizationId(), plan.getId(), cmd.endDate());
        }
        
        Subscription saved = subscriptionRepository.save(subscription);
        return SubscriptionDto.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getSubscriptionForTenant(String tenantId) {
        Subscription subscription = subscriptionRepository.findByOrganizationId(tenantId)
            .orElseThrow(() -> erreur(org.springframework.http.HttpStatus.NOT_FOUND, "Aucun abonnement pour cet établissement."));
        return SubscriptionDto.fromDomain(subscription);
    }
    
    @Transactional(readOnly = true)
    public List<SubscriptionDto> listSubscriptions() {
        return subscriptionRepository.findAll().stream()
            .map(SubscriptionDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional
    public void suspendSubscription(String subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> erreur(org.springframework.http.HttpStatus.NOT_FOUND, "Abonnement introuvable."));
        if ("SUSPENDED".equalsIgnoreCase(subscription.getStatus())) {
            throw erreur(org.springframework.http.HttpStatus.CONFLICT, "Cet abonnement est déjà suspendu.");
        }
        subscription.suspend();
        subscriptionRepository.save(subscription);
    }
}
