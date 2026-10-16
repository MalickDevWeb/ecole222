package ecole_221.sn.ecole.modules.superadmin.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.modules.superadmin.application.port.command.CreateTenantCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.command.UpdateTenantCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.TenantDto;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.TenantEmailAlreadyExistsException;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.TenantNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service applicatif pour la gestion des tenants (établissements scolaires).
 * Respecte l'architecture hexagonale : dépend des ports, jamais des adaptateurs JPA.
 */
@Service
public class TenantService {

    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort organizationUserRepositoryPort;

    public TenantService(
            OrganizationRepositoryPort organizationRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder,
            ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort organizationUserRepositoryPort) {
        this.organizationRepositoryPort = organizationRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.organizationUserRepositoryPort = organizationUserRepositoryPort;
    }

    // ── LIST ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TenantDto> listTenants(String statusFilter) {
        List<Organization> organizations;
        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equals("ALL")) {
            OrganizationStatus status = OrganizationStatus.fromString(statusFilter);
            organizations = organizationRepositoryPort.findByStatus(status);
        } else {
            organizations = organizationRepositoryPort.findAll().stream()
                .filter(org -> !org.getStatus().equals(OrganizationStatus.ARCHIVED))
                .collect(Collectors.toList());
        }
        return organizations.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── GET ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TenantDto getTenant(String tenantId) {
        Organization org = organizationRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        return toDto(org);
    }

    // ── CREATE ──────────────────────────────────────────────────────────────────

    @Transactional
    public TenantDto createTenant(CreateTenantCommand command) {
        // 1. Vérifier l'unicité email de l'établissement
        if (organizationRepositoryPort.existsByEmail(command.email())) {
            throw new IllegalArgumentException("L'email de l'établissement (" + command.email() + ") est déjà enregistré.");
        }
        
        if (userRepositoryPort.findByEmail(command.adminEmail()).isPresent()) {
            throw new IllegalArgumentException("L'email de l'administrateur (" + command.adminEmail() + ") est déjà enregistré.");
        }

        // 2. Créer l'établissement (statut PENDING par défaut avec slug unique)
        String slug = command.name().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            slug = "ecole-" + System.currentTimeMillis();
        }
        if (organizationRepositoryPort.existsBySlug(slug)) {
            slug = slug + "-" + (int)(Math.random() * 9000 + 1000);
        }

        Organization org = OrganizationFactory.create(
                command.name(),
                OrganizationType.ECOLE,
                OrganizationStatus.PENDING,
                slug,
                command.country(),
                command.city(),
                command.address(),
                command.phone(),
                command.email(),
                command.website(),
                command.logo(),
                null,  // subscriptionId
                null,  // primaryDomainId
                null   // settings
        );
        Organization savedOrg = organizationRepositoryPort.save(org);

        // 3. Créer l'utilisateur admin initial de l'école
        // Mot de passe temporaire : {email}221! — à changer dès la première connexion
        String matricule = "ADM-" + System.currentTimeMillis();
        String tempPassword = passwordEncoder.encode(command.adminEmail() + "221!");
        User adminUser = UserFactory.create(
                command.adminNom(),
                command.adminPrenom(),
                command.adminEmail(),
                tempPassword,
                matricule,
                RoleUtilisateur.fromString("ADMIN"),
                null   // photoUrl — aucune photo à la création
        );
        userRepositoryPort.save(adminUser);

        // 4. Lier l'admin à l'école via OrganizationUser
        ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser orgUser = 
            ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUserFactory.create(
                adminUser.getId(),
                savedOrg.getId(),
                "ADMIN"
        );
        organizationUserRepositoryPort.save(orgUser);

        return toDto(savedOrg);
    }

    // ── UPDATE (PATCH partiel) ───────────────────────────────────────────────────

    @Transactional
    public TenantDto updateTenant(String tenantId, UpdateTenantCommand command) {
        Organization org = organizationRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // Appliquer uniquement les champs non-null (sémantique PATCH)
        if (command.name() != null && !command.name().isBlank()) {
            org.setName(command.name());
        }
        if (command.country() != null) org.setCountry(command.country());
        if (command.city() != null)    org.setCity(command.city());
        if (command.address() != null) org.setAddress(command.address());
        if (command.phone() != null)   org.setPhone(command.phone());
        if (command.website() != null) org.setWebsite(command.website());
        if (command.logo() != null)    org.setLogo(command.logo());
        if (command.email() != null) {
            // Vérifie unicité seulement si l'email change
            if (!command.email().equals(org.getEmail())
                    && organizationRepositoryPort.existsByEmail(command.email())) {
                throw new IllegalArgumentException("L'email de l'établissement (" + command.email() + ") est déjà enregistré.");
            }
            org.setEmail(command.email());
        }

        return toDto(organizationRepositoryPort.save(org));
    }

    // ── DELETE ──────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteTenant(String tenantId) {
        Organization org = organizationRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        org.archive();
        organizationRepositoryPort.save(org);
    }

    // ── APPROVE ─────────────────────────────────────────────────────────────────

    @Transactional
    public TenantDto approveTenant(String tenantId) {
        Organization org = organizationRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        org.validate(); // PENDING → ACTIVE, lève IllegalStateException si état invalide
        return toDto(organizationRepositoryPort.save(org));
    }

    // ── SUSPEND ─────────────────────────────────────────────────────────────────

    @Transactional
    public TenantDto suspendTenant(String tenantId) {
        Organization org = organizationRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        org.suspend(); // ACTIVE → SUSPENDED, lève IllegalStateException si état invalide
        return toDto(organizationRepositoryPort.save(org));
    }

    // ── ACTIVATE ────────────────────────────────────────────────────────────────

    @Transactional
    public TenantDto activateTenant(String tenantId) {
        Organization org = organizationRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        org.reactivate(); // SUSPENDED → ACTIVE, lève IllegalStateException si état invalide
        return toDto(organizationRepositoryPort.save(org));
    }

    // ── MAPPER ──────────────────────────────────────────────────────────────────

    private TenantDto toDto(Organization org) {
        return new TenantDto(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getType().value(),
                org.getStatus().value(),
                org.getCountry(),
                org.getCity(),
                org.getAddress(),
                org.getPhone(),
                org.getEmail(),
                org.getWebsite(),
                org.getLogo(),
                org.getCreatedAt(),
                org.getUpdatedAt(),
                org.getValidatedAt(),
                org.getSuspendedAt()
        );
    }
}
