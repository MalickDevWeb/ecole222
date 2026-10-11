package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentEnrollmentService {

    private final OrganizationUserRepositoryPort orgUserRepositoryPort;

    public StudentEnrollmentService(OrganizationUserRepositoryPort orgUserRepositoryPort) {
        this.orgUserRepositoryPort = orgUserRepositoryPort;
    }

    /**
     * Inscrit un élève dans une école.
     * Règle métier : Un élève ne peut être actif que dans une seule école.
     */
    @Transactional
    public void enrollStudent(String userId, String organizationId) {
        List<OrganizationUser> memberships = orgUserRepositoryPort.findByUserId(userId);
        
        // 1. Check if user is already an active student somewhere else
        boolean hasActiveStudentRole = memberships.stream()
            .filter(m -> m.getRole().equals(RoleUtilisateur.ETUDIANT.value()) || m.getRole().equals("sys_student_id"))
            .anyMatch(OrganizationUser::isActive);

        if (hasActiveStudentRole) {
            throw new IllegalStateException("Règle métier violée : Un élève ne peut être actif que dans une seule école à la fois. Utilisez le processus de transfert.");
        }

        // 2. Enroll the student
        OrganizationUser newMembership = OrganizationUserFactory.create(
            userId,
            organizationId,
            RoleUtilisateur.ETUDIANT.value()
        );
        newMembership.activate();
        orgUserRepositoryPort.save(newMembership);
    }
}
