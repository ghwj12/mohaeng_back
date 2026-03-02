package org.poolpool.mohaeng.event.mypage.controller;

import java.util.List;

import org.poolpool.mohaeng.event.mypage.dto.BoothApplicationDetailResponse;
import org.poolpool.mohaeng.event.mypage.dto.BoothMypageResponse;
import org.poolpool.mohaeng.event.mypage.service.MypageBoothService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage/events")
public class MypageBoothController {

    private final MypageBoothService service;

    /**
     * ✅ 이 프로젝트의 JwtAuthenticationFilter는 Authentication principal에 'userId(String)'을 넣습니다.
     *    (UsernamePasswordAuthenticationToken(userId, null, ...))
     * 그래서 Authentication#getName() 을 Long으로 파싱해서 사용합니다.
     */
    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            // 혹시라도 principal이 다른 타입/포맷으로 들어왔을 때를 대비
            throw new IllegalStateException("인증 정보가 올바르지 않습니다.");
        }
    }

    /** ✅ 부스 관리(내 신청 내역) */
    @GetMapping("/booths")
    public ResponseEntity<List<BoothMypageResponse>> myBooths(Authentication authentication) {
        Long uid = currentUserId(authentication);
        return ResponseEntity.ok(service.getMyBooths(uid));
    }

    /** ✅ 부스 관리(주최자: 받은 부스) */
    @GetMapping("/booths/received")
    public ResponseEntity<List<BoothMypageResponse>> receivedBooths(Authentication authentication) {
        Long uid = currentUserId(authentication);
        return ResponseEntity.ok(service.getReceivedBooths(uid));
    }

    /** ✅ 부스 신청서 상세(주최자/신청자 모두 조회) */
    @GetMapping("/booths/{pctBoothId}")
    public ResponseEntity<BoothApplicationDetailResponse> boothDetail(
            Authentication authentication,
            @PathVariable("pctBoothId") Long pctBoothId) {
        Long uid = currentUserId(authentication);
        return ResponseEntity.ok(service.getBoothApplicationDetail(uid, pctBoothId));
    }

    /** ✅ (주최자) 승인 */
    @PutMapping("/booths/{pctBoothId}/approve")
    public ResponseEntity<Void> approve(Authentication authentication, @PathVariable("pctBoothId") Long pctBoothId) {
        Long uid = currentUserId(authentication);
        service.approveBooth(uid, pctBoothId);
        return ResponseEntity.ok().build();
    }

    /** ✅ (주최자) 반려 + 환불처리(결제 모듈 연동 지점) */
    @PutMapping("/booths/{pctBoothId}/reject")
    public ResponseEntity<Void> reject(Authentication authentication, @PathVariable("pctBoothId") Long pctBoothId) {
        Long uid = currentUserId(authentication);
        service.rejectBooth(uid, pctBoothId);
        return ResponseEntity.ok().build();
    }
}
