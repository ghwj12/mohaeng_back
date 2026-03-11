package org.poolpool.mohaeng.event.list.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.poolpool.mohaeng.event.list.dto.EventDailyCountDto;
import org.poolpool.mohaeng.event.list.dto.EventRegionCountDto;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    Page<EventEntity> findByHost_UserIdAndEventStatusNot(Long userId, String eventStatus, Pageable pageable);

    Page<EventEntity> findByHost_UserIdAndEventStatusNotIn(Long userId, List<String> eventStatus, Pageable pageable);

    // 공개 행사 검색 - 승인된 행사만 노출
    @Query(value =
        "SELECT e.* FROM event e " +
        "LEFT JOIN event_category c ON e.category_id = c.category_id " +
        "LEFT JOIN event_region r ON e.region_id = r.region_id " +
        "WHERE " +
        "  (:keyword IS NULL OR e.title LIKE CONCAT('%', :keyword, '%') OR e.simple_explain LIKE CONCAT('%', :keyword, '%')) AND " +
        "  (:regionMin IS NULL OR r.region_id BETWEEN :regionMin AND :regionMax) AND " +
        "  (:filterStart IS NULL OR e.end_date >= :filterStart) AND " +
        "  (:filterEnd IS NULL OR e.start_date <= :filterEnd) AND " +
        "  (:categoryId IS NULL OR e.category_id = :categoryId) AND " +
        "  (:checkFree = false OR e.price = 0) AND " +
        "  (:hideClosed = false OR e.end_date >= :today) AND " +
        "  e.event_status NOT IN ('DELETED', 'REPORT_DELETED', 'report_deleted', '행사삭제') AND " +
        "  e.moderation_status = '승인' AND " +
        "  (:eventStatus IS NULL OR e.event_status = :eventStatus) " +
        "ORDER BY " +
        "  CASE WHEN e.start_date >= :today THEN 0 ELSE 1 END ASC, " +
        "  ABS(DATEDIFF(e.start_date, :today)) ASC",
        countQuery =
        "SELECT COUNT(*) FROM event e " +
        "LEFT JOIN event_region r ON e.region_id = r.region_id " +
        "WHERE " +
        "  (:keyword IS NULL OR e.title LIKE CONCAT('%', :keyword, '%') OR e.simple_explain LIKE CONCAT('%', :keyword, '%')) AND " +
        "  (:regionMin IS NULL OR r.region_id BETWEEN :regionMin AND :regionMax) AND " +
        "  (:filterStart IS NULL OR e.end_date >= :filterStart) AND " +
        "  (:filterEnd IS NULL OR e.start_date <= :filterEnd) AND " +
        "  (:categoryId IS NULL OR e.category_id = :categoryId) AND " +
        "  (:checkFree = false OR e.price = 0) AND " +
        "  (:hideClosed = false OR e.end_date >= :today) AND " +
        "  e.event_status NOT IN ('DELETED', 'REPORT_DELETED', 'report_deleted', '행사삭제') AND " +
        "  e.moderation_status = '승인' AND " +
        "  (:eventStatus IS NULL OR e.event_status = :eventStatus)",
        nativeQuery = true)
    Page<EventEntity> searchEventsOrderByDateProximity(
            @Param("keyword") String keyword,
            @Param("regionMin") Long regionMin,
            @Param("regionMax") Long regionMax,
            @Param("filterStart") LocalDate filterStart,
            @Param("filterEnd") LocalDate filterEnd,
            @Param("categoryId") Integer categoryId,
            @Param("checkFree") boolean checkFree,
            @Param("hideClosed") boolean hideClosed,
            @Param("today") LocalDate today,
            @Param("eventStatus") String eventStatus,
            Pageable pageable);

    // 주제 필터 포함 버전 - 승인된 행사만 노출
    @Query(value =
        "SELECT e.* FROM event e " +
        "LEFT JOIN event_region r ON e.region_id = r.region_id " +
        "WHERE " +
        "  (:keyword IS NULL OR e.title LIKE CONCAT('%', :keyword, '%') OR e.simple_explain LIKE CONCAT('%', :keyword, '%')) AND " +
        "  (:regionMin IS NULL OR r.region_id BETWEEN :regionMin AND :regionMax) AND " +
        "  (:filterStart IS NULL OR e.end_date >= :filterStart) AND " +
        "  (:filterEnd IS NULL OR e.start_date <= :filterEnd) AND " +
        "  (:categoryId IS NULL OR e.category_id = :categoryId) AND " +
        "  (:checkFree = false OR e.price = 0) AND " +
        "  (:hideClosed = false OR e.end_date >= :today) AND " +
        "  e.event_status NOT IN ('DELETED', 'REPORT_DELETED', 'report_deleted', '행사삭제') AND " +
        "  e.moderation_status = '승인' AND " +
        "  (:eventStatus IS NULL OR e.event_status = :eventStatus) AND " +
        "  CONCAT(',', e.topic_ids, ',') LIKE CONCAT('%,', :topicId, ',%') " +
        "ORDER BY " +
        "  CASE WHEN e.start_date >= :today THEN 0 ELSE 1 END ASC, " +
        "  ABS(DATEDIFF(e.start_date, :today)) ASC",
        countQuery =
        "SELECT COUNT(*) FROM event e " +
        "LEFT JOIN event_region r ON e.region_id = r.region_id " +
        "WHERE " +
        "  (:keyword IS NULL OR e.title LIKE CONCAT('%', :keyword, '%') OR e.simple_explain LIKE CONCAT('%', :keyword, '%')) AND " +
        "  (:regionMin IS NULL OR r.region_id BETWEEN :regionMin AND :regionMax) AND " +
        "  (:filterStart IS NULL OR e.end_date >= :filterStart) AND " +
        "  (:filterEnd IS NULL OR e.start_date <= :filterEnd) AND " +
        "  (:categoryId IS NULL OR e.category_id = :categoryId) AND " +
        "  (:checkFree = false OR e.price = 0) AND " +
        "  (:hideClosed = false OR e.end_date >= :today) AND " +
        "  e.event_status NOT IN ('DELETED', 'REPORT_DELETED', 'report_deleted', '행사삭제') AND " +
        "  e.moderation_status = '승인' AND " +
        "  (:eventStatus IS NULL OR e.event_status = :eventStatus) AND " +
        "  CONCAT(',', e.topic_ids, ',') LIKE CONCAT('%,', :topicId, ',%')",
        nativeQuery = true)
    Page<EventEntity> searchEventsWithTopicOrderByDate(
            @Param("keyword") String keyword,
            @Param("regionMin") Long regionMin,
            @Param("regionMax") Long regionMax,
            @Param("filterStart") LocalDate filterStart,
            @Param("filterEnd") LocalDate filterEnd,
            @Param("categoryId") Integer categoryId,
            @Param("checkFree") boolean checkFree,
            @Param("hideClosed") boolean hideClosed,
            @Param("today") LocalDate today,
            @Param("topicId") String topicId,
            @Param("eventStatus") String eventStatus,
            Pageable pageable);

    @Query("SELECT new org.poolpool.mohaeng.event.list.dto.EventRegionCountDto(e.region.regionId, COUNT(e)) " +
           "FROM EventEntity e " +
           "WHERE e.eventStatus NOT IN ('DELETED','REPORT_DELETED','report_deleted','행사삭제','행사종료') " +
           "AND e.moderationStatus = '승인' " +
           "GROUP BY e.region.regionId")
    List<EventRegionCountDto> countEventsByRegion();

    @Query(value = "SELECT COUNT(*) FROM event_participation " +
                   "WHERE event_id = :eventId " +
                   "AND pct_status IN ('결제대기', '결제완료', '참여확정')",
           nativeQuery = true)
    Integer countParticipantsByEventId(@Param("eventId") Long eventId);

    @Query(value =
        "SELECT DATE(d.date) as date, COUNT(e.event_id) as count " +
        "FROM event e " +
        "JOIN ( " +
        "  SELECT DATE_ADD(e2.start_date, INTERVAL n.n DAY) as date, e2.event_id " +
        "  FROM event e2 " +
        "  JOIN ( " +
        "    SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 " +
        "    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 " +
        "    UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 " +
        "    UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 " +
        "    UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 " +
        "    UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 " +
        "    UNION SELECT 30 " +
        "  ) n " +
        "  WHERE DATE_ADD(e2.start_date, INTERVAL n.n DAY) <= e2.end_date " +
        ") d ON e.event_id = d.event_id " +
        "WHERE e.region_id BETWEEN :regionMin AND :regionMax " +
        "AND e.event_status NOT IN ('DELETED', 'REPORT_DELETED', 'report_deleted', '행사삭제') " +
        "AND e.moderation_status = '승인' " +
        "GROUP BY DATE(d.date) ORDER BY DATE(d.date)",
        nativeQuery = true)
    List<EventDailyCountDto> countDailyEventsByRegion(
            @Param("regionMin") Long regionMin,
            @Param("regionMax") Long regionMax);

    List<EventEntity> findTop6ByEventStatusNotInOrderByViewsDesc(List<String> statuses);

    @Query("SELECT e FROM EventEntity e WHERE e.eventStatus NOT IN ('DELETED', 'report_deleted', 'REPORT_DELETED', '행사삭제')")
    List<EventEntity> findAllForScheduler();

    boolean existsByHost_UserIdAndEventStatusNotIn(Long hostId, List<String> statuses);

    boolean existsByExternalSourceAndExternalContentId(String externalSource, String externalContentId);

    List<EventEntity> findTop6ByEventStatusNotInOrderByCreatedAtDesc(List<String> statuses);

    List<EventEntity> findByEventStatusNotIn(List<String> statuses);

    // 관리자 검수용
    Page<EventEntity> findByModerationStatusInOrderByCreatedAtDesc(List<String> statuses, Pageable pageable);

    Optional<EventEntity> findByEventIdAndModerationStatusIn(Long eventId, List<String> statuses);

    Page<EventEntity> findByModerationStatusOrderByCreatedAtDesc(String moderationStatus, Pageable pageable);

    Optional<EventEntity> findByEventIdAndModerationStatus(Long eventId, String moderationStatus);

    Page<EventEntity> findByEventStatusInOrderByCreatedAtDesc(List<String> statuses, Pageable pageable);

    Page<EventEntity> findByEventStatusOrderByCreatedAtDesc(String eventStatus, Pageable pageable);
}