package org.poolpool.mohaeng.event.participation.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    // ══════════════════════════════════════
    //   마이페이지: 내 행사 참여 목록 (MypageEventController 호출)
    // ══════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<EventParticipationDto> getParticipationList(Long userId) {
        return participationRepository.findParticipationsByUserId(userId)
                .stream()
                .map(EventParticipationDto::fromEntity)
                .toList();
    }

    // ══════════════════════════════════════
    //   일반 행사 참여 신청
    // ══════════════════════════════════════

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

    // ══════════════════════════════════════
    //   일반 행사 참여 취소 + 환불 (문제 7)
    // ══════════════════════════════════════

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
        log.info("[행사 참여 취소] pctId={}, refundRate={}%", pctId, refundRate);
    }

    // ══════════════════════════════════════
    //   부스 취소 + 환불 + 재고 복원
    // ══════════════════════════════════════

    @Override
    public void cancelBoothParticipation(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        if (Set.of("승인", "반려").contains(booth.getStatus())) {
            throw new IllegalStateException("승인 또는 반려된 부스는 취소할 수 없습니다.");
        }

        Long eventId = getEventIdFromHostBooth(booth.getHostBoothId());
        int refundRate = calcRefundRate(findEvent(eventId).getStartDate());

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPctBoothId(pctBoothId);
        refundIfPaid(paymentOpt, refundRate, "참가자 부스 취소");
        restoreInventory(pctBoothId, booth.getHostBoothId());

        booth.setStatus("취소");
        participationRepository.saveBooth(booth);
        log.info("[부스 취소] pctBoothId={}, refundRate={}%", pctBoothId, refundRate);
    }

    // ══════════════════════════════════════
    //   부스 승인
    // ══════════════════════════════════════

    @Override
    public void approveBooth(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        if (!Set.of("신청", "결제완료").contains(booth.getStatus())) {
            throw new IllegalStateException("신청/결제완료 상태에서만 승인 가능합니다.");
        }

        booth.setStatus("승인");
        booth.setApprovedDate(java.time.LocalDateTime.now());
        participationRepository.saveBooth(booth);
        log.info("[부스 승인] pctBoothId={}", pctBoothId);
    }

    // ══════════════════════════════════════
    //   부스 반려 - 100% 환불 + 재고 복원 (문제 1)
    // ══════════════════════════════════════

    @Override
    public void rejectBooth(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        if (!Set.of("신청", "결제완료").contains(booth.getStatus())) {
            throw new IllegalStateException("신청/결제완료 상태에서만 반려 가능합니다.");
        }

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPctBoothId(pctBoothId);
        refundIfPaid(paymentOpt, 100, "주최자 반려");
        restoreInventory(pctBoothId, booth.getHostBoothId());

        booth.setStatus("반려");
        participationRepository.saveBooth(booth);
        log.info("[부스 반려] pctBoothId={} → 100% 환불 + 재고 복원 완료", pctBoothId);
    }

    // ══════════════════════════════════════
    //   중복 신청 여부 (EventParticipationCheckController 호출)
    // ══════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveParticipation(Long eventId) {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        return participationRepository.existsActiveParticipation(userId, eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveBooth(Long eventId) {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        return participationRepository.existsActiveBoothParticipation(userId, eventId);
    }

    // ══════════════════════════════════════
    //   미사용
    // ══════════════════════════════════════

    @Override
    public Long submitBoothParticipation(Long userId, Long eventId, Object dto) {
        throw new UnsupportedOperationException("컨트롤러에서 직접 처리");
    }

    // ══════════════════════════════════════
    //   공통 유틸
    // ══════════════════════════════════════

    private int calcRefundRate(LocalDate eventStartDate) {
        if (eventStartDate == null) return 0;
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), eventStartDate);
        if (daysLeft >= 30) return 100;
        if (daysLeft >= 15) return 80;
        if (daysLeft >= 7)  return 50;
        if (daysLeft >= 3)  return 30;
        return 0;
    }

    private void refundIfPaid(Optional<PaymentEntity> paymentOpt, int rate, String reason) {
        paymentOpt.ifPresent(payment -> {
            if (!"APPROVED".equals(payment.getPaymentStatus())) return;
            if (payment.getPaymentKey() == null) return;
            int cancelAmount = payment.getAmountTotal() * rate / 100;
            if (cancelAmount <= 0) {
                log.info("[환불 생략] rate={}%, amount=0", rate);
                return;
            }
            paymentService.cancelPayment(payment.getPaymentKey(), cancelAmount, reason);
            log.info("[환불 완료] paymentKey={}, cancelAmount={}원 ({}%), reason={}",
                    payment.getPaymentKey(), cancelAmount, rate, reason);
        });
    }

    private void restoreInventory(Long pctBoothId, Long hostBoothId) {
        if (hostBoothId != null) {
            participationRepository.increaseBoothRemainCount(hostBoothId);
            log.info("[재고 복원] hostBoothId={} remainCount+1", hostBoothId);
        }

        List<ParticipationBoothFacilityEntity> facilities =
                participationRepository.findFacilitiesByPctBoothId(pctBoothId);

        for (ParticipationBoothFacilityEntity faci : facilities) {
            if (faci.getHostBoothFaciId() != null
                    && faci.getFaciCount() != null
                    && faci.getFaciCount() > 0) {
                participationRepository.increaseFacilityRemainCount(
                        faci.getHostBoothFaciId(), faci.getFaciCount());
                log.info("[시설 재고 복원] faciId={}, count+{}",
                        faci.getHostBoothFaciId(), faci.getFaciCount());
            }
        }
    }

    private Long getEventIdFromHostBooth(Long hostBoothId) {
        return hostBoothRepository.findById(hostBoothId)
                .map(hb -> hb.getEventId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "호스트 부스 정보를 찾을 수 없습니다. hostBoothId=" + hostBoothId));
    }

    private EventEntity findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "행사 정보를 찾을 수 없습니다. eventId=" + eventId));
    }

    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            return Long.parseLong(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
