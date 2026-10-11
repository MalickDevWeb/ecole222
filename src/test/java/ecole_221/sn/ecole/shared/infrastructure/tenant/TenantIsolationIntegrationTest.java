package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.FiliereJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.FiliereJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TenantIsolationIntegrationTest {

    @Autowired
    private FiliereJpaRepository filiereJpaRepository;

    @Autowired
    private OrganizationRepositoryPort organizationRepositoryPort;

    private Organization orgA;
    private Organization orgB;

    @BeforeEach
    void setUp() {
        // Clear context before each test
        TenantContext.clear();

        // Create Tenant A
        orgA = OrganizationFactory.create("School A", OrganizationType.ECOLE, "school-a");
        organizationRepositoryPort.save(orgA);

        // Create Tenant B
        orgB = OrganizationFactory.create("School B", OrganizationType.ECOLE, "school-b");
        organizationRepositoryPort.save(orgB);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testIsolation_EntitySavedInTenantA_CannotBeFoundInTenantB() {
        // Given Context is Tenant A
        TenantContext.setCurrentOrganization(orgA);

        // Create Filiere in Tenant A
        FiliereJpaEntity filiere = new FiliereJpaEntity();
        filiere.setId("fil-1");
        filiere.setName("Maths");
        filiere.setCode("MATH");
        filiere.setDescription("Mathematics");
        filiere.setLevel("L1");
        // We do NOT set organizationId manually because @TenantId is supposed to handle it
        filiereJpaRepository.save(filiere);

        // Verify Filiere exists in Tenant A
        assertTrue(filiereJpaRepository.findById("fil-1").isPresent());

        // When switching to Tenant B
        TenantContext.setCurrentOrganization(orgB);

        // Then Filiere should NOT be visible
        assertFalse(filiereJpaRepository.findById("fil-1").isPresent());
    }
}
