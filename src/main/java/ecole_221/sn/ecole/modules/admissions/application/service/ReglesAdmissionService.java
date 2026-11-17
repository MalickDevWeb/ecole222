package ecole_221.sn.ecole.modules.admissions.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

/**
 * Règles d'admission créées par l'école : « SI condition(s) ALORS effet », pour une session ou pour toutes.
 * <p>
 * Les règles sont enregistrées par école (table admissions_portail_elements, nature REGLE, @TenantId)
 * et appliquées par le SERVEUR au dépôt d'un dossier (portail public et parent) :
 * <ul>
 *   <li>EXIGER_PIECE : le dossier doit contenir la pièce (format PDF, image, ou l'un des deux) — 400 sinon ;</li>
 *   <li>BLOQUER : le dépôt est refusé avec le message de l'école — 400 ;</li>
 *   <li>AVERTIR : le dossier est déposé ; le message s'affiche sur le dossier pour l'administration.</li>
 * </ul>
 * Conditions combinées par ET. Une information absente du dossier rend la condition fausse (règle non appliquée).
 */
@Service
public class ReglesAdmissionService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ZoneId DAKAR = ZoneId.of("Africa/Dakar");
    static final int MAX_REGLES = 50;
    static final int MAX_CONDITIONS = 10;

    /** Champ → opérateurs autorisés. */
    static final Map<String, Set<String>> OPERATEURS = Map.of(
            "nationalite", Set.of("egal", "different"),
            "sexe", Set.of("egal"),
            "age", Set.of("inferieur", "superieur", "egal"),
            "serieBac", Set.of("egal", "different"),
            "mentionBac", Set.of("egal", "different"),
            "anneeBac", Set.of("inferieur", "superieur", "egal"),
            "depotParent", Set.of("egal"));
    static final Set<String> EFFETS = Set.of("EXIGER_PIECE", "BLOQUER", "AVERTIR");
    static final Set<String> FORMATS = Set.of("PDF", "IMAGE", "PDF_IMAGE");
    private static final Set<String> PAYS = Set.of(Locale.getISOCountries());
    /** Codes des pièces déjà demandées par les formulaires : une pièce de règle ne peut pas les réutiliser. */
    private static final Set<String> CODES_RESERVES = Set.of("pieceidentite", "diplome", "releves", "photo");

    private final ElementPortailAdmissionJpaRepository elements;

    public ReglesAdmissionService(ElementPortailAdmissionJpaRepository elements) {
        this.elements = elements;
    }

    // ───────────────────────────── Données d'un dossier ─────────────────────────────

    /** Réponses du dossier utiles aux conditions (null = non renseigné). */
    public record Dossier(String session, String nationalite, String sexe, String dateNaissance, String serieBac,
                          String mentionBac, String anneeBac, boolean depotParent) {

        /** Depuis le corps envoyé (portail : clés du dépôt ; parent : clés du formulaire). */
        public static Dossier depuis(Map<String, ?> corps, boolean depotParent) {
            String session = texte(corps.get("campagneId"));
            return new Dossier(session == null || session.length() > 80 ? null : session, texte(corps.get("nationalite")), texte(corps.get("sexe")),
                    texte(corps.get("dateNaissance")), texte(corps.get("serieBac")), texte(corps.get("mentionBac")),
                    texte(corps.get("anneeBac")), depotParent);
        }
    }

    /** Pièce exigée par une règle. formats : PDF, IMAGE ou PDF_IMAGE. */
    public record PieceExigee(String code, String libelle, String formats) {}

    /** Résultat des règles applicables à un dossier. */
    public record Evaluation(List<PieceExigee> piecesExigees, List<String> blocages, List<String> avertissements) {}

    // ───────────────────────────── Administration (CRUD) ─────────────────────────────

    public List<Map<String, Object>> lister() {
        List<Map<String, Object>> regles = new ArrayList<>();
        for (ElementPortailAdmissionJpaEntity e : elements.findByNatureOrderByPositionAsc(ElementPortailAdmissionJpaEntity.REGLE)) {
            Map<String, Object> r = lire(e);
            if (r != null) regles.add(r);
        }
        return regles;
    }

    @Transactional
    public Map<String, Object> creer(Map<String, Object> corps) {
        List<ElementPortailAdmissionJpaEntity> existantes = elements.findByNatureOrderByPositionAsc(ElementPortailAdmissionJpaEntity.REGLE);
        if (existantes.size() >= MAX_REGLES) {
            throw mauvais("Une école peut avoir " + MAX_REGLES + " règles au plus : supprimez-en une avant d'en créer une autre.");
        }
        Map<String, Object> regle = valider(corps);
        String id = "regle-" + UUID.randomUUID().toString().substring(0, 8);
        regle.put("id", id);
        regle.put("creeLe", LocalDateTime.now(DAKAR).toString());
        ElementPortailAdmissionJpaEntity e = new ElementPortailAdmissionJpaEntity();
        e.setId(id);
        e.setNature(ElementPortailAdmissionJpaEntity.REGLE);
        e.setPosition(existantes.stream().mapToInt(ElementPortailAdmissionJpaEntity::getPosition).max().orElse(-1) + 1);
        enregistrer(e, regle);
        return regle;
    }

    @Transactional
    public Map<String, Object> modifier(String id, Map<String, Object> corps) {
        ElementPortailAdmissionJpaEntity e = trouver(id);
        Map<String, Object> avant = lire(e);
        Map<String, Object> regle = valider(corps);
        regle.put("id", id);
        if (avant != null && avant.get("creeLe") != null) regle.put("creeLe", avant.get("creeLe"));
        regle.put("modifieLe", LocalDateTime.now(DAKAR).toString());
        enregistrer(e, regle);
        return regle;
    }

    @Transactional
    public void supprimer(String id) {
        elements.delete(trouver(id));
    }

    private ElementPortailAdmissionJpaEntity trouver(String id) {
        // Filtré par école (@TenantId) : la règle d'une autre école est « introuvable »
        return elements.findById(id == null ? "" : id)
                .filter(e -> ElementPortailAdmissionJpaEntity.REGLE.equals(e.getNature()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Règle introuvable."));
    }

    private void enregistrer(ElementPortailAdmissionJpaEntity e, Map<String, Object> regle) {
        try {
            e.setContenuJson(JSON.writeValueAsString(regle));
        } catch (Exception ex) {
            throw mauvais("Règle illisible.");
        }
        e.setUpdatedAt(LocalDateTime.now(DAKAR));
        elements.save(e);
    }

    private static Map<String, Object> lire(ElementPortailAdmissionJpaEntity e) {
        try {
            return JSON.readValue(e.getContenuJson(), new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            return null;
        }
    }

    // ───────────────────────────── Validation d'une règle ─────────────────────────────

    /** Règle propre (champs connus uniquement) ou 400 avec la raison. */
    static Map<String, Object> valider(Map<String, Object> corps) {
        if (corps == null) throw mauvais("Règle vide.");
        Map<String, Object> r = new LinkedHashMap<>();
        String nom = texte(corps.get("nom"));
        if (nom == null) throw mauvais("Donnez un nom à la règle (ex. : « Passeport pour les candidats étrangers »).");
        if (nom.length() > 120) throw mauvais("Le nom de la règle dépasse 120 caractères.");
        r.put("nom", nom);
        r.put("actif", !Boolean.FALSE.equals(corps.get("actif")));
        String session = texte(corps.get("session"));
        if (session != null && session.length() > 80) throw mauvais("Session inconnue.");
        r.put("session", session);

        Object brutes = corps.get("conditions");
        List<Map<String, Object>> conditions = new ArrayList<>();
        if (brutes != null && !(brutes instanceof List<?>)) throw mauvais("« conditions » doit être une liste.");
        if (brutes instanceof List<?> liste) {
            if (liste.size() > MAX_CONDITIONS) throw mauvais(MAX_CONDITIONS + " conditions au plus par règle.");
            for (Object o : liste) {
                if (!(o instanceof Map<?, ?> c)) throw mauvais("Condition illisible.");
                conditions.add(validerCondition(texte(c.get("champ")), texte(c.get("operateur")), texte(c.get("valeur"))));
            }
        }
        r.put("conditions", conditions);

        if (!(corps.get("effet") instanceof Map<?, ?> effet)) throw mauvais("Choisissez ce que fait la règle (ALORS…).");
        String type = texte(effet.get("type"));
        if (type == null || !EFFETS.contains(type)) throw mauvais("Effet inconnu : exiger une pièce, bloquer le dépôt ou avertir l'administration.");
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("type", type);
        if ("EXIGER_PIECE".equals(type)) {
            String libelle = texte(effet.get("libelle"));
            if (libelle == null) throw mauvais("Indiquez la pièce à exiger (ex. : « Passeport »).");
            if (libelle.length() > 80) throw mauvais("Le nom de la pièce dépasse 80 caractères.");
            String formats = Optional.ofNullable(texte(effet.get("formats"))).orElse("PDF_IMAGE");
            if (!FORMATS.contains(formats)) throw mauvais("Format inconnu : PDF, image, ou PDF ou image.");
            String code = codePiece(libelle);
            if (code.isEmpty()) throw mauvais("Le nom de la pièce doit contenir des lettres ou des chiffres.");
            if (CODES_RESERVES.contains(code.replace("-", ""))) {
                throw mauvais("« " + libelle + " » est déjà demandée par le formulaire : choisissez une autre pièce.");
            }
            e.put("libelle", libelle);
            e.put("formats", formats);
            e.put("code", code);
        } else {
            String message = texte(effet.get("message"));
            if (message == null) throw mauvais("Écrivez le message affiché (au candidat pour un blocage, à l'administration pour un avertissement).");
            if (message.length() > 300) throw mauvais("Le message dépasse 300 caractères.");
            e.put("message", message);
        }
        r.put("effet", e);
        return r;
    }

    private static Map<String, Object> validerCondition(String champ, String operateur, String valeur) {
        if (champ == null || !OPERATEURS.containsKey(champ)) throw mauvais("Condition sur un champ inconnu : « " + champ + " ».");
        if (operateur == null || !OPERATEURS.get(champ).contains(operateur)) {
            throw mauvais("Comparaison impossible pour « " + champ + " » : « " + operateur + " ».");
        }
        if (valeur == null) throw mauvais("Indiquez la valeur de chaque condition.");
        String v = switch (champ) {
            case "nationalite" -> {
                String code = valeur.toUpperCase(Locale.ROOT);
                if (!PAYS.contains(code)) throw mauvais("Pays inconnu : « " + valeur + " ».");
                yield code;
            }
            case "sexe" -> {
                if (!valeur.equals("M") && !valeur.equals("F")) throw mauvais("Sexe : « M » ou « F ».");
                yield valeur;
            }
            case "age" -> String.valueOf(entier(valeur, 0, 120, "L'âge"));
            case "anneeBac" -> String.valueOf(entier(valeur, 1950, 2100, "L'année du bac"));
            case "depotParent" -> {
                if (!valeur.equals("oui") && !valeur.equals("non")) throw mauvais("Dépôt par un parent : « oui » ou « non ».");
                yield valeur;
            }
            default -> {
                if (valeur.length() > 40) throw mauvais("Valeur trop longue pour « " + champ + " ».");
                yield valeur;
            }
        };
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("champ", champ);
        c.put("operateur", operateur);
        c.put("valeur", v);
        return c;
    }

    private static int entier(String valeur, int min, int max, String libelle) {
        try {
            int n = Integer.parseInt(valeur.trim());
            if (n < min || n > max) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException ex) {
            throw mauvais(libelle + " doit être un nombre entre " + min + " et " + max + ".");
        }
    }

    /** « Attestation de stage » → « attestation-de-stage ». */
    static String codePiece(String libelle) {
        String sansAccents = Normalizer.normalize(libelle, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String code = sansAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return code.length() > 40 ? code.substring(0, 40).replaceAll("-+$", "") : code;
    }

    // ───────────────────────────── Évaluation ─────────────────────────────

    /** Règles actives de l'école qui s'appliquent à ce dossier. */
    public Evaluation evaluer(Dossier d) {
        return evaluer(lister(), d, LocalDate.now(DAKAR));
    }

    @SuppressWarnings("unchecked")
    static Evaluation evaluer(List<Map<String, Object>> regles, Dossier d, LocalDate aujourdhui) {
        Map<String, PieceExigee> pieces = new LinkedHashMap<>();
        List<String> blocages = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();
        for (Map<String, Object> r : regles) {
            if (Boolean.FALSE.equals(r.get("actif"))) continue;
            String session = texte(r.get("session"));
            if (session != null && !session.equals(d.session())) continue;
            boolean toutes = true;
            for (Object o : (List<Object>) r.getOrDefault("conditions", List.of())) {
                if (!(o instanceof Map<?, ?> c) || !vraie((Map<String, Object>) c, d, aujourdhui)) {
                    toutes = false;
                    break;
                }
            }
            if (!toutes || !(r.get("effet") instanceof Map<?, ?> effet)) continue;
            String nom = String.valueOf(r.get("nom"));
            switch (String.valueOf(effet.get("type"))) {
                case "EXIGER_PIECE" -> pieces.putIfAbsent(String.valueOf(effet.get("code")),
                        new PieceExigee(String.valueOf(effet.get("code")), String.valueOf(effet.get("libelle")), String.valueOf(effet.get("formats"))));
                case "BLOQUER" -> blocages.add(String.valueOf(effet.get("message")));
                case "AVERTIR" -> avertissements.add(nom + " : " + effet.get("message"));
                default -> { }
            }
        }
        return new Evaluation(List.copyOf(pieces.values()), blocages, avertissements);
    }

    private static boolean vraie(Map<String, Object> c, Dossier d, LocalDate aujourdhui) {
        String champ = String.valueOf(c.get("champ"));
        String op = String.valueOf(c.get("operateur"));
        String attendu = String.valueOf(c.get("valeur"));
        switch (champ) {
            case "age" -> {
                Integer age = age(d.dateNaissance(), aujourdhui);
                return age != null && compare(age, op, Integer.parseInt(attendu));
            }
            case "anneeBac" -> {
                Integer annee = entierOuNull(d.anneeBac());
                return annee != null && compare(annee, op, Integer.parseInt(attendu));
            }
            case "depotParent" -> {
                return ("oui".equals(attendu)) == d.depotParent();
            }
            default -> {
                String valeur = switch (champ) {
                    case "nationalite" -> d.nationalite() == null ? null : d.nationalite().trim().toUpperCase(Locale.ROOT);
                    case "sexe" -> d.sexe();
                    case "serieBac" -> d.serieBac();
                    case "mentionBac" -> d.mentionBac();
                    default -> null;
                };
                // Non renseigné (ou nationalité saisie en texte libre par un ancien formulaire) : condition fausse
                if (valeur == null || ("nationalite".equals(champ) && !PAYS.contains(valeur))) return false;
                boolean egal = normaliser(valeur).equals(normaliser(attendu));
                return "egal".equals(op) == egal;
            }
        }
    }

    private static boolean compare(int valeur, String op, int attendu) {
        return switch (op) {
            case "inferieur" -> valeur < attendu;
            case "superieur" -> valeur > attendu;
            default -> valeur == attendu;
        };
    }

    static Integer age(String dateNaissance, LocalDate aujourdhui) {
        try {
            LocalDate n = LocalDate.parse(dateNaissance == null ? "" : dateNaissance.trim());
            return n.isAfter(aujourdhui) ? null : Period.between(n, aujourdhui).getYears();
        } catch (Exception ex) {
            return null;
        }
    }

    private static Integer entierOuNull(String s) {
        try {
            return s == null ? null : Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normaliser(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }

    // ───────────────────────────── Application au dépôt ─────────────────────────────

    /**
     * Applique les règles au dépôt d'un dossier : 400 si une règle bloque, si une pièce exigée manque ou n'est pas au
     * bon format. Renvoie les informations à ajouter au dossier (clés plates de additional_info) :
     * {@code campagneId}, {@code pieceSupp.<code>} (lien), {@code pieceSuppLibelle.<code>}, {@code alerteRegle.<n>}.
     */
    public Map<String, String> appliquerAuDepot(Dossier d, Map<String, ?> piecesFournies) {
        return appliquer(evaluer(d), d, piecesFournies);
    }

    static Map<String, String> appliquer(Evaluation ev, Dossier d, Map<String, ?> piecesFournies) {
        if (!ev.blocages().isEmpty()) throw mauvais(ev.blocages().get(0));
        Map<String, ?> fournies = piecesFournies == null ? Map.of() : piecesFournies;
        Map<String, String> ajouts = new LinkedHashMap<>();
        if (d.session() != null) ajouts.put("campagneId", d.session());
        for (PieceExigee p : ev.piecesExigees()) {
            String lien = texte(fournies.get(p.code()));
            if (lien == null) throw mauvais("Joignez « " + p.libelle() + " » (" + libelleFormats(p.formats()) + ") : l'école l'exige pour ce dossier.");
            if (!lien.matches("(?i)^https?://\\S+$") || lien.length() > 1000) {
                throw mauvais("Le lien de « " + p.libelle() + " » n'est pas valide : envoyez de nouveau le fichier.");
            }
            if (!formatAccepte(lien, p.formats())) {
                throw mauvais("« " + p.libelle() + " » doit être en " + libelleFormats(p.formats()) + ".");
            }
            ajouts.put("pieceSupp." + p.code(), lien);
            ajouts.put("pieceSuppLibelle." + p.code(), p.libelle());
        }
        for (int i = 0; i < ev.avertissements().size(); i++) {
            ajouts.put("alerteRegle." + (i + 1), ev.avertissements().get(i));
        }
        return ajouts;
    }

    static String libelleFormats(String formats) {
        return switch (formats) {
            case "PDF" -> "PDF";
            case "IMAGE" -> "image (JPG, PNG)";
            default -> "PDF ou image";
        };
    }

    /** Format d'après l'extension du lien (les liens viennent de /media/upload, qui nomme le fichier selon son contenu). */
    static boolean formatAccepte(String lien, String formats) {
        String chemin = lien.replaceAll("[?#].*$", "").toLowerCase(Locale.ROOT);
        boolean pdf = chemin.endsWith(".pdf");
        boolean image = chemin.matches(".*\\.(png|jpe?g|webp|gif)$");
        return switch (formats) {
            case "PDF" -> pdf;
            case "IMAGE" -> image;
            default -> pdf || image;
        };
    }

    // ───────────────────────────── Outils ─────────────────────────────

    static String texte(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static ResponseStatusException mauvais(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
