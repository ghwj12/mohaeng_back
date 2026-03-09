package org.poolpool.mohaeng.event.list.repository;

import java.util.Optional;

import org.poolpool.mohaeng.event.list.entity.EventCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCategoryRepository extends JpaRepository<EventCategoryEntity, Integer> {
    Optional<EventCategoryEntity> findFirstByCategoryNameContaining(String keyword);

    Optional<EventCategoryEntity> findTopByOrderByCategoryIdAsc();
}