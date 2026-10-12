package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.application.dto.SchoolRegistrationRequest;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrganizationApplicationService {

    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final OrganizationUserRepositoryPort organizationUserRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    public OrganizationApplicationService(
            OrganizationRepositoryPort organizationRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            OrganizationUserRepositoryPort organizationUserRepositoryPort,
            PasswordEncoder passwordEncoder) {
        this.organizationRepositoryPort = organizationRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.organizationUserRepositoryPort = organizationUserRepositoryPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Organization> getAllOrganizations() {
        return organizationRepositoryPort.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Organization> getOrganizationById(String id) {
        return organizationRepositoryPort.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Organization> getOrganizationBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return organizationRepositoryPort.findBySlug(slug.trim().toLowerCase());
    }

    @Transactional
    public Organization suspendOrganization(String id) {
        Organization org = organizationRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        org.suspend();
        return organizationRepositoryPort.save(org);
    }

    @Transactional
    public Organization activateOrganization(String id) {
        Organization org = organizationRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        org.reactivate();
        return organizationRepositoryPort.save(org);
    }

    @Transactional
    public Organization registerSchool(SchoolRegistrationRequest request) {
        if (request.getSchoolName() == null || request.getSchoolName().trim().isBlank()) {
            throw new IllegalArgumentException("Le nom de l'établissement est obligatoire.");
        }
        if (request.getAdminEmail() == null || request.getAdminEmail().trim().isBlank()) {
            throw new IllegalArgumentException("L'adresse email de l'administrateur est obligatoire.");
        }

        String cleanEmail = request.getAdminEmail().trim().toLowerCase();
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            throw new IllegalArgumentException("L'adresse email '" + cleanEmail + "' n'est pas valide.");
        }

        // 1. Unicité stricte de l'Email
        if (userRepositoryPort.existsByEmail(cleanEmail)) {
            throw new ecole_221.sn.ecole.shared.error.AppException(
                ecole_221.sn.ecole.shared.error.ErrorCode.USER_EMAIL_ALREADY_EXISTS, 
                "L'adresse email (" + cleanEmail + ") est déjà utilisée par un compte administrateur."
            );
        }
        if (organizationRepositoryPort.existsByEmail(cleanEmail)) {
            throw new ecole_221.sn.ecole.shared.error.AppException(
                ecole_221.sn.ecole.shared.error.ErrorCode.TENANT_EMAIL_ALREADY_EXISTS, 
                "L'adresse email (" + cleanEmail + ") est déjà associée à un établissement."
            );
        }

        // 2. Unicité stricte du Téléphone
        String cleanPhone = null;
        if (request.getAdminPhone() != null && !request.getAdminPhone().trim().isBlank()) {
            cleanPhone = request.getAdminPhone().trim();
            if (userRepositoryPort.existsByTelephone(cleanPhone)) {
                throw new ecole_221.sn.ecole.shared.error.AppException(
                    ecole_221.sn.ecole.shared.error.ErrorCode.USER_PHONE_ALREADY_EXISTS, 
                    "Le numéro de téléphone (" + cleanPhone + ") est déjà associé à un compte administrateur."
                );
            }
        }

        // 3. Calcul et vérification d'unicité du slug
        String slug = request.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = request.getSchoolName().toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
            if (slug.isEmpty()) {
                slug = "ecole-" + System.currentTimeMillis();
            }
        }
        slug = slug.trim().toLowerCase();

        if (organizationRepositoryPort.existsBySlug(slug)) {
            throw new ecole_221.sn.ecole.shared.error.AppException(
                ecole_221.sn.ecole.shared.error.ErrorCode.TENANT_SLUG_ALREADY_EXISTS, 
                "L'identifiant d'école / slug '" + slug + "' est déjà réservé. Veuillez en choisir un autre."
            );
        }

        // 4. Construction des settings avec la couleur primaire choisie
        Map<String, Object> settings = new java.util.HashMap<>();
        if (request.getPrimaryColor() != null && !request.getPrimaryColor().isBlank()) {
            settings.put("primaryColor", request.getPrimaryColor().trim());
        }

        // 5. Création et sauvegarde de l'Organization
        Organization org = OrganizationFactory.create(
                request.getSchoolName().trim(),
                OrganizationType.ECOLE,
                OrganizationStatus.PENDING,
                slug,
                null,                  // country
                null,                  // city
                null,                  // address
                cleanPhone,            // phone
                cleanEmail,            // email
                null,                  // website
                null,                  // logo
                null,                  // subscriptionId
                null,                  // primaryDomainId
                settings.isEmpty() ? null : settings
        );
        Organization savedOrg = organizationRepositoryPort.save(org);

        // 6. Création du compte administrateur initial de l'école
        String matricule = "ADM-" + System.currentTimeMillis();
        String tempPassword = passwordEncoder.encode(cleanEmail + "221!");
        String nom = request.getAdminLastName() != null && !request.getAdminLastName().isBlank()
                ? request.getAdminLastName().trim() : "Directeur";
        String prenom = request.getAdminFirstName() != null && !request.getAdminFirstName().isBlank()
                ? request.getAdminFirstName().trim() : "Admin";

        User adminUser = UserFactory.create(
                nom,
                prenom,
                cleanEmail,
                tempPassword,
                matricule,
                RoleUtilisateur.fromString("ADMIN"),
                null,       // photoUrl
                cleanPhone, // telephone
                false,      // emailVerified
                false,      // phoneVerified
                "fr",
                "Africa/Dakar"
        );
        userRepositoryPort.save(adminUser);

        // 7. Lier l'administrateur à l'établissement via OrganizationUser
        ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser orgUser =
                OrganizationUserFactory.create(
                        adminUser.getId(),
                        savedOrg.getId(),
                        "ADMIN"
                );
        organizationUserRepositoryPort.save(orgUser);

        return savedOrg;
    }

    
    @Transactional
    public Organization approveSchool(String id) {
        Organization org = organizationRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        org.validate(); // Fait passer de PENDING à ACTIVE
        return organizationRepositoryPort.save(org);
    }
}