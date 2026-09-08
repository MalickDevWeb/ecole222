package ecole_221.sn.ecole.modules.planning.application.service;

import ecole_221.sn.ecole.modules.planning.application.port.dto.*;
import ecole_221.sn.ecole.modules.planning.application.port.exception.RoomNotFoundException;
import ecole_221.sn.ecole.modules.planning.application.port.exception.SlotNotFoundException;
import ecole_221.sn.ecole.modules.planning.application.port.usecase.PlanningUseCase;
import ecole_221.sn.ecole.modules.planning.domain.model.*;
import ecole_221.sn.ecole.modules.planning.domain.port.repository.PlanningRepositoryPort;
import ecole_221.sn.ecole.modules.planning.domain.valueobject.SlotType;
import ecole_221.sn.ecole.partage.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service d'application pour le module Planning.
 * Implémente les cas d'utilisation définis dans PlanningUseCase.
 */
@Service
public class PlanningService implements PlanningUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(PlanningService.class);
    private final PlanningRepositoryPort planningRepositoryPort;
    
    public PlanningService(PlanningRepositoryPort planningRepositoryPort) {
        this.planningRepositoryPort = planningRepositoryPort;
    }
    
    // ==================== GESTION DES SALLES ====================
    
    @Override
    public List<RoomDto> getAllRooms() {
        logger.info("Récupération de toutes les salles");
        return planningRepositoryPort.findAllRooms().stream()
            .map(this::toRoomDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public RoomDto getRoomById(String roomId) {
        logger.info("Récupération de la salle: {}", roomId);
        return planningRepositoryPort.findRoomById(roomId)
            .map(this::toRoomDto)
            .orElse(null);
    }
    
    @Override
    public RoomDto saveRoom(RoomDto roomDto) {
        logger.info("Sauvegarde de la salle: {}", roomDto.id());
        Room room = toRoom(roomDto);
        Room savedRoom = planningRepositoryPort.saveRoom(room);
        return toRoomDto(savedRoom);
    }
    
    @Override
    public void deleteRoom(String roomId) {
        logger.info("Suppression de la salle: {}", roomId);
        planningRepositoryPort.deleteRoom(roomId);
    }
    
    // ==================== GESTION DES CRÉNEAUX HORAIRES ====================
    
    @Override
    public List<SlotDto> getAllSlots() {
        logger.info("Récupération de tous les créneaux horaires");
        return planningRepositoryPort.findAllSlots().stream()
            .map(this::toSlotDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public SlotDto getSlotById(String slotId) {
        logger.info("Récupération du créneau horaire: {}", slotId);
        return planningRepositoryPort.findSlotById(slotId)
            .map(this::toSlotDto)
            .orElse(null);
    }
    
    @Override
    public SlotDto saveSlot(SlotDto slotDto) {
        logger.info("Sauvegarde du créneau horaire: {}", slotDto.id());
        Slot slot = toSlot(slotDto);
        Slot savedSlot = planningRepositoryPort.saveSlot(slot);
        return toSlotDto(savedSlot);
    }
    
    @Override
    public void deleteSlot(String slotId) {
        logger.info("Suppression du créneau horaire: {}", slotId);
        planningRepositoryPort.deleteSlot(slotId);
    }
    
    // ==================== GESTION DES FILIÈRES ====================
    
    @Override
    public List<FiliereDto> getAllFilieres() {
        logger.info("Récupération de toutes les filières");
        return planningRepositoryPort.findAllFilieres().stream()
            .map(this::toFiliereDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public FiliereDto getFiliereById(String filiereId) {
        logger.info("Récupération de la filière: {}", filiereId);
        return planningRepositoryPort.findFiliereById(filiereId)
            .map(this::toFiliereDto)
            .orElse(null);
    }
    
    @Override
    public FiliereDto saveFiliere(FiliereDto filiereDto) {
        logger.info("Sauvegarde de la filière: {}", filiereDto.id());
        Filiere filiere = toFiliere(filiereDto);
        Filiere savedFiliere = planningRepositoryPort.saveFiliere(filiere);
        return toFiliereDto(savedFiliere);
    }
    
    @Override
    public void deleteFiliere(String filiereId) {
        logger.info("Suppression de la filière: {}", filiereId);
        planningRepositoryPort.deleteFiliere(filiereId);
    }
    
    // ==================== GESTION DES CLASSES ====================
    
    @Override
    public PageResult<ClasseDto> getAllClasses(int page, int size) {
        logger.info("Récupération de toutes les classes (page={}, size={})", page, size);
        List<Classe> classes = planningRepositoryPort.findAllClasses();
        
        // Pagination simplifiée pour l'exemple
        int start = page * size;
        int end = Math.min(start + size, classes.size());
        List<Classe> pageContent = classes.subList(start, end);
        
        List<ClasseDto> dtos = pageContent.stream()
            .map(this::toClasseDto)
            .collect(Collectors.toList());
        
        return new PageResult<>(dtos, page, size, classes.size(), (int) Math.ceil((double) classes.size() / size));
    }
    
    @Override
    public ClasseDto getClasseById(String classeId) {
        logger.info("Récupération de la classe: {}", classeId);
        return planningRepositoryPort.findClasseById(classeId)
            .map(this::toClasseDto)
            .orElse(null);
    }
    
    @Override
    public ClasseDto saveClasse(ClasseDto classeDto) {
        logger.info("Sauvegarde de la classe: {}", classeDto.id());
        Classe classe = toClasse(classeDto);
        Classe savedClasse = planningRepositoryPort.saveClasse(classe);
        return toClasseDto(savedClasse);
    }
    
    @Override
    public void deleteClasse(String classeId) {
        logger.info("Suppression de la classe: {}", classeId);
        planningRepositoryPort.deleteClasse(classeId);
    }
    
    @Override
    public List<ClasseDto> getClassesByFiliere(String filiereId) {
        logger.info("Récupération des classes de la filière: {}", filiereId);
        return planningRepositoryPort.findClassesByFiliere(filiereId).stream()
            .map(this::toClasseDto)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS ====================
    
    private RoomDto toRoomDto(Room room) {
        return new RoomDto(
            room.getId(),
            room.getName(),
            room.getCapacity(),
            room.getEquipment(),
            room.getLocation()
        );
    }
    
    private Room toRoom(RoomDto dto) {
        return RoomFactory.reconstruct(
            dto.id(),
            dto.name(),
            dto.capacity(),
            dto.equipment(),
            dto.location()
        );
    }
    
    private SlotDto toSlotDto(Slot slot) {
        return new SlotDto(
            slot.getId(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getDayOfWeek(),
            slot.getType().value()
        );
    }
    
    private Slot toSlot(SlotDto dto) {
        return SlotFactory.reconstruct(
            dto.id(),
            dto.startTime(),
            dto.endTime(),
            dto.dayOfWeek(),
            new SlotType(dto.type())
        );
    }
    
    private FiliereDto toFiliereDto(Filiere filiere) {
        return new FiliereDto(
            filiere.getId(),
            filiere.getName(),
            filiere.getCode(),
            filiere.getDescription(),
            filiere.getLevel()
        );
    }
    
    private Filiere toFiliere(FiliereDto dto) {
        return FiliereFactory.reconstruct(
            dto.id(),
            dto.name(),
            dto.code(),
            dto.description(),
            dto.level()
        );
    }
    
    private ClasseDto toClasseDto(Classe classe) {
        return new ClasseDto(
            classe.getId(),
            classe.getName(),
            classe.getCode(),
            classe.getFiliereId(),
            classe.getLevel(),
            classe.getCapacity(),
            classe.getCurrentSize()
        );
    }
    
    private Classe toClasse(ClasseDto dto) {
        return ClasseFactory.reconstruct(
            dto.id(),
            dto.name(),
            dto.code(),
            dto.filiereId(),
            dto.level(),
            dto.capacity(),
            dto.currentSize()
        );
    }
}