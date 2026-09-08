package ecole_221.sn.ecole.modules.planning.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotJpaRepository extends JpaRepository<SlotJpaEntity, String> {
    List<SlotJpaEntity> findByDayOfWeek(String dayOfWeek);
    List<SlotJpaEntity> findByType(String type);
}
