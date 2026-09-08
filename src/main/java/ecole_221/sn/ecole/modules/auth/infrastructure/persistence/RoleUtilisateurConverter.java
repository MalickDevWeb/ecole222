package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;

@Converter(autoApply = true)
public class RoleUtilisateurConverter implements AttributeConverter<RoleUtilisateur, String> {

    @Override
    public String convertToDatabaseColumn(RoleUtilisateur attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RoleUtilisateur convertToEntityAttribute(String dbData) {
        return RoleUtilisateur.fromString(dbData);
    }
}
