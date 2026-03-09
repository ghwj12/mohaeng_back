package org.poolpool.mohaeng.event.list.repository;

import java.util.Optional;

import org.poolpool.mohaeng.event.list.entity.EventRegionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegionRepository extends JpaRepository<EventRegionEntity, Long> {
    Optional<EventRegionEntity> findFirstByRegionNameContaining(String keyword);

    Optional<EventRegionEntity> findTopByOrderByRegionIdAsc();
}