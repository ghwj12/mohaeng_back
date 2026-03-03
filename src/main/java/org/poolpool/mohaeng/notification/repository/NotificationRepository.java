package org.poolpool.mohaeng.notification.repository;

import java.util.List;

import org.poolpool.mohaeng.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    // 알림 목록
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 알림 개수
    long countByUserId(Long userId);

    // 읽음 = 삭제
    int deleteByNotificationIdAndUserId(Long notificationId, Long userId);

    // 전체 읽음 = 전체 삭제
    int deleteByUserId(Long userId);

    //  FK 끊기(단일 reportId)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.reportId = null where n.reportId = :reportId")
    int detachReport(@Param("reportId") Long reportId);

    //  FK 끊기(여러 reportId 한번에)  ← approve에서 같은 이벤트의 다른 신고들도 삭제하므로 필요
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.reportId = null where n.reportId in :reportIds")
    int detachReports(@Param("reportIds") List<Long> reportIds);
}