package ecole_221.sn.ecole.modules.planning.infrastructure.persistence;

import ecole_221.sn.ecole.modules.planning.domain.exception.ClasseNotFoundException;
import ecole_221.sn.ecole.modules.planning.domain.exception.FiliereNotFoundException;
import ecole_221.sn.ecole.modules.planning.domain.exception.RoomNotFoundException;
import ecole_221.sn.ecole.modules.planning.domain.exception.SlotNotFoundException;
import ecole_221.sn.ecole.modules.planning.domain.model.*;
import ecole_221.sn.ecole.modules.planning.domain.port.repository.PlanningRepositoryPort;
import ecole_221.sn.ecole.modules.planning.domain.valueobject.SlotType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Planning.
 * Implémente PlanningRepositoryPort en utilisant les repositories JPA existants.
 */
@Component
public class PlanningRepositoryAdapter implements PlanningRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(PlanningRepositoryAdapter.class);
    
    private final RoomJpaRepository roomJpaRepository;
    private final SlotJpaRepository slotJpaRepository;
    private final FiliereJpaRepository filiereJpaRepository;
    private final ClasseJpaRepository classeJpaRepository;
    
    public PlanningRepositoryAdapter(
            RoomJpaRepository roomJpaRepository,
            SlotJpaRepository slotJpaRepository,
            FiliereJpaRepository filiereJpaRepository,
            ClasseJpaRepository classeJpaRepository) {
        this.roomJpaRepository = roomJpaRepository;
        this.slotJpaRepository = slotJpaRepository;
        this.filiereJpaRepository = filiereJpaRepository;
        this.classeJpaRepository = classeJpaRepository;
    }
    
    // ==================== REPOSITORY SALLES ====================
    
    @Override
    public Room saveRoom(Room room) {
        logger.debug("Sauvegarde de la salle: {}", room.getId());
        RoomJpaEntity entity = toRoomEntity(room);
        RoomJpaEntity savedEntity = roomJpaRepository.save(entity);
        return toRoom(savedEntity);
    }
    
    @Override
    public Optional<Room> findRoomById(String roomId) {
        logger.debug("Récupération de la salle: {}", roomId);
        return roomJpaRepository.findById(roomId)
            .map(this::toRoom);
    }
    
    @Override
    public List<Room> findAllRooms() {
        logger.debug("Récupération de toutes les salles depuis JPA");
        return roomJpaRepository.findAll().stream()
            .map(this::toRoom)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteRoom(String roomId) {
        logger.debug("Suppression de la salle: {}", roomId);
        if (!roomJpaRepository.existsById(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        roomJpaRepository.deleteById(roomId);
    }
    
    @Override
    public List<Room> findRoomsByCapacityMin(int minCapacity) {
        logger.debug("Récupération des salles avec capacité minimale: {}", minCapacity);
        return roomJpaRepository.findByCapacityGreaterThanEqual(minCapacity).stream()
            .map(this::toRoom)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Room> findRoomsByLocation(String location) {
        logger.debug("Récupération des salles par emplacement: {}", location);
        return roomJpaRepository.findByLocation(location).stream()
            .map(this::toRoom)
            .collect(Collectors.toList());
    }
    
    // ==================== REPOSITORY CRÉNEAUX ====================
    
    @Override
    public Slot saveSlot(Slot slot) {
        logger.debug("Sauvegarde du créneau: {}", slot.getId());
        SlotJpaEntity entity = toSlotEntity(slot);
        SlotJpaEntity savedEntity = slotJpaRepository.save(entity);
        return toSlot(savedEntity);
    }
    
    @Override
    public Optional<Slot> findSlotById(String slotId) {
        logger.debug("Récupération du créneau: {}", slotId);
        return slotJpaRepository.findById(slotId)
            .map(this::toSlot);
    }
    
    @Override
    public List<Slot> findAllSlots() {
        logger.debug("Récupération de tous les créneaux depuis JPA");
        return slotJpaRepository.findAll().stream()
            .map(this::toSlot)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteSlot(String slotId) {
        logger.debug("Suppression du créneau: {}", slotId);
        if (!slotJpaRepository.existsById(slotId)) {
            throw new SlotNotFoundException(slotId);
        }
        slotJpaRepository.deleteById(slotId);
    }
    
    @Override
    public List<Slot> findSlotsByDayOfWeek(String dayOfWeek) {
        logger.debug("Récupération des créneaux par jour: {}", dayOfWeek);
        return slotJpaRepository.findByDayOfWeek(dayOfWeek).stream()
            .map(this::toSlot)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Slot> findSlotsByType(SlotType type) {
        logger.debug("Récupération des créneaux par type: {}", type);
        return slotJpaRepository.findByType(type.value()).stream()
            .map(this::toSlot)
            .collect(Collectors.toList());
    }
    
    // ==================== REPOSITORY FILIÈRES ====================
    
    @Override
    public Filiere saveFiliere(Filiere filiere) {
        logger.debug("Sauvegarde de la filière: {}", filiere.getId());
        FiliereJpaEntity entity = toFiliereEntity(filiere);
        FiliereJpaEntity savedEntity = filiereJpaRepository.save(entity);
        return toFiliere(savedEntity);
    }
    
    @Override
    public Optional<Filiere> findFiliereById(String filiereId) {
        logger.debug("Récupération de la filière: {}", filiereId);
        return filiereJpaRepository.findById(filiereId)
            .map(this::toFiliere);
    }
    
    @Override
    public List<Filiere> findAllFilieres() {
        logger.debug("Récupération de toutes les filières depuis JPA");
        return filiereJpaRepository.findAll().stream()
            .map(this::toFiliere)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteFiliere(String filiereId) {
        logger.debug("Suppression de la filière: {}", filiereId);
        if (!filiereJpaRepository.existsById(filiereId)) {
            throw new FiliereNotFoundException(filiereId);
        }
        filiereJpaRepository.deleteById(filiereId);
    }
    
    @Override
    public List<Filiere> findFilieresByLevel(String level) {
        logger.debug("Récupération des filières par niveau: {}", level);
        return filiereJpaRepository.findByLevel(level).stream()
            .map(this::toFiliere)
            .collect(Collectors.toList());
    }
    
    // ==================== REPOSITORY CLASSES ====================
    
    @Override
    public Classe saveClasse(Classe classe) {
        logger.debug("Sauvegarde de la classe: {}", classe.getId());
        ClasseJpaEntity entity = toClasseEntity(classe);
        ClasseJpaEntity savedEntity = classeJpaRepository.save(entity);
        return toClasse(savedEntity);
    }
    
    @Override
    public Optional<Classe> findClasseById(String classeId) {
        logger.debug("Récupération de la classe: {}", classeId);
        return classeJpaRepository.findById(classeId)
            .map(this::toClasse);
    }
    
    @Override
    public List<Classe> findAllClasses() {
        logger.debug("Récupération de toutes les classes depuis JPA");
        return classeJpaRepository.findAll().stream()
            .map(this::toClasse)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteClasse(String classeId) {
        logger.debug("Suppression de la classe: {}", classeId);
        if (!classeJpaRepository.existsById(classeId)) {
            throw new ClasseNotFoundException(classeId);
        }
        classeJpaRepository.deleteById(classeId);
    }
    
    @Override
    public List<Classe> findClassesByFiliere(String filiereId) {
        logger.debug("Récupération des classes de la filière: {}", filiereId);
        return classeJpaRepository.findByFiliereId(filiereId).stream()
            .map(this::toClasse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Classe> findClassesByLevel(String level) {
        logger.debug("Récupération des classes par niveau: {}", level);
        return classeJpaRepository.findByLevel(level).stream()
            .map(this::toClasse)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Room toRoom(RoomJpaEntity entity) {
        return RoomFactory.reconstruct(
            entity.getId(),
            entity.getName(),
            entity.getCapacity(),
            entity.getEquipment(),
            entity.getLocation()
        );
    }
    
    private RoomJpaEntity toRoomEntity(Room room) {
        RoomJpaEntity entity = new RoomJpaEntity();
        entity.setId(room.getId());
        entity.setName(room.getName());
        entity.setCapacity(room.getCapacity());
        entity.setEquipment(room.getEquipment());
        entity.setLocation(room.getLocation());
        return entity;
    }
    
    private Slot toSlot(SlotJpaEntity entity) {
        return SlotFactory.reconstruct(
            entity.getId(),
            entity.getStartTime(),
            entity.getEndTime(),
            entity.getDayOfWeek(),
            new SlotType(entity.getType())
        );
    }
    
    private SlotJpaEntity toSlotEntity(Slot slot) {
        SlotJpaEntity entity = new SlotJpaEntity();
        entity.setId(slot.getId());
        entity.setStartTime(slot.getStartTime());
        entity.setEndTime(slot.getEndTime());
        entity.setDayOfWeek(slot.getDayOfWeek());
        entity.setType(slot.getType().value());
        return entity;
    }
    
    private Filiere toFiliere(FiliereJpaEntity entity) {
        return FiliereFactory.reconstruct(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.getDescription(),
            entity.getLevel()
        );
    }
    
    private FiliereJpaEntity toFiliereEntity(Filiere filiere) {
        FiliereJpaEntity entity = new FiliereJpaEntity();
        entity.setId(filiere.getId());
        entity.setName(filiere.getName());
        entity.setCode(filiere.getCode());
        entity.setDescription(filiere.getDescription());
        entity.setLevel(filiere.getLevel());
        return entity;
    }
    
    private Classe toClasse(ClasseJpaEntity entity) {
        return ClasseFactory.reconstruct(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.getFiliereId(),
            entity.getLevel(),
            entity.getCapacity(),
            entity.getCurrentSize()
        );
    }
    
    private ClasseJpaEntity toClasseEntity(Classe classe) {
        ClasseJpaEntity entity = new ClasseJpaEntity();
        entity.setId(classe.getId());
        entity.setName(classe.getName());
        entity.setCode(classe.getCode());
        entity.setFiliereId(classe.getFiliereId());
        entity.setLevel(classe.getLevel());
        entity.setCapacity(classe.getCapacity());
        entity.setCurrentSize(classe.getCurrentSize());
        return entity;
    }
}