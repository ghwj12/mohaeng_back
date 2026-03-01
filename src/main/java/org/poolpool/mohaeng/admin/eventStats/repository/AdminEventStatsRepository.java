package org.poolpool.mohaeng.admin.eventStats.repository;

import org.poolpool.mohaeng.admin.eventStats.dto.AdminEventStatsDto;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AdminEventStatsRepository extends JpaRepository<EventEntity, Long> {

    // ── 1. 필터링된 행사 목록 (페이징) ──
    @Query("SELECT e FROM EventEntity e WHERE " +
           "e.eventStatus != 'DELETED' AND " +
           "(:keyword IS NULL OR e.title LIKE CONCAT('%',:keyword,'%') OR e.simpleExplain LIKE CONCAT('%',:keyword,'%')) AND " +
           "(:categoryId IS NULL OR e.category.categoryId = :categoryId) AND " +
           "(:status IS NULL OR e.eventStatus = :status) AND " +
           "(:regionMin IS NULL OR e.region.regionId BETWEEN :regionMin AND :regionMax) AND " +
           "(:startDate IS NULL OR e.endDate >= :startDate) AND " +
           "(:endDate IS NULL OR e.startDate <= :endDate) AND " +
           "(:checkFree = false OR e.price = 0) AND " +
           "(:hideClosed = false OR e.endDate >= :today)")
    Page<EventEntity> findAllEventsFiltered(
        @Param("keyword") String keyword,
        @Param("categoryId") Integer categoryId,
        @Param("status") String status,
        @Param("regionMin") Long regionMin,
        @Param("regionMax") Long regionMax,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("checkFree") boolean checkFree,
        @Param("hideClosed") boolean hideClosed,
        @Param("today") LocalDate today,
        Pageable pageable
    );

    // ── 2. 월별 행사 수 ──
    @Query("SELECT new org.poolpool.mohaeng.admin.eventStats.dto.AdminEventStatsDto$MonthlyStatsResponse(" +
           "MONTH(e.startDate), COUNT(e)) " +
           "FROM EventEntity e " +
           "WHERE YEAR(e.startDate) = :year AND e.eventStatus != 'DELETED' " +
           "GROUP BY MONTH(e.startDate) ORDER BY MONTH(e.startDate)")
    List<AdminEventStatsDto.MonthlyStatsResponse> countByMonth(@Param("year") int year);

    // ── 3. 카테고리별 진행중인 행사 수 ──
    @Query("SELECT new org.poolpool.mohaeng.admin.eventStats.dto.AdminEventStatsDto$CategoryStatsResponse(" +
           "e.category.categoryName, COUNT(e)) " +
           "FROM EventEntity e " +
           "WHERE e.eventStatus IN ('행사중', '행사참여모집중', '부스모집중') " +
           "GROUP BY e.category.categoryName")
    List<AdminEventStatsDto.CategoryStatsResponse> countByCategory();

    // ── 4. 결제완료 참여자 수 ──
    @Query(value = "SELECT COUNT(*) FROM EVENT_PARTICIPATION WHERE EVENT_ID = :eventId AND PCT_STATUS IN ('결제완료', '참여확정')",
           nativeQuery = true)
    Long countParticipantsByEventId(@Param("eventId") Long eventId);

    // ── 5. 성별 통계 ──
    @Query(value = "SELECT PCT_GENDER, COUNT(*) as cnt FROM EVENT_PARTICIPATION " +
                   "WHERE EVENT_ID = :eventId AND PCT_STATUS IN ('결제완료', '참여확정') AND PCT_GENDER IS NOT NULL " +
                   "GROUP BY PCT_GENDER",
           nativeQuery = true)
    List<Object[]> countGenderByEventId(@Param("eventId") Long eventId);

    // ── 6. 연령대 통계 ──
    @Query(value = "SELECT PCT_AGEGROUP, COUNT(*) as cnt FROM EVENT_PARTICIPATION " +
                   "WHERE EVENT_ID = :eventId AND PCT_STATUS IN ('결제완료', '참여확정') AND PCT_AGEGROUP IS NOT NULL " +
                   "GROUP BY PCT_AGEGROUP ORDER BY PCT_AGEGROUP",
           nativeQuery = true)
    List<Object[]> countAgeGroupByEventId(@Param("eventId") Long eventId);

    // ── 7. 부스 수익 합산 ──
    @Query(value = "SELECT COALESCE(SUM((TOTAL_COUNT - REMAIN_COUNT) * BOOTH_PRICE), 0) FROM HOST_BOOTH WHERE EVENT_ID = :eventId",
           nativeQuery = true)
    Long sumBoothRevenueByEventId(@Param("eventId") Long eventId);

    // ── 8. 리뷰 수 ──
    @Query(value = "SELECT COUNT(*) FROM event_review WHERE EVENT_ID = :eventId", nativeQuery = true)
    Long countReviewsByEventId(@Param("eventId") Long eventId);

    // ── 9. 관심(위시리스트) 수 ──
    @Query(value = "SELECT COUNT(*) FROM event_wishlist WHERE EVENT_ID = :eventId", nativeQuery = true)
    Long countWishlistByEventId(@Param("eventId") Long eventId);
}
