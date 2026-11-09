package ecole_221.sn.ecole.modules.security.application.service;

import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;
import ecole_221.sn.ecole.modules.security.application.port.dto.VigilProfileDto;
import ecole_221.sn.ecole.modules.security.application.port.usecase.SecurityUseCase;
import ecole_221.sn.ecole.modules.security.domain.model.VigilProfile;
import ecole_221.sn.ecole.modules.security.domain.port.repository.SecurityRepositoryPort;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.VigilProfileJpaRepository;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.VigilProfileJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SecurityService implements SecurityUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    private final AdminRepositoryPort adminRepository;
    private final SecurityRepositoryPort securityRepositoryPort;
    private final VigilProfileJpaRepository vigilProfileRepository;
    private final Map<String, List<Map<String, Object>>> passagesParEcole = new java.util.concurrent.ConcurrentHashMap<>();

    private final ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort comptes;
    private final ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances;

    @org.springframework.beans.factory.annotation.Autowired
    public SecurityService(AdminRepositoryPort adminRepository,
                          SecurityRepositoryPort securityRepositoryPort,
                          VigilProfileJpaRepository vigilProfileRepository,
                          ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort comptes,
                          ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances) {
        this.adminRepository = adminRepository;
        this.securityRepositoryPort = securityRepositoryPort;
        this.vigilProfileRepository = vigilProfileRepository;
        this.comptes = comptes;
        this.appartenances = appartenances;
        // APP-SEC-18 : plus de passage « Mamadou Sall » inventé au démarrage
    }

    /** Journal des passages de l'école courante (APP-SEC-18 : avant, une seule liste pour toutes les écoles). */
    private List<Map<String, Object>> journalEcole() {
        String ecole = ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId().orElse("sans-ecole");
        return passagesParEcole.computeIfAbsent(ecole, k -> new CopyOnWriteArrayList<>());
    }

    // ==================== GESTION DES PROFILS DE VIGILANCE ====================

    @Override
    public List<VigilProfileDto> getAllVigilProfiles() {
        logger.info("Récupération de tous les profils de vigilance");
        return securityRepositoryPort.findAllVigilProfiles().stream()
            .map(this::toVigilProfileDto)
            .collect(Collectors.toList());
    }

    @Override
    public VigilProfileDto getVigilProfileById(String id) {
        logger.info("Récupération du profil de vigilance: {}", id);
        return securityRepositoryPort.findVigilProfileById(id)
            .map(this::toVigilProfileDto)
            .orElse(null);
    }

    @Override
    public List<VigilProfileDto> getVigilProfilesByUserId(String userId) {
        logger.info("Récupération des profils de vigilance par utilisateur: {}", userId);
        return securityRepositoryPort.findVigilProfilesByUserId(userId).stream()
            .map(this::toVigilProfileDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<VigilProfileDto> getVigilProfilesByRole(String role) {
        logger.info("Récupération des profils de vigilance par rôle: {}", role);
        return securityRepositoryPort.findVigilProfilesByRole(role).stream()
            .map(this::toVigilProfileDto)
            .collect(Collectors.toList());
    }

    @Override
    public VigilProfileDto createVigilProfile(VigilProfileDto vigilProfileDto) {
        logger.info("Création du profil de vigilance: {}", vigilProfileDto.name());
        VigilProfile vigilProfile = toVigilProfile(vigilProfileDto);
        VigilProfile savedProfile = securityRepositoryPort.saveVigilProfile(vigilProfile);
        return toVigilProfileDto(savedProfile);
    }

    @Override
    public VigilProfileDto updateVigilProfile(String id, VigilProfileDto vigilProfileDto) {
        logger.info("Mise à jour du profil de vigilance: {}", id);
        VigilProfile vigilProfile = toVigilProfile(vigilProfileDto);
        // Reconstruction avec l'ID existant via Factory
        VigilProfile updatedProfile = securityRepositoryPort.saveVigilProfile(
            ecole_221.sn.ecole.modules.security.domain.model.VigilProfileFactory.reconstitute(
                id,
                vigilProfileDto.name(),
                vigilProfileDto.role(),
                vigilProfileDto.userId(),
                vigilProfileDto.status(),
                vigilProfileDto.createdAt()
            )
        );
        return toVigilProfileDto(updatedProfile);
    }

    @Override
    public void deleteVigilProfile(String id) {
        logger.info("Suppression du profil de vigilance: {}", id);
        securityRepositoryPort.deleteVigilProfile(id);
    }

    // ==================== OPÉRATIONS DE SÉCURITÉ ====================

    @Override
    public Map<String, Object> getCheckpointStatus() {
        Map<String, String> assignment = adminRepository.getGateAssignment();
        Map<String, Object> status = new HashMap<>();
        status.put("gate", assignment.getOrDefault("gate", "Porte Principale"));
        status.put("guard", assignment.getOrDefault("guard", "Non assigné"));   // plus de gardien inventé
        status.put("totalScansToday", journalEcole().size());
        status.put("status", "ACTIVE");
        return status;
    }

    @Override
    public Map<String, Object> processBadgeScan(String badgeId) {
        // APP-SEC-18 : le badge est rattaché à un compte RÉEL (identifiant ou matricule), membre ACTIF de
        // l'école courante. Avant : tout badge était attribué à l'élève fictif « Mamadou Sall » (ou
        // « Awa Diop ») et l'accès décidé d'après sa situation — un badge inconnu pouvait être accepté.
        String brut = badgeId == null ? "" : badgeId.trim();
        String sansPrefixe = brut.startsWith("badge-") ? brut.substring(6) : brut;
        Optional<ecole_221.sn.ecole.modules.auth.domain.model.User> titulaire = comptes.findById(brut)
                .or(() -> comptes.findById(sansPrefixe))
                .or(() -> comptes.findByMatricule(brut))
                .or(() -> comptes.findByMatricule(sansPrefixe));
        String ecole = ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId().orElse(null);

        boolean accessGranted;
        String nom;
        String statutFrais = null;
        String msg;
        if (titulaire.isEmpty()) {
            accessGranted = false;
            nom = "Inconnu";
            msg = "Accès refusé : badge non reconnu.";
        } else {
            var u = titulaire.get();
            nom = ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim();
            boolean membre = ecole != null && appartenances.findByUserIdAndOrganizationId(u.getId(), ecole)
                    .map(m -> m.isActive() && !m.isExpired()).orElse(false);
            if (!membre) {
                accessGranted = false;
                msg = "Accès refusé : ce badge n'appartient pas à cet établissement.";
            } else if (u.getRole() != null && "ETUDIANT".equals(u.getRole().value())) {
                StatutFrais frais = adminRepository.getStudentStatus(u.getId());
                statutFrais = frais.getDisplayName();
                accessGranted = frais == StatutFrais.EN_REGLE;
                msg = accessGranted ? "Accès autorisé."
                        : "Accès refusé. Veuillez régulariser votre situation financière auprès de la comptabilité.";
            } else {
                accessGranted = true;   // personnel de l'école
                msg = "Accès autorisé (personnel).";
            }
        }

        Map<String, Object> log = new HashMap<>();
        log.put("id", "scan-" + UUID.randomUUID().toString().substring(0, 8));
        log.put("studentName", nom);
        log.put("status", accessGranted ? "ENTREE" : "BLOQUE");
        log.put("timestamp", LocalDateTime.now().toString());
        log.put("statutFrais", statutFrais);
        log.put("accessGranted", accessGranted);
        log.put("message", msg);
        journalEcole().add(0, log);
        adminRepository.addRealtimeLog("Scannage de badge pour '" + nom + "' -> " + (accessGranted ? "AUTORISÉ" : "REFUSÉ"));
        return log;
    }

    @Override
    public List<Map<String, Object>> getVigilCheckIns() {
        return journalEcole();
    }

    @Override
    public Map<String, Object> getVigilProfile(String vigilId) {
        Optional<VigilProfileJpaEntity> profile = vigilProfileRepository.findById(vigilId);
        if (profile.isPresent()) {
            VigilProfileJpaEntity entity = profile.get();
            Map<String, Object> map = new HashMap<>();
            map.put("id", entity.getId());
            map.put("nom", entity.getNom());
            map.put("prenom", entity.getPrenom());
            map.put("badgeId", entity.getBadgeId());
            map.put("equipe", entity.getEquipe());
            map.put("derniereConnexion", entity.getDerniereConnexion());
            map.put("statut", entity.getStatut());
            map.put("zone", entity.getZone());
            return map;
        }
        
        // Profil non configuré : réponse explicite (avant : vigile « Mamadou Diallo » inventé)
        Map<String, Object> nonConfigure = new HashMap<>();
        nonConfigure.put("id", vigilId);
        nonConfigure.put("statut", "non_configuré");
        return nonConfigure;
    }

    // ==================== MAPPERS ====================

    private VigilProfileDto toVigilProfileDto(VigilProfile vigilProfile) {
        return new VigilProfileDto(
            vigilProfile.getId(),
            vigilProfile.getName(),
            vigilProfile.getRole(),
            vigilProfile.getUserId(),
            vigilProfile.getStatus(),
            vigilProfile.getCreatedAt()
        );
    }

    private VigilProfile toVigilProfile(VigilProfileDto dto) {
        return ecole_221.sn.ecole.modules.security.domain.model.VigilProfileFactory.reconstitute(
            dto.id(),
            dto.name(),
            dto.role(),
            dto.userId(),
            dto.status(),
            dto.createdAt()
        );
    }
}
