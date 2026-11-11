package ecole_221.sn.ecole.modules.parent.domain.model;

/** Présence réelle d'un enfant (saisie par un professeur, table attendances) — APP-BE-19. */
public record PresenceEnfant(String id, String date, String statut, String coursId, String coursNom, String professeurId) {
}
