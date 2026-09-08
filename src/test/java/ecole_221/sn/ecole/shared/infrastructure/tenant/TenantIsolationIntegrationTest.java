package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
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
    private UserRepositoryPort userRepositoryPort;

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
    void testIsolation_UserSavedInTenantA_CannotBeFoundInTenantB() {
        // Given Context is Tenant A
        TenantContext.setCurrentOrganization(orgA);

        // Create User in Tenant A
        User userA = UserFactory.create("Doe", "John", "studentA@example.com", "password", RoleUtilisateur.STUDENT);
        userRepositoryPort.save(userA);

        // Verify User A exists in Tenant A
        assertTrue(userRepositoryPort.findByEmail("studentA@example.com").isPresent());

        // When switching to Tenant B
        TenantContext.setCurrentOrganization(orgB);

        // Then User A should NOT be visible
        assertFalse(userRepositoryPort.findByEmail("studentA@example.com").isPresent());
    }
}
