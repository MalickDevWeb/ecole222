package ecole_221.sn.ecole.modules.corbeille.application;

import java.util.Arrays;
import java.util.Optional;

/**
 * Éléments qui passent par la corbeille (suppression douce, V21). Chaque type nomme sa table, son libellé et le module
 * de la matrice des droits qui gouverne sa restauration par le personnel (null : contenu d'un cours, restauré par son
 * auteur ou l'administration seulement).
 *
 * <p>{@code selectLibelle} renvoie, pour une ligne {@code t}, les colonnes {@code titre}, {@code ctx1} et {@code ctx2}
 * qui composent le libellé lisible (ex. « Devoir “Exercices chapitre 3” — Algorithmique (L1 Info) »).
 * Les jointures sont toujours bornées à la même école que la ligne.</p>
 */
public enum TypeCorbeille {

    DEVOIR("devoir", "homeworks", "Devoir", null, true, "t.title"),
    SUPPORT("support", "lessons", "Support de cours", null, true, "t.title"),
    MODULE("module", "modules", "Module", null, true, "t.name"),
    QUIZ("quiz", "quizzes", "Quiz", null, true, "t.title"),
    RAPPEL("rappel", "reminders", "Rappel", null, true, "t.content"),
    EVALUATION("evaluation", "evaluations_cours", "Évaluation", null, true, "t.titre"),
    CRENEAU_HORAIRE("creneau-horaire", "slots", "Créneau horaire", "planning", false,
            "SELECT t.day_of_week AS titre, t.start_time AS ctx1, t.end_time AS ctx2 FROM slots t"),
    CRENEAU("creneau", "creneaux_planning", "Créneau d'emploi du temps", "planning", false,
            "SELECT t.nom AS titre, t.jour AS ctx1, COALESCE(cl.nom, cl.name) AS ctx2, t.heure_debut AS debut, t.heure_fin AS fin "
                    + "FROM creneaux_planning t LEFT JOIN classes cl ON cl.id = t.classe_id AND cl.organization_id = t.organization_id"),
    SALLE("salle", "rooms", "Salle", "planning", false,
            "SELECT COALESCE(t.nom, t.name) AS titre, t.location AS ctx1, CAST(NULL AS VARCHAR(1)) AS ctx2 FROM rooms t"),
    FILIERE("filiere", "filieres", "Filière", "planning", false,
            "SELECT COALESCE(t.nom, t.name) AS titre, t.code AS ctx1, CAST(NULL AS VARCHAR(1)) AS ctx2 FROM filieres t"),
    CLASSE("classe", "classes", "Classe", "planning", false,
            "SELECT COALESCE(t.nom, t.name) AS titre, COALESCE(f.nom, f.name) AS ctx1, CAST(NULL AS VARCHAR(1)) AS ctx2 FROM classes t "
                    + "LEFT JOIN filieres f ON f.id = t.filiere_id AND f.organization_id = t.organization_id"),
    MESSAGE("message", "messages", "Message", "collaboration", false,
            // jamais le contenu du message : seulement la discussion où il a été écrit
            "SELECT CAST(NULL AS VARCHAR(1)) AS titre, g.name AS ctx1, CAST(NULL AS VARCHAR(1)) AS ctx2 FROM messages t "
                    + "LEFT JOIN group_chats g ON g.id = t.group_id AND g.organization_id = t.organization_id"),
    SEANCE_EN_DIRECT("seance-en-direct", "meets", "Séance en direct", "collaboration", false,
            "SELECT t.title AS titre, t.class_names AS ctx1, CAST(NULL AS VARCHAR(1)) AS ctx2 FROM meets t"),
    REFERENTIEL("referentiel", "reference_values", "Valeur de référentiel", "ecole", false,
            "SELECT t.label AS titre, t.list_code AS ctx1, t.code AS ctx2 FROM reference_values t");

    private final String code;
    private final String table;
    private final String libelle;
    private final String moduleDroits;
    private final boolean contenuDeCours;
    private final String selectLibelle;

