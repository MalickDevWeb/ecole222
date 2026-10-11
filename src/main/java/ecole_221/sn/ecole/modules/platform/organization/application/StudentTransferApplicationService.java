package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.MembershipStatus;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentTransferApplicationService {

    private final OrganizationUserRepositoryPort orgUserRepositoryPort;

    public StudentTransferApplicationService(OrganizationUserRepositoryPort orgUserRepositoryPort) {
        this.orgUserRepositoryPort = orgUserRepositoryPort;
    }

    /**
     * Transfère un élève d'une école A vers une école B.
     * Met le statut à TRANSFERRED dans l'ancienne école et crée un ACTIVE dans la nouvelle.
     */
    @Transactional
    public void transferStudent(String userId, String fromOrganizationId, String toOrganizationId, String reason) {
        // 1. Check if the user is currently a student in the 'from' organization
        List<OrganizationUser> memberships = orgUserRepositoryPort.findByUserId(userId);
        
        Optional<OrganizationUser> oldMembershipOpt = memberships.stream()
            .filter(m -> m.getOrganizationId().equals(fromOrganizationId))
            .filter(m -> m.getRole().equals(RoleUtilisateur.ETUDIANT.value()) || m.getRole().equals("sys_student_id")) // Support id or string
            .filter(OrganizationUser::isActive)
            .findFirst();

        if (oldMembershipOpt.isEmpty()) {
            throw new IllegalArgumentException("L'utilisateur n'est pas un élève actif dans l'école source.");
        }

        OrganizationUser oldMembership = oldMembershipOpt.get();

        // 2. Vérifier s'il est déjà dans l'école de destination (pour éviter les doublons)
        boolean alreadyInDest = memberships.stream()
            .anyMatch(m -> m.getOrganizationId().equals(toOrganizationId) && 
                          (m.getRole().equals(RoleUtilisateur.ETUDIANT.value()) || m.getRole().equals("sys_student_id")) &&
                          m.isActive());

        if (alreadyInDest) {
            throw new IllegalArgumentException("L'élève est déjà actif dans l'école de destination.");
        }

        // 3. Clôturer l'ancien membership
        oldMembership.setStatus(MembershipStatus.TRANSFERRED);
        orgUserRepositoryPort.save(oldMembership);

        // 4. Créer le nouveau membership dans l'école de destination
        OrganizationUser newMembership = OrganizationUserFactory.create(
            userId,
            toOrganizationId,
            oldMembership.getRole()
        );
        newMembership.activate(); // Passe de INVITED à ACTIVE
        orgUserRepositoryPort.save(newMembership);

        // TODO: Déclencher un événement de domaine (DomainEvent) pour que le module Académique 
        // puisse générer le Snapshot du Dossier (StudentTransferRecord) de manière asynchrone 
        // ou le récupérer, afin de le lier au transfert.
    }
}
