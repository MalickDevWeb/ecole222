package ecole_221.sn.ecole.modules.platform.billing.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Entitlement;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Plan;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Subscription;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.PlanRepositoryPort;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.SubscriptionRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** APP-FE-11 : « Mon abonnement » = offre et consommation RÉELLES de l'école courante. */
class AbonnementEcoleControllerTest {

    private final SubscriptionRepositoryPort abonnements = Mockito.mock(SubscriptionRepositoryPort.class);
    private final PlanRepositoryPort offres = Mockito.mock(PlanRepositoryPort.class);
    private final OrganizationUserRepositoryPort appartenances = Mockito.mock(OrganizationUserRepositoryPort.class);
    private final AbonnementEcoleController controleur = new AbonnementEcoleController(abonnements, offres, appartenances);

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
    }

    @Test
    void sansAbonnementDisponibleFaux() {
        var ecole = OrganizationFactory.create("A", OrganizationType.ECOLE, "a@exemple.sn");
        TenantContext.setCurrentOrganization(ecole);
        Mockito.when(abonnements.findByOrganizationId(ecole.getId())).thenReturn(Optional.empty());
        assertEquals(Boolean.FALSE, controleur.abonnement().getBody().get("disponible"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void offreEtConsommationReelles() {
        var ecole = OrganizationFactory.create("A", OrganizationType.ECOLE, "a@exemple.sn");
        TenantContext.setCurrentOrganization(ecole);
        Plan plan = new Plan("plan-1", "Essentiel", "Pour démarrer", new BigDecimal("25000"), new BigDecimal("250000"),
                true, LocalDateTime.now(), List.of(new Entitlement("e1", "MAX_STUDENTS", 200, true)));
        Mockito.when(abonnements.findByOrganizationId(ecole.getId()))
                .thenReturn(Optional.of(Subscription.create(ecole.getId(), "plan-1", LocalDateTime.of(2027, 1, 1, 0, 0))));
        Mockito.when(offres.findById("plan-1")).thenReturn(Optional.of(plan));
        Mockito.when(appartenances.findByOrganizationIdAndRole(ecole.getId(), "ETUDIANT")).thenReturn(List.of(
                OrganizationUserFactory.create("u1", ecole.getId(), "ETUDIANT"),
                OrganizationUserFactory.create("u2", ecole.getId(), "ETUDIANT")));

        Map<String, Object> corps = controleur.abonnement().getBody();
        assertEquals(Boolean.TRUE, corps.get("disponible"));
        Map<String, Object> offre = (Map<String, Object>) corps.get("plan");
        assertEquals("Essentiel", offre.get("name"));
        assertEquals("2027-01-01", offre.get("endDate"));
        Map<String, Object> quota = ((List<Map<String, Object>>) offre.get("features")).get(0);
        assertEquals(200, quota.get("limit"));
        assertEquals(2, quota.get("used"), "consommation = élèves actifs réels");
    }
}