    TypeCorbeille(String code, String table, String libelle, String moduleDroits, boolean contenuDeCours, String titreOuSelect) {
        this.code = code;
        this.table = table;
        this.libelle = libelle;
        this.moduleDroits = moduleDroits;
        this.contenuDeCours = contenuDeCours;
        this.selectLibelle = contenuDeCours
                ? "SELECT " + titreOuSelect + " AS titre, c.name AS ctx1, COALESCE(cl.nom, cl.name) AS ctx2 FROM " + table + " t "
                  + "LEFT JOIN courses c ON c.id = t.course_id AND c.organization_id = t.organization_id "
                  + "LEFT JOIN classes cl ON cl.id = c.classe AND cl.organization_id = t.organization_id"
                : titreOuSelect;
    }

    /** Code utilisé dans les routes : /corbeille/{type}/{id}/restaurer. */
    public String code() { return code; }
    public String table() { return table; }
    public String libelle() { return libelle; }
    /** Module de la matrice des droits (personnel) ; null pour un contenu de cours. */
    public String moduleDroits() { return moduleDroits; }
    /** Ligne rattachée à un cours (colonne course_id) : restaurée seulement si le cours existe et, pour un
     *  professeur, s'il en est toujours l'enseignant. */
    public boolean contenuDeCours() { return contenuDeCours; }
    String selectLibelle() { return selectLibelle; }

    /** Genre grammatical du libellé (charte d'écriture : accord des participes, jamais « placé(e) »). */
    public boolean feminin() {
        return switch (this) {
            case EVALUATION, SALLE, FILIERE, CLASSE, SEANCE_EN_DIRECT, REFERENTIEL -> true;
            default -> false;
        };
    }

    /** Participe passé accordé au type : « placé » → « placée » pour une salle. */
    public String accorde(String participeMasculin) {
        return feminin() ? participeMasculin + "e" : participeMasculin;
    }

    /** « ce devoir », « cette salle », « cette valeur de référentiel ». */
    public String demonstratif() {
        String nom = Character.toLowerCase(libelle.charAt(0)) + libelle.substring(1);
        if (feminin()) return "cette " + nom;
        return ("aeiouhéèê".indexOf(nom.charAt(0)) >= 0 ? "cet " : "ce ") + nom;
    }

    /** Pronom sujet : « il » ou « elle ». */
    public String pronom() {
        return feminin() ? "elle" : "il";
    }

    /** Message d'une mise à la corbeille : « Salle “B12” placée dans la corbeille. » */
    public String messageMiseALaCorbeille(String libelleElement) {
        return libelleElement + " " + accorde("placé") + " dans la corbeille.";
    }

    /** Nom au pluriel, en minuscules : « salles », « créneaux d'emploi du temps », « quiz ». */
    public String pluriel() {
        return switch (this) {
            case DEVOIR -> "devoirs";
            case SUPPORT -> "supports de cours";
            case MODULE -> "modules";
            case QUIZ -> "quiz";
            case RAPPEL -> "rappels";
            case EVALUATION -> "évaluations";
            case CRENEAU_HORAIRE -> "créneaux horaires";
            case CRENEAU -> "créneaux d'emploi du temps";
            case SALLE -> "salles";
            case FILIERE -> "filières";
            case CLASSE -> "classes";
            case MESSAGE -> "messages";
            case SEANCE_EN_DIRECT -> "séances en direct";
            case REFERENTIEL -> "valeurs de référentiel";
        };
    }

    /**
     * Message de plusieurs mises à la corbeille du même type : le libellé seul pour un élément, sinon
     * « 3 salles placées dans la corbeille : Salle “A”, Salle “B”, Salle “C”. » (cinq libellés au plus).
     */
    public String messageMiseALaCorbeille(java.util.List<String> libelles) {
        if (libelles.size() == 1) return messageMiseALaCorbeille(libelles.get(0));
        int n = libelles.size();
        String extrait = String.join(", ", libelles.subList(0, Math.min(5, n)))
                + (n > 5 ? (n - 5 == 1 ? " et 1 autre" : " et " + (n - 5) + " autres") : "");
        return n + " " + pluriel() + " " + accorde("placé") + "s dans la corbeille : " + extrait + ".";
    }

    public static Optional<TypeCorbeille> parCode(String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst();
    }
}
