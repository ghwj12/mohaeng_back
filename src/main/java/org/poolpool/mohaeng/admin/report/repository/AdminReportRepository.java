package org.poolpool.mohaeng.admin.report.repository;

import java.util.List;

import org.poolpool.mohaeng.admin.report.entity.AdminReportFEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AdminReportRepository extends JpaRepository<AdminReportFEntity, Long> {

    //  미처리(PENDING) 먼저, 그 다음 최신순
    @Query("""
        select r
          from AdminReportFEntity r
         order by
           case when r.reportResult = 'PENDING' then 0 else 1 end,
           r.createdAt desc
    """)
    Page<AdminReportFEntity> findAllForAdminOrder(Pageable pageable);

    // (기존) 최신순 목록
    Page<AdminReportFEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 중복 신고 방지
    boolean existsByReporterIdAndEventId(long reporterId, long eventId);

    // (기존) 같은 이벤트의 다른 신고들 삭제 (이제 안 쓸 예정이지만 남겨둬도 됨)
    int deleteByEventIdAndReportIdNot(long eventId, long reportId);

    // (기존) FK 끊기용 (이제 신고 삭제 안 하면 보통 필요 없음)
    @Query("select r.reportId from AdminReportFEntity r where r.eventId = :eventId")
    List<Long> findReportIdsByEventId(@Param("eventId") Long eventId);

    //  승인 처리 시: 같은 이벤트의 다른 "미처리" 신고는 자동 반려로 내려서 목록 아래로 보내기
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update AdminReportFEntity r
           set r.reportResult = 'REJECTED'
         where r.eventId = :eventId
           and r.reportId <> :reportId
           and r.reportResult = 'PENDING'
    """)
    int rejectOtherPendings(@Param("eventId") Long eventId, @Param("reportId") Long reportId);
}