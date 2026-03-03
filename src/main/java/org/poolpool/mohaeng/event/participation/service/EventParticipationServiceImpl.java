package org.poolpool.mohaeng.event.participation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.poolpool.mohaeng.auth.security.principal.CustomUserPrincipal;
import org.poolpool.mohaeng.event.host.entity.HostBoothEntity;
import org.poolpool.mohaeng.event.host.repository.HostBoothRepository;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.participation.dto.EventParticipationDto;
import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;
import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.payment.entity.PaymentEntity;
import org.poolpool.mohaeng.payment.repository.PaymentRepository;
import org.poolpool.mohaeng.payment.service.PaymentService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 행사 참여/취소 및 부스 신청 처리(취소/승인/반려) 서비스.
 *
 * ⚠️ 참고
 * - 부스 '신청 제출'은 현재 EventParticipationController에서 직접 엔티티를 저장하고 있어
 *   이 서비스의 submitBoothParticipation()은 "미사용/호환용"으로 남겨둡니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventParticipationServiceImpl implements EventParticipationService {

    private final EventParticipationRepository participationRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final EventRepository eventRepository;
    private final HostBoothRepository hostBoothRepository;

    // ──────────────────────────────────────
    //  공통: 로그인 사용자 ID
    // ──────────────────────────────────────

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof CustomUserPrincipal cup) {
            return Long.parseLong(cup.getUserId());
        }
        if (principal instanceof UserDetails ud) {
            return Long.parseLong(ud.getUsername());
        }
        if (principal instanceof String s) {
            return Long.parseLong(s);
        }

        throw new IllegalStateException("인증 정보를 찾을 수 없습니다. principal=" + principal);
    }

    // ──────────────────────────────────────
    //  마이페이지: 내 행사 참여 목록
    // ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<EventParticipationDto> getParticipationList(Long userId) {
        return participationRepository.findParticipationsByUserId(userId)
                .stream()
                .map(EventParticipationDto::fromEntity)
                .toList();
    }

    // ──────────────────────────────────────
    //  일반 행사 참여
    // ──────────────────────────────────────

    @Override
    public Long submitParticipation(Long userId, Long eventId) {
        if (participationRepository.existsActiveParticipation(userId, eventId)) {
            throw new IllegalStateException("이미 신청한 행사입니다.");
        }

        EventEntity event = findEvent(eventId);
        boolean isPaid = event.getPrice() != null && event.getPrice() > 0;
        String initialStatus = isPaid ? "결제대기" : "참여확정";

        EventParticipationEntity pct = new EventParticipationEntity();
        pct.setEventId(eventId);
        pct.setUserId(userId);
        pct.setPctStatus(initialStatus);

        participationRepository.saveParticipation(pct);
        log.info("[행사 참여 신청] userId={}, eventId={}, status={}", userId, eventId, initialStatus);
        return pct.getPctId();
    }

    @Override
    public void cancelParticipation(Long pctId) {
        EventParticipationEntity pct = participationRepository.findParticipationById(pctId)
                .orElseThrow(() -> new IllegalArgumentException("참여 신청 정보를 찾을 수 없습니다."));

        if (!Set.of("임시저장", "신청", "결제대기", "결제완료", "참여확정").contains(pct.getPctStatus())) {
            throw new IllegalStateException("현재 상태(" + pct.getPctStatus() + ")에서는 취소할 수 없습니다.");
        }

        EventEntity event = findEvent(pct.getEventId());
        int refundRate = calcRefundRate(event.getStartDate());

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPctId(pctId);
        refundIfPaid(paymentOpt, refundRate, "참가자 취소");

        pct.setPctStatus("취소");
        participationRepository.saveParticipation(pct);
        log.info("[행사 참여 취소] pctId={}, refundRate={}%, paymentExists={}", pctId, refundRate, paymentOpt.isPresent());
    }

    // ──────────────────────────────────────
    //  부스 신청 (취소/승인/반려)
    // ──────────────────────────────────────

    /**
     * 현재 프로젝트에서는 부스 신청 저장을 Controller가 직접 수행 중이라
     * 서비스 시그니처 호환을 위해 예외로 막아둡니다.
     */
    @Override
    public Long submitBoothParticipation(Long userId, Long eventId, Object dto) {
        throw new UnsupportedOperationException(
                "부스 신청 제출은 /api/eventParticipation/submitBoothApply 컨트롤러에서 처리합니다.");
    }

    @Override
    public void cancelBoothParticipation(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        // 승인/반려는 주최자 처리로 간주 (취소 방지)
        if (Set.of("승인", "반려").contains(booth.getStatus())) {
            throw new IllegalStateException("승인 또는 반려된 부스는 취소할 수 없습니다.");
        }

        Long eventId = getEventIdFromHostBooth(booth.getHostBoothId());
        int refundRate = calcRefundRate(findEvent(eventId).getStartDate());

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPctBoothId(pctBoothId);
        refundIfPaid(paymentOpt, refundRate, "참가자 부스 취소");

        restoreInventory(pctBoothId, booth.getHostBoothId(), booth.getBoothCount());

        booth.setStatus("취소");
        participationRepository.saveBooth(booth);
        log.info("[부스 취소] pctBoothId={}, refundRate={}%, paymentExists={}", pctBoothId, refundRate, paymentOpt.isPresent());
    }

    @Override
    public void approveBooth(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        if (!Set.of("신청", "결제완료", "결제대기").contains(booth.getStatus())) {
            throw new IllegalStateException("신청/결제 상태에서만 승인 가능합니다.");
        }

        booth.setStatus("승인");
        booth.setApprovedDate(LocalDateTime.now());
        participationRepository.saveBooth(booth);
        log.info("[부스 승인] pctBoothId={}", pctBoothId);
    }

    @Override
    public void rejectBooth(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        if (!Set.of("신청", "결제완료", "결제대기").contains(booth.getStatus())) {
            throw new IllegalStateException("신청/결제 상태에서만 반려 가능합니다.");
        }

        // 반려는 전액 환불 + 재고 복원
        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPctBoothId(pctBoothId);
        refundIfPaid(paymentOpt, 100, "주최자 반려");
        restoreInventory(pctBoothId, booth.getHostBoothId(), booth.getBoothCount());

        booth.setStatus("반려");
        participationRepository.saveBooth(booth);
        log.info("[부스 반려] pctBoothId={} → 100% 환불 + 재고 복원", pctBoothId);
    }

    // ──────────────────────────────────────
    //  중복 신청 여부 확인
    // ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveParticipation(Long eventId) {
        Long userId = getCurrentUserId();
        return participationRepository.existsActiveParticipation(userId, eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveBooth(Long eventId) {
        Long userId = getCurrentUserId();
        return participationRepository.existsActiveBoothParticipation(userId, eventId);
    }

    // ──────────────────────────────────────
    //  내부 유틸
    // ──────────────────────────────────────

    private EventEntity findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("행사를 찾을 수 없습니다. eventId=" + eventId));
    }

    private Long getEventIdFromHostBooth(Long hostBoothId) {
        HostBoothEntity hb = hostBoothRepository.findById(hostBoothId)
                .orElseThrow(() -> new IllegalArgumentException("HostBooth를 찾을 수 없습니다. boothId=" + hostBoothId));
        return hb.getEventId();
    }

    /**
     * 환불률 계산 (시작일 기준 D-day)
     * - D-7 이상: 100%
     * - D-3 이상: 50%
     * - D-1 이상: 30%
     * - 당일/지난 행사: 0%
     */
    private int calcRefundRate(LocalDate startDate) {
        if (startDate == null) return 0;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), startDate);
        if (days >= 7) return 100;
        if (days >= 3) return 50;
        if (days >= 1) return 30;
        return 0;
    }

    private void refundIfPaid(Optional<PaymentEntity> paymentOpt, int refundRate, String reason) {
        if (paymentOpt.isEmpty()) return;

        PaymentEntity p = paymentOpt.get();
        if (p.getPaymentKey() == null || p.getPaymentKey().isBlank()) return;

        // 이미 취소 처리된 결제는 중복 환불 방지
        if ("CANCELED".equalsIgnoreCase(p.getPaymentStatus())) return;

        // 승인 결제만 환불 대상으로 처리
        if (!"APPROVED".equalsIgnoreCase(p.getPaymentStatus())) return;

        int total = p.getAmountTotal() == null ? 0 : p.getAmountTotal();
        int cancelAmount = (int) Math.floor(total * (refundRate / 100.0));
        if (cancelAmount <= 0) return;

        paymentService.cancelPayment(p.getPaymentKey(), cancelAmount, reason);

        // 프로젝트 요구사항상 부분 환불도 '취소'로 정리
        p.setPaymentStatus("CANCELED");
        p.setCanceledAt(LocalDateTime.now());
        paymentRepository.save(p);
    }

    /**
     * 부스/부대시설 재고 복원
     */
    private void restoreInventory(Long pctBoothId, Long hostBoothId, Integer boothCount) {
        // 부대시설 재고 복원
        List<ParticipationBoothFacilityEntity> facilities = participationRepository.findFacilitiesByPctBoothId(pctBoothId);
        for (ParticipationBoothFacilityEntity f : facilities) {
            if (f.getHostBoothFaciId() == null) continue;
            int cnt = (f.getFaciCount() == null || f.getFaciCount() <= 0) ? 1 : f.getFaciCount();
            participationRepository.increaseFacilityRemainCount(f.getHostBoothFaciId(), cnt);
        }

        // 부스 재고 복원 (boothCount만큼)
        int bc = (boothCount == null || boothCount <= 0) ? 1 : boothCount;
        for (int i = 0; i < bc; i++) {
            participationRepository.increaseBoothRemainCount(hostBoothId);
        }
    }
}
