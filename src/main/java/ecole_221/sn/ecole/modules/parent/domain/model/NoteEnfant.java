package ecole_221.sn.ecole.modules.parent.domain.model;

/**
 * Note réelle d'un enfant (saisie par un professeur, table grades) — APP-BE-19.
 * cc / examen peuvent être null tant que la note n'est pas saisie.
 */
public record NoteEnfant(String id, String coursId, String coursNom, String professeurId, Double cc, Double examen) {
}
