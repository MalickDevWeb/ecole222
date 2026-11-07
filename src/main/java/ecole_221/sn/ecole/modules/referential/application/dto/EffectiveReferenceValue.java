package ecole_221.sn.ecole.modules.referential.application.dto;

/**
 * Valeur telle que la voit une école, après fusion « globale + surcharge école ».
 *
 * @param scope    GLOBAL (valeur plateforme), SURCHARGE (valeur plateforme modifiée par l'école),
 *                 ECOLE (valeur créée par l'école)
 * @param system   valeur de départ de la plateforme
 * @param locked   la liste est verrouillée par le code (clés non modifiables)
 */
public record EffectiveReferenceValue(String code, String label, String color, int sortOrder,
                                      boolean active, boolean system, boolean locked, Scope scope) {

    public enum Scope { GLOBAL, SURCHARGE, ECOLE }
}
