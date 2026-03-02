package org.poolpool.mohaeng.event.mypage.service;

import java.util.List;

import org.poolpool.mohaeng.auth.exception.AuthException;
import org.poolpool.mohaeng.event.mypage.dto.BoothApplicationDetailResponse;
import org.poolpool.mohaeng.event.mypage.dto.BoothMypageResponse;
import org.poolpool.mohaeng.event.mypage.repository.MypageBoothRepository;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MypageBoothService {

    private final MypageBoothRepository repo;
    private final NotificationService notificationService; //  추가

    //  마이페이지 부스 내역(신청한 부스)
    public List<BoothMypageResponse> getMyBooths(Long userId) {
        return repo.findMyBooths(userId);
    }

    //  마이페이지 부스 내역(주최자: 받은 부스)
    public List<BoothMypageResponse> getReceivedBooths(Long hostUserId) {
        return repo.findReceivedBooths(hostUserId);
    }

    /**
     *  신청서 상세 조회
     * - 주최자(해당 이벤트 host) 또는 신청자(pb.user_id)만 조회 가능
     */
    public BoothApplicationDetailResponse getBoothApplicationDetail(Long viewerId, Long pctBoothId) {
        BoothApplicationDetailResponse res = repo.findBoothDetail(viewerId, pctBoothId);
        if (res == null) {
            throw AuthException.forbidden("BOOTH_FORBIDDEN", "조회 권한이 없거나 신청 내역을 찾을 수 없습니다.");
        }
        return res;
    }

    /**
     *  (주최자) 승인
     * - 승인 처리 성공 후 신청자에게 BOOTH_ACCEPT 알림(9)
     */
    @Transactional
    public void approveBooth(Long hostUserId, Long pctBoothId) {
        int updated = repo.updateBoothStatusAsHostRelaxed(hostUserId, pctBoothId, "승인");
        if (updated == 0) {
            throw AuthException.badRequest("BOOTH_CANNOT_APPROVE", "승인할 수 없는 상태이거나 처리 권한이 없습니다.");
        }

        //  승인 알림(신청자에게)
        BoothApplicationDetailResponse detail = repo.findBoothDetail(hostUserId, pctBoothId);
        if (detail != null) {
            Long applicantId = detail.getUserId(); // 신청자
            Long eventId = detail.getEventId();    // 행사

            if (applicantId != null && eventId != null && !applicantId.equals(hostUserId)) {
                // reportId는 항상 null (FK 이슈 방지)
                notificationService.create(applicantId, NotiTypeId.BOOTH_ACCEPT, eventId, null);
            }
        }
    }

    /**
     *  (주최자) 반려 + (결제 환불 처리: 결제 모듈 연동 지점)
     * - 반려 처리 성공 후 신청자에게 BOOTH_REJECT 알림(10)
     */
    @Transactional
    public void rejectBooth(Long hostUserId, Long pctBoothId) {
        int updated = repo.updateBoothStatusAsHostRelaxed(hostUserId, pctBoothId, "반려");
        if (updated == 0) {
            throw AuthException.badRequest("BOOTH_CANNOT_REJECT", "반려할 수 없는 상태이거나 처리 권한이 없습니다.");
        }

        //  반려 알림(신청자에게)
        BoothApplicationDetailResponse detail = repo.findBoothDetail(hostUserId, pctBoothId);
        if (detail != null) {
            Long applicantId = detail.getUserId();
            Long eventId = detail.getEventId();

            if (applicantId != null && eventId != null && !applicantId.equals(hostUserId)) {
                notificationService.create(applicantId, NotiTypeId.BOOTH_REJECT, eventId, null);
            }
        }

        // TODO: 결제 환불 처리 연동
    }
}