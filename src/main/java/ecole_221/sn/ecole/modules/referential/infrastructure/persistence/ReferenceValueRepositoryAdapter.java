package ecole_221.sn.ecole.modules.referential.infrastructure.persistence;

import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceList;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceValue;
import ecole_221.sn.ecole.modules.referential.domain.port.ReferenceValueRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReferenceValueRepositoryAdapter implements ReferenceValueRepositoryPort {

    private final ReferenceValueJpaRepository jpa;

    public ReferenceValueRepositoryAdapter(ReferenceValueJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ReferenceValue> findVisible(ReferenceList list, String organizationId) {
        return jpa.findVisible(list.name(), organizationId).stream()
                .map(e -> toDomain(e, list))
                .toList();
    }

    @Override
    public Optional<ReferenceValue> find(ReferenceList list, String code, String organizationId) {
        Optional<ReferenceValueJpaEntity> entity = organizationId == null
                ? jpa.findByListCodeAndCodeAndOrganizationIdIsNull(list.name(), code)
                : jpa.findByListCodeAndCodeAndOrganizationId(list.name(), code, organizationId);
        return entity.map(e -> toDomain(e, list));
    }

    @Override
    public ReferenceValue save(ReferenceValue v) {
        ReferenceValueJpaEntity saved = jpa.save(new ReferenceValueJpaEntity(v.id(), v.list().name(), v.code(),
                v.label(), v.color(), v.sortOrder(), v.active(), v.system(), v.organizationId()));
        return toDomain(saved, v.list());
    }

    @Override
    public void deleteById(String id) {
        jpa.deleteById(id);
    }

    private ReferenceValue toDomain(ReferenceValueJpaEntity e, ReferenceList list) {
        return new ReferenceValue(e.getId(), list, e.getCode(), e.getLabel(), e.getColor(),
                e.getSortOrder(), e.isActive(), e.isSystemValue(), e.getOrganizationId());
    }
}
