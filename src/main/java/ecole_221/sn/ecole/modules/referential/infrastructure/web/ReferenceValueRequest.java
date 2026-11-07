package ecole_221.sn.ecole.modules.referential.infrastructure.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corps des requêtes POST / PUT /v1/referentiels/{list}.
 * En PUT, un champ absent (null) conserve sa valeur actuelle.
 */
public record ReferenceValueRequest(
        @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_]*", message = "code : lettres, chiffres ou _") String code,
        @Size(max = 80) String label,
        @Size(max = 20) @Pattern(regexp = "[a-z]*", message = "color : nom de teinte en minuscules (ex. emerald)") String color,
        @Min(0) @Max(9999) Integer sortOrder,
        Boolean active
) { }
