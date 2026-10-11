package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.MembershipStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class StudentTransferIntegrationTest {

    @Autowired
    private StudentEnrollmentService studentEnrollmentService;

    @Autowired
    private StudentTransferApplicationService studentTransferApplicationService;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private OrganizationRepositoryPort organizationRepositoryPort;

    @Autowired
    private OrganizationUserRepositoryPort orgUserRepositoryPort;

    private User student;
    private Organization schoolA;
    private Organization schoolB;

    @BeforeEach
    void setUp() {
        // Create a global user (Student)
        student = UserFactory.create("Diop", "Amadou", "amadou.diop@test.com", "password123", RoleUtilisateur.ETUDIANT);
        student = userRepositoryPort.save(student);

        // Create School A
        schoolA = OrganizationFactory.create("Ecole A", OrganizationType.ECOLE, "school-a-slug");
        organizationRepositoryPort.save(schoolA);

        // Create School B
        schoolB = OrganizationFactory.create("Ecole B", OrganizationType.ECOLE, "school-b-slug");
        organizationRepositoryPort.save(schoolB);
    }

    @Test
    @Transactional
    void testEnrollmentRule_StudentCannotBeInTwoSchools() {
        // 1. Enroll student in School A
        studentEnrollmentService.enrollStudent(student.getId(), schoolA.getId());
        
        // Assert student is active in School A
        List<OrganizationUser> memberships = orgUserRepositoryPort.findByUserId(student.getId());
        assertEquals(1, memberships.size());
        assertTrue(memberships.get(0).isActive());

        // 2. Try enrolling student in School B (should fail)
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            studentEnrollmentService.enrollStudent(student.getId(), schoolB.getId());
        });

        assertTrue(exception.getMessage().contains("une seule école"));
    }

    @Test
    @Transactional
    void testTransferProcess_Success() {
        // 1. Setup: Enroll student in School A
        studentEnrollmentService.enrollStudent(student.getId(), schoolA.getId());

        // 2. Execute Transfer to School B
        studentTransferApplicationService.transferStudent(student.getId(), schoolA.getId(), schoolB.getId(), "Déménagement");

        // 3. Verify Memberships
        List<OrganizationUser> memberships = orgUserRepositoryPort.findByUserId(student.getId());
        assertEquals(2, memberships.size());

        Optional<OrganizationUser> oldMembership = memberships.stream()
                .filter(m -> m.getOrganizationId().equals(schoolA.getId()))
                .findFirst();
        
        Optional<OrganizationUser> newMembership = memberships.stream()
                .filter(m -> m.getOrganizationId().equals(schoolB.getId()))
                .findFirst();

        assertTrue(oldMembership.isPresent());
        assertTrue(newMembership.isPresent());

        // Assert old school membership is TRANSFERRED
        assertEquals(MembershipStatus.TRANSFERRED, oldMembership.get().getStatus());

        // Assert new school membership is ACTIVE
        assertEquals(MembershipStatus.ACTIVE, newMembership.get().getStatus());
    }
}
