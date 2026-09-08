package ecole_221.sn.ecole.modules.planning.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, String> {
    List<RoomJpaEntity> findByCapacityGreaterThanEqual(int minCapacity);
    List<RoomJpaEntity> findByLocation(String location);
}
