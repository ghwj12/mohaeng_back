package org.poolpool.mohaeng.event.mypage.service;

import java.util.List;

import org.poolpool.mohaeng.auth.exception.AuthException;
import org.poolpool.mohaeng.event.mypage.dto.BoothApplicationDetailResponse;
import org.poolpool.mohaeng.event.mypage.dto.BoothMypageResponse;
import org.poolpool.mohaeng.event.mypage.repository.MypageBoothRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MypageBoothService {

    private final MypageBoothRepository repo;

    // ✅ 마이페이지 부스 내역(신청한 부스)
    public List<BoothMypageResponse> getMyBooths(Long userId) {
        return repo.findMyBooths(userId);
    }

    // ✅ 마이페이지 부스 내역(주최자: 받은 부스)
    public List<BoothMypageResponse> getReceivedBooths(Long hostUserId) {
        return repo.findReceivedBooths(hostUserId);
    }

    /**
     * ✅ 신청서 상세 조회
     * - 주최자(해당 이벤트 host) 또는 신청자(pb.user_id)만 조회 가능
     */
    public BoothApplicationDetailResponse getBoothApplicationDetail(Long viewerId, Long pctBoothId) {
        BoothApplicationDetailResponse res = repo.findBoothDetail(viewerId, pctBoothId);
        if (res == null) {
            // NOT_FOUND / FORBIDDEN 을 구분하기 어렵지만, 현재 쿼리는 권한 포함 조건이라 null이면 둘 중 하나
            throw AuthException.forbidden("BOOTH_FORBIDDEN", "조회 권한이 없거나 신청 내역을 찾을 수 없습니다.");
        }
        return res;
    }

    /**
     * ✅ (주최자) 승인
     * - '결제완료' 같은 상태는 화면에서 쓰지 않지만, DB에는 남아있을 수 있어요.
     * - 그래서 승인/반려/취소가 아닌 상태는 모두 '대기'로 보고 승인 가능하게 처리합니다.
     */
    @Transactional
    public void approveBooth(Long hostUserId, Long pctBoothId) {
        int updated = repo.updateBoothStatusAsHostRelaxed(hostUserId, pctBoothId, "승인");
        if (updated == 0) {
            throw AuthException.badRequest("BOOTH_CANNOT_APPROVE", "승인할 수 없는 상태이거나 처리 권한이 없습니다.");
        }
    }

    /**
     * ✅ (주최자) 반려 + (결제 환불 처리: 결제 모듈 연동 지점)
     */
    @Transactional
    public void rejectBooth(Long hostUserId, Long pctBoothId) {
        int updated = repo.updateBoothStatusAsHostRelaxed(hostUserId, pctBoothId, "반려");
        if (updated == 0) {
            throw AuthException.badRequest("BOOTH_CANNOT_REJECT", "반려할 수 없는 상태이거나 처리 권한이 없습니다.");
        }

        // TODO: 결제 환불 처리 연동
        // - 결제 테이블 / PG 환불 API가 준비되면 여기서 pctBoothId 기준으로 환불 요청
    }
}
