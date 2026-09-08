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
    private final List<Map<String, Object>> scanLogs = new CopyOnWriteArrayList<>();

    public SecurityService(AdminRepositoryPort adminRepository,
                          SecurityRepositoryPort securityRepositoryPort,
                          VigilProfileJpaRepository vigilProfileRepository) {
        this.adminRepository = adminRepository;
        this.securityRepositoryPort = securityRepositoryPort;
        this.vigilProfileRepository = vigilProfileRepository;
        
        // Seed default scan log
        Map<String, Object> log = new HashMap<>();
        log.put("id", "scan-1");
        log.put("studentName", "Mamadou Sall");
        log.put("status", "ENTREE");
        log.put("timestamp", LocalDateTime.now().minusMinutes(10).toString());
        log.put("statutFrais", "En Règle");
        log.put("accessGranted", true);
        log.put("message", "Accès autorisé. Bienvenue à l'École 221.");
        scanLogs.add(log);
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
        status.put("guard", assignment.getOrDefault("guard", "Agent Mamadou Diallo"));
        status.put("totalScansToday", scanLogs.size());
        status.put("status", "ACTIVE");
        return status;
    }

    @Override
    public Map<String, Object> processBadgeScan(String badgeId) {
        // Resolve student details based on badgeId
        String studentId = "std-001";
        String studentName = "Mamadou Sall";
        
        if (badgeId.contains("002") || badgeId.equals("badge-std-002")) {
            studentId = "std-002";
            studentName = "Awa Diop";
        }

        StatutFrais feesStatus = adminRepository.getStudentStatus(studentId);
        boolean accessGranted = feesStatus == StatutFrais.EN_REGLE;
        String statusText = accessGranted ? "ENTREE" : "BLOQUE";
        String msg = accessGranted 
            ? "Accès autorisé. Bienvenue à l'École 221." 
            : "Accès refusé. Veuillez régulariser votre situation financière auprès de la comptabilité.";

        Map<String, Object> log = new HashMap<>();
        log.put("id", "scan-" + UUID.randomUUID().toString().substring(0, 5));
        log.put("studentName", studentName);
        log.put("status", statusText);
        log.put("timestamp", LocalDateTime.now().toString());
        log.put("statutFrais", feesStatus.getDisplayName());
        log.put("accessGranted", accessGranted);
        log.put("message", msg);
        
        scanLogs.add(0, log);
        adminRepository.addRealtimeLog("Scannage de badge '" + badgeId + "' pour '" + studentName + "' -> " + (accessGranted ? "AUTORISÉ" : "REFUSÉ"));

        return log;
    }

    @Override
    public List<Map<String, Object>> getVigilCheckIns() {
        return scanLogs;
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
        
        // Return default profile if not found
        Map<String, Object> defaultProfile = new HashMap<>();
        defaultProfile.put("id", "VIG-221");
        defaultProfile.put("nom", "Diallo");
        defaultProfile.put("prenom", "Mamadou");
        defaultProfile.put("badgeId", "BADGE-VIG-001");
        defaultProfile.put("equipe", "Jour");
        defaultProfile.put("derniereConnexion", LocalDateTime.now().toString());
        defaultProfile.put("statut", "Opérationnel");
        defaultProfile.put("zone", "Portail Principal");
        return defaultProfile;
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
