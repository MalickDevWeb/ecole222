package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.domain.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Conversion des listes envoyées par l'écran « Gestion de l'école » vers le modèle admin.
 * Avant : le corps était désérialisé directement dans les agrégats (sans constructeur Jackson) → le PUT
 * échouait et le front se rabattait sur PUT /planning/* qui répondait 200 sans rien enregistrer.
 * Accepte les noms de champs du front (day/slot/subject/prof/roomName, capacityMax…) comme ceux du back.
 * Champ obligatoire manquant → 400 explicite ; aucune valeur par défaut inventée.
 */
final class PlanningPayloads {

    private PlanningPayloads() {}

    static List<Room> salles(List<Map<String, Object>> corps) {
        List<Room> res = new ArrayList<>();
        for (Map<String, Object> m : lignes(corps)) {
            String nom = obligatoire(m, "le nom de la salle", "name", "nom");
            Integer cap = entier(m, "capacity", "capacite", "capacityMax");
            if (cap != null && cap < 0) throw mauvais("La capacité de la salle « " + nom + " » ne peut pas être négative.");
            res.add(RoomFactory.reconstruct(id(m), nom, cap == null ? 0 : cap, liste(m.get("equipment"))));
        }
        return res;
    }

    static List<Filiere> filieres(List<Map<String, Object>> corps) {
        List<Filiere> res = new ArrayList<>();
        for (Map<String, Object> m : lignes(corps)) {
            String nom = obligatoire(m, "le nom de la filière", "name", "nom");
            String code = texte(m, "code");
            res.add(FiliereFactory.reconstruct(id(m), code == null ? "" : code, nom, texte(m, "description")));
        }
        return res;
    }

    static List<Classe> classes(List<Map<String, Object>> corps) {
        List<Classe> res = new ArrayList<>();
        for (Map<String, Object> m : lignes(corps)) {
            String nom = obligatoire(m, "le nom de la classe", "name", "nom");
            String filiere = texte(m, "filiereId");
            res.add(ClasseFactory.reconstruct(id(m), nom, filiere == null ? "" : filiere,
                    texte(m, "level", "niveau"), entier(m, "capacityMax", "capacity", "capacite")));
        }
        return res;
    }

    static List<TimeSlot> creneaux(List<Map<String, Object>> corps) {
        List<TimeSlot> res = new ArrayList<>();
        for (Map<String, Object> m : lignes(corps)) {
            String jour = obligatoire(m, "le jour du créneau", "day", "jour");
            String debut = texte(m, "heureDebut", "startTime");
            String fin = texte(m, "heureFin", "endTime");
            String plage = texte(m, "slot", "heureStr");
            if ((debut == null || fin == null) && plage != null) {
                String[] p = plage.split("\\s*-\\s*", 2);
                if (debut == null) debut = p[0].trim();
                if (fin == null && p.length > 1) fin = p[1].trim();
            }
            if (debut == null || debut.isEmpty()) throw mauvais("L'heure de début du créneau est obligatoire.");
            String classe = obligatoire(m, "la classe du créneau", "classeId");
            res.add(TimeSlotFactory.reconstruct(id(m), jour, debut, fin == null ? "" : fin,
                    texte(m, "subject", "nom", "matiere"), texte(m, "type"),
                    texte(m, "prof", "professeur"), texte(m, "roomName", "salle"),
                    texte(m, "description"), classe));
        }
        return res;
    }

    // ==================== OUTILS ====================

    private static List<Map<String, Object>> lignes(List<Map<String, Object>> corps) {
        if (corps == null) throw mauvais("Liste attendue dans le corps de la requête.");
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> m : corps) {
            if (m == null) throw mauvais("Élément vide dans la liste.");
            Object id = m.get("id");
            if (id != null && !ids.add(id.toString())) throw mauvais("Identifiant en double : " + id);
        }
        return corps;
    }

    private static String id(Map<String, Object> m) {
        String id = texte(m, "id");
        return id == null ? UUID.randomUUID().toString() : id;
    }

    private static String texte(Map<String, Object> m, String... cles) {
        for (String c : cles) {
            Object v = m.get(c);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return null;
    }

    private static String obligatoire(Map<String, Object> m, String libelle, String... cles) {
        String v = texte(m, cles);
        if (v == null) throw mauvais("Champ obligatoire manquant : " + libelle + ".");
        return v;
    }

    private static Integer entier(Map<String, Object> m, String... cles) {
        String v = texte(m, cles);
        if (v == null) return null;
        try {
            return (int) Math.round(Double.parseDouble(v));
        } catch (NumberFormatException e) {
            throw mauvais("Nombre invalide : " + v);
        }
    }

    private static List<String> liste(Object v) {
        if (v == null) return List.of();
        if (v instanceof Collection<?> c) {
            List<String> l = new ArrayList<>();
            for (Object o : c) if (o != null && !o.toString().isBlank()) l.add(o.toString().trim());
            return l;
        }
        List<String> l = new ArrayList<>();
        for (String s : v.toString().split(",")) if (!s.isBlank()) l.add(s.trim());
        return l;
    }

    private static ResponseStatusException mauvais(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
