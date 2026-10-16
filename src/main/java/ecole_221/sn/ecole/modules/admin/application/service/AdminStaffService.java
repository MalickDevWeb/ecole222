package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.*;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminStaffService {
    private static final Logger log = LoggerFactory.getLogger(AdminStaffService.class);
    private final UserJpaRepository userRepo;
    private final OrganizationUserJpaRepository orgUserRepo;
    private final ActionLogger actionLogger;

    public AdminStaffService(UserJpaRepository userRepo, OrganizationUserJpaRepository orgUserRepo, ActionLogger actionLogger) {
        this.userRepo = userRepo;
        this.orgUserRepo = orgUserRepo;
        this.actionLogger = actionLogger;
    }

    private String getOrgId() {
        return TenantContext.getCurrentOrganizationId().filter(id -> !id.isBlank()).orElse("default-tenant-id");
    }

    private String getOrgSlug() {
        return TenantContext.getCurrentOrganization().map(Organization::getSlug).filter(s -> !s.isBlank()).orElse("ecole-221");
    }

    public List<Map<String, Object>> getStaffMembers() {
        String orgId = getOrgId();
        String slug = getOrgSlug();
        Set<String> memberIds = new HashSet<>();
        if (orgUserRepo != null) {
            orgUserRepo.findByOrganizationId(orgId).forEach(ou -> memberIds.add(ou.getUserId()));
        }
        boolean isDefault = "default-tenant-id".equals(orgId) || "ecole-221".equals(slug) || "ecole221".equals(slug);

        return userRepo.findAll().stream()
            .filter(u -> u.getRole() != null && !"ETUDIANT".equalsIgnoreCase(u.getRole()))
            .filter(u -> {
                if (memberIds.contains(u.getId())) return true;
                String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                return !isDefault ? email.endsWith("@" + slug + ".sn") : (email.endsWith("@ecole221.sn") || email.endsWith("@gmail.com"));
            })
            .map(this::toMap)
            .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(UserJpaEntity u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("firstName", u.getPrenom() != null ? u.getPrenom() : "");
        m.put("lastName", u.getNom() != null ? u.getNom() : "");
        m.put("email", u.getEmail());
        m.put("phone", u.getTelephone() != null ? u.getTelephone() : "");
        m.put("role", u.getRole());
        m.put("status", "Actif");
        return m;
    }

    @Transactional
    public ResponseEntity<?> createStaffMember(Map<String, Object> body) {
        String email = (String) body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body(Map.of("message", "Email requis"));
        String cleanEmail = email.trim().toLowerCase();
        String orgId = getOrgId();
        String role = (String) body.getOrDefault("role", "SECRETAIRE");
        String phone = (String) body.getOrDefault("phone", "");

        try {
            Optional<UserJpaEntity> opt = userRepo.findByEmail(cleanEmail);
            String userId = opt.map(UserJpaEntity::getId).orElseGet(() -> "EMP-2026-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            UserJpaEntity user = opt.orElseGet(UserJpaEntity::new);
            user.setId(userId);
            user.setPrenom((String) body.getOrDefault("firstName", ""));
            user.setNom((String) body.getOrDefault("lastName", ""));
            user.setEmail(cleanEmail);
            user.setRole(role);
            user.setMatricule(userId);
            if (!phone.isBlank() && (opt.isPresent() || userRepo.findByTelephone(phone.trim()).isEmpty())) {
                user.setTelephone(phone.trim());
            }
            userRepo.save(user);

            if (orgUserRepo != null && orgUserRepo.findByUserIdAndOrganizationId(userId, orgId).isEmpty()) {
                OrganizationUserJpaEntity ou = new OrganizationUserJpaEntity();
                ou.setId(UUID.randomUUID().toString());
                ou.setUserId(userId);
                ou.setOrganizationId(orgId);
                ou.setRole(role);
                ou.setStatus("ACTIVE");
                ou.setScopeType("ORGANIZATION");
                orgUserRepo.save(ou);
            }
            actionLogger.logAdminAction("CREATE_STAFF", "ADMIN", userId, "Collaborateur: " + cleanEmail);
            Map<String, Object> res = new HashMap<>(body);
            res.put("id", userId);
            res.put("email", cleanEmail);
            res.put("status", "Actif");
            return ResponseEntity.ok(res);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Conflit collaborateur {}: {}", cleanEmail, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Cet email ou téléphone est déjà utilisé."));
        }
    }
}

