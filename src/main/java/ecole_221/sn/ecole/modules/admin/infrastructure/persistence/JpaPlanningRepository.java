package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import ecole_221.sn.ecole.modules.admin.domain.model.*;
import ecole_221.sn.ecole.modules.admin.domain.port.repository.PlanningRepositoryPort;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.FiliereJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.FiliereJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.RoomJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.RoomJpaRepository;
import ecole_221.sn.ecole.modules.corbeille.application.CorbeilleService;
import ecole_221.sn.ecole.modules.corbeille.application.TypeCorbeille;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Planning de l'administration, persisté et cloisonné par école.
 * Remplace InMemoryPlanningRepository (salles « Salle 101 », filière « Génie Logiciel », classe « L3 GL »
 * et créneaux inventés, PUT gardés en mémoire seulement et communs à toutes les écoles).
 *
 * Salles, filières et classes : mêmes tables JPA que le module planning (rooms, filieres, classes — @TenantId),
 * donc /admin/planning/* et /planning/* lisent les mêmes données. Créneaux : table creneaux_planning (@TenantId).
 *
 * Les PUT remplacent la liste de l'école courante : les lignes absentes sont supprimées, les autres sont
 * mises à jour en conservant les colonnes que l'écran admin ne gère pas (emplacement, niveau de filière…).
 * findAll() étant filtré par @TenantId, une école ne voit ni ne supprime jamais les lignes d'une autre.
 *
 * Corbeille (V21) : une ligne absente du PUT est mise à la corbeille (restaurable 30 jours), jamais effacée ;
 * un PUT qui renverrait l'identifiant d'une ligne de la corbeille est refusé (409 : la restaurer plutôt).
 */
@Repository
public class JpaPlanningRepository implements PlanningRepositoryPort {

    private final RoomJpaRepository rooms;
    private final FiliereJpaRepository filieres;
    private final ClasseJpaRepository classes;
    private final CreneauPlanningJpaRepository creneaux;
    private final CorbeilleService corbeille;

    public JpaPlanningRepository(RoomJpaRepository rooms, FiliereJpaRepository filieres,
                                 ClasseJpaRepository classes, CreneauPlanningJpaRepository creneaux, CorbeilleService corbeille) {
        this.corbeille = corbeille;
        this.rooms = rooms;
        this.filieres = filieres;
        this.classes = classes;
        this.creneaux = creneaux;
    }

    // ==================== SALLES ====================

