package org.poolpool.mohaeng.event.list.repository;

import java.time.LocalDate;
import java.util.List;

import org.poolpool.mohaeng.event.list.dto.EventDailyCountDto;
import org.poolpool.mohaeng.event.list.dto.EventRegionCountDto;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    // 마이페이지: 내가 등록(주최)한 행사 목록
    Page<EventEntity> findByHost_UserIdAndEventStatusNot(Long userId, String eventStatus, Pageable pageable);

    // 마이페이지: 내가 등록(주최)한 행사 목록 (여러 상태 제외)
    Page<EventEntity> findByHost_UserIdAndEventStatusNotIn(Long userId, List<String> eventStatus, Pageable pageable);

    // ✅ Issue 5: eventStatus 파라미터 추가 (NULL이면 전체 조회)
    @Query("SELECT e FROM EventEntity e WHERE "
            + "(:keyword IS NULL OR e.title LIKE CONCAT('%', :keyword, '%') OR e.simpleExplain LIKE CONCAT('%', :keyword, '%')) AND "
            + "(:regionId IS NULL OR e.region.regionId BETWEEN :regionMin AND :regionMax) AND "
            + "(:filterStart IS NULL OR e.endDate >= :filterStart) AND "
            + "(:filterEnd IS NULL OR e.startDate <= :filterEnd) AND "
            + "(:categoryId IS NULL OR e.category.categoryId = :categoryId) AND "
            + "(:checkFree = false OR e.price = 0) AND "
            + "(:hideClosed = false OR e.endDate >= :today) AND "
            + "e.eventStatus NOT IN ('DELETED','행사삭제') AND "
            + "(:eventStatus IS NULL OR e.eventStatus = :eventStatus) AND "
            + "(:topicIds IS NULL OR e.topicIds LIKE CONCAT('%', :topicIds, '%'))")
    Page<EventEntity> searchEvents(
            @Param("keyword") String keyword,
            @Param("regionId") Long regionId,
            @Param("regionMin") Long regionMin,
            @Param("regionMax") Long regionMax,
            @Param("filterStart") LocalDate filterStart,
            @Param("filterEnd") LocalDate filterEnd,
            @Param("categoryId") Integer categoryId,
            @Param("checkFree") boolean checkFree,
            @Param("hideClosed") boolean hideClosed,
            @Param("today") LocalDate today,
            @Param("topicIds") String topicIds,
            @Param("eventStatus") String eventStatus,  // ✅ Issue 5
            Pageable pageable);

    @Query("SELECT new org.poolpool.mohaeng.event.list.dto.EventRegionCountDto(e.region.regionId, COUNT(e)) "
            + "FROM EventEntity e "
            + "WHERE e.eventStatus NOT IN ('DELETED','행사삭제') "
            + "GROUP BY e.region.regionId")
    List<EventRegionCountDto> countEventsByRegion();

    // ✅ Issue 3: 행사별 결제완료/대기/확정 참여자 수 조회
    @Query(value = "SELECT COUNT(*) FROM event_participation " +
                   "WHERE event_id = :eventId " +
                   "AND pct_status IN ('결제완료', '결제대기', '참여확정')",
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
        "    UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 " +
        "    UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 " +
        "    UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 " +
        "    UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 " +
        "    UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 " +
        "    UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 " +
        "    UNION SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 " +
        "    UNION SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 " +
        "    UNION SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 " +
        "    UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 " +
        "    UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 " +
        "    UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 " +
        "    UNION SELECT 90 UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 " +
        "    UNION SELECT 95 UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 " +
        "    UNION SELECT 100 UNION SELECT 101 UNION SELECT 102 UNION SELECT 103 UNION SELECT 104 " +
        "    UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108 UNION SELECT 109 " +
        "    UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114 " +
        "    UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119 " +
        "    UNION SELECT 120 UNION SELECT 121 UNION SELECT 122 UNION SELECT 123 UNION SELECT 124 " +
        "    UNION SELECT 125 UNION SELECT 126 UNION SELECT 127 UNION SELECT 128 UNION SELECT 129 " +
        "    UNION SELECT 130 UNION SELECT 131 UNION SELECT 132 UNION SELECT 133 UNION SELECT 134 " +
        "    UNION SELECT 135 UNION SELECT 136 UNION SELECT 137 UNION SELECT 138 UNION SELECT 139 " +
        "    UNION SELECT 140 UNION SELECT 141 UNION SELECT 142 UNION SELECT 143 UNION SELECT 144 " +
        "    UNION SELECT 145 UNION SELECT 146 UNION SELECT 147 UNION SELECT 148 UNION SELECT 149 " +
        "    UNION SELECT 150 UNION SELECT 151 UNION SELECT 152 UNION SELECT 153 UNION SELECT 154 " +
        "    UNION SELECT 155 UNION SELECT 156 UNION SELECT 157 UNION SELECT 158 UNION SELECT 159 " +
        "    UNION SELECT 160 UNION SELECT 161 UNION SELECT 162 UNION SELECT 163 UNION SELECT 164 " +
        "    UNION SELECT 165 " +
        "  ) n " +
        "  WHERE DATE_ADD(e2.start_date, INTERVAL n.n DAY) <= e2.end_date " +
        ") d ON e.event_id = d.event_id " +
        "WHERE e.region_id BETWEEN :regionMin AND :regionMax " +
        "AND e.event_status NOT IN ('DELETED') " +
        "GROUP BY DATE(d.date) " +
        "ORDER BY DATE(d.date)",
        nativeQuery = true)
    List<EventDailyCountDto> countDailyEventsByRegion(
            @Param("regionMin") Long regionMin,
            @Param("regionMax") Long regionMax);

    // 회원 탈퇴 시 주최 행사 중 행사종료/삭제되지 않은 행사 존재 유무 조회
    boolean existsByHost_UserIdAndEventStatusNotIn(Long hostId, List<String> statuses);
}