    @Override
    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return rooms.findAll().stream()
                .map(e -> RoomFactory.reconstruct(e.getId(), texte(e.getName()), Math.max(0, e.getCapacity()), equipements(e.getEquipment())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<java.util.Map<String, Object>> saveRooms(List<Room> nouvelles) {
        Map<String, RoomJpaEntity> existantes = parId(rooms.findAll(), RoomJpaEntity::getId);
        Set<String> gardees = new HashSet<>();
        for (Room r : nouvelles) {
            if (!existantes.containsKey(r.getId())) corbeille.exigerHorsCorbeille(TypeCorbeille.SALLE, r.getId());
            RoomJpaEntity e = existantes.getOrDefault(r.getId(), new RoomJpaEntity());
            e.setId(r.getId());
            e.setName(r.getName());
            e.setCapacity(r.getCapacity());
            e.setEquipment(r.getEquipment().isEmpty() ? null : String.join(",", r.getEquipment()));
            rooms.save(e);
            gardees.add(r.getId());
        }
        return mettreALaCorbeille(TypeCorbeille.SALLE, existantes.keySet().stream().filter(id -> !gardees.contains(id)).toList());
    }

    // ==================== FILIÈRES ====================

    @Override
    @Transactional(readOnly = true)
    public List<Filiere> getAllFilieres() {
        return filieres.findAll().stream()
                .map(e -> FiliereFactory.reconstruct(e.getId(), texte(e.getCode()), texte(e.getName()), e.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<java.util.Map<String, Object>> saveFilieres(List<Filiere> nouvelles) {
        Map<String, FiliereJpaEntity> existantes = parId(filieres.findAll(), FiliereJpaEntity::getId);
        Set<String> gardees = new HashSet<>();
        for (Filiere f : nouvelles) {
            if (!existantes.containsKey(f.getId())) corbeille.exigerHorsCorbeille(TypeCorbeille.FILIERE, f.getId());
            FiliereJpaEntity e = existantes.getOrDefault(f.getId(), new FiliereJpaEntity());
            e.setId(f.getId());
            e.setCode(f.getCode());
            e.setName(f.getName());
            e.setDescription(f.getDescription());
            filieres.save(e);
            gardees.add(f.getId());
        }
        return mettreALaCorbeille(TypeCorbeille.FILIERE, existantes.keySet().stream().filter(id -> !gardees.contains(id)).toList());
    }

    // ==================== CLASSES ====================

    @Override
    @Transactional(readOnly = true)
    public List<Classe> getAllClasses() {
        return classes.findAll().stream()
                .map(e -> ClasseFactory.reconstruct(e.getId(), texte(e.getName()), texte(e.getFiliereId()),
                        e.getLevel(), e.getCapacity() > 0 ? e.getCapacity() : null, e.getFraisAnnuels()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<java.util.Map<String, Object>> saveClasses(List<Classe> nouvelles) {
        Map<String, ClasseJpaEntity> existantes = parId(classes.findAll(), ClasseJpaEntity::getId);
        Set<String> gardees = new HashSet<>();
        for (Classe c : nouvelles) {
            if (!existantes.containsKey(c.getId())) corbeille.exigerHorsCorbeille(TypeCorbeille.CLASSE, c.getId());
            ClasseJpaEntity e = existantes.getOrDefault(c.getId(), new ClasseJpaEntity());
            e.setId(c.getId());
            e.setName(c.getName());
            e.setFiliereId(c.getFiliereId());
            if (c.getLevel() != null) e.setLevel(c.getLevel());
            if (c.getCapacity() != null) e.setCapacity(c.getCapacity());
            e.setFraisAnnuels(c.getPrice());
            classes.save(e);
            gardees.add(c.getId());
        }
        return mettreALaCorbeille(TypeCorbeille.CLASSE, existantes.keySet().stream().filter(id -> !gardees.contains(id)).toList());
    }

    // ==================== CRÉNEAUX ====================

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlot> getAllSlots() {
        return creneaux.findAll().stream()
                .map(e -> TimeSlotFactory.reconstruct(e.getId(), texte(e.getJour()), texte(e.getHeureDebut()), texte(e.getHeureFin()),
                        e.getNom(), e.getType(), e.getProfesseur(), e.getSalle(), e.getDescription(), texte(e.getClasseId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<java.util.Map<String, Object>> saveSlots(List<TimeSlot> nouveaux) {
        Map<String, CreneauPlanningJpaEntity> existants = parId(creneaux.findAll(), CreneauPlanningJpaEntity::getId);
        Set<String> gardes = new HashSet<>();
        for (TimeSlot s : nouveaux) {
            if (!existants.containsKey(s.getId())) corbeille.exigerHorsCorbeille(TypeCorbeille.CRENEAU, s.getId());
            CreneauPlanningJpaEntity e = existants.getOrDefault(s.getId(), new CreneauPlanningJpaEntity());
            e.setId(s.getId());
            e.setJour(s.getDay());
            e.setHeureDebut(s.getHeureDebut());
            e.setHeureFin(s.getHeureFin());
            e.setNom(s.getNom());
            e.setType(s.getType());
            e.setProfesseur(s.getProfesseur());
            e.setSalle(s.getSalle());
            e.setDescription(s.getDescription());
            e.setClasseId(s.getClasseId());
            creneaux.save(e);
            gardes.add(s.getId());
        }
        return mettreALaCorbeille(TypeCorbeille.CRENEAU, existants.keySet().stream().filter(id -> !gardes.contains(id)).toList());
    }

    // ==================== OUTILS ====================

    /** Lignes absentes du PUT : mises à la corbeille, renvoyées pour que l'écran l'annonce. */
    private List<Map<String, Object>> mettreALaCorbeille(TypeCorbeille type, List<String> ids) {
        List<Map<String, Object>> retirees = new java.util.ArrayList<>();
        for (String id : ids) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("type", type.code());
            e.put("id", id);
            e.put("libelle", corbeille.supprimer(type, id));
            retirees.add(e);
        }
        return retirees;
    }

    private static <T> Map<String, T> parId(List<T> lignes, Function<T, String> id) {
        Map<String, T> map = new LinkedHashMap<>();
        for (T l : lignes) map.put(id.apply(l), l);
        return map;
    }

    /** Valeur absente rendue vide (jamais remplacée par une valeur inventée). */
    private static String texte(String v) { return v == null ? "" : v; }

    private static List<String> equipements(String brut) {
        if (brut == null || brut.isBlank()) return List.of();
        return Arrays.stream(brut.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }
}
