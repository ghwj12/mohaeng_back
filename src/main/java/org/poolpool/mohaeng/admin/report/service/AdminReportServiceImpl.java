package org.poolpool.mohaeng.admin.report.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.poolpool.mohaeng.admin.report.dto.AdminReportCreateRequestDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportDetailDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportListItemDto;
import org.poolpool.mohaeng.admin.report.entity.AdminReportFEntity;
import org.poolpool.mohaeng.admin.report.repository.AdminReportRepository;
import org.poolpool.mohaeng.admin.report.type.ReportResult;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.poolpool.mohaeng.payment.entity.PaymentEntity;
import org.poolpool.mohaeng.payment.repository.PaymentRepository;
import org.poolpool.mohaeng.payment.service.PaymentService;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private static final Logger log = LoggerFactory.getLogger(AdminReportServiceImpl.class);

    private final AdminReportRepository reportRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private final EventParticipationRepository participationRepository;

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReportListItemDto> getList(Pageable pageable) {
        Page<AdminReportFEntity> page = reportRepository.findAllForAdminOrder(pageable);

        List<AdminReportListItemDto> items = page.getContent().stream()
            .map(r -> AdminReportListItemDto.fromEntity(
                r,
                getEventNameSafe(r.getEventId()),
                getEventThumbSafe(r.getEventId())
            ))
            .toList();

        return new PageResponse<>(
            items,
            pageable.getPageNumber(),
            pageable.getPageSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReportDetailDto getDetail(long reportId) {
        AdminReportFEntity r = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("신고가 존재하지 않습니다."));

        String eventName = getEventNameSafe(r.getEventId());
        String reporterName = userRepository.findById(r.getReporterId())
            .map(u -> u.getName())
            .orElse("(알 수 없음)");

        return AdminReportDetailDto.fromEntity(r, eventName, reporterName);
    }

    @Override
    @Transactional
    public long create(long reporterId, AdminReportCreateRequestDto request) {
        Long eventId = request.getEventId();

        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("존재하지 않는 이벤트입니다.");
        }

        if (reportRepository.existsByReporterIdAndEventId(reporterId, eventId)) {
            throw new IllegalStateException("이미 해당 이벤트를 신고했습니다.");
        }

        AdminReportFEntity r = AdminReportFEntity.builder()
            .eventId(eventId)
            .reporterId(reporterId)
            .reasonCategory(request.getReasonCategory())
            .reasonDetailText(request.getReasonDetailText())
            .reportResult(ReportResult.PENDING)
            .build();

        return reportRepository.save(r).getReportId();
    }

    @Override
    @Transactional
    public void approve(long reportId) {
        AdminReportFEntity r = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("신고가 존재하지 않습니다."));

        if (!ReportResult.PENDING.equals(r.getReportResult())) {
            throw new IllegalStateException("이미 처리된 신고입니다.");
        }

        EventEntity event = eventRepository.findById(r.getEventId())
            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이벤트입니다."));

        // 신고자 승인 알림(6)
        notificationService.create(r.getReporterId(), NotiTypeId.REPORT_ACCEPT, r.getEventId(), null);

        // 주최자 신고 승인 알림(5)
        if (event.getHost() != null && event.getHost().getUserId() != null) {
            long hostUserId = event.getHost().getUserId();
            if (hostUserId != r.getReporterId()) {
                notificationService.create(hostUserId, NotiTypeId.REPORT_RECEIVER, r.getEventId(), null);
            }
        }

        // 이벤트 비활성화(삭제)
        event.changeStatusToDeleted();

        // 승인으로 이벤트 비활성화되면 reportResult = REPORT_DELETED
        r.setReportResult(ReportResult.REPORT_DELETED);

        // 같은 이벤트 다른 미처리 신고 반려 정리
        reportRepository.rejectOtherPendings(r.getEventId(), r.getReportId());

        // 전액 환불 + 환불 성공자에게만 11번 알림
        sendRefundNoti11OnReportApproved(r.getEventId(), r.getReportId());

        //  추가: 무료 참여자(참여확정)에게 12번 알림
        sendPctCancelNoti12ForFreeParticipants(r.getEventId(), r.getReportId());
    }

    @Override
    @Transactional
    public void reject(long reportId) {
        AdminReportFEntity r = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("신고가 존재하지 않습니다."));

        if (!ReportResult.PENDING.equals(r.getReportResult())) {
            throw new IllegalStateException("이미 처리된 신고입니다.");
        }

        // 신고자 반려 알림(7)
        notificationService.create(r.getReporterId(), NotiTypeId.REPORT_REJECT, r.getEventId(), null);

        r.setReportResult(ReportResult.REJECTED);
    }

    private String getEventNameSafe(Long eventId) {
        return eventRepository.findById(eventId)
            .map(EventEntity::getTitle)
            .orElse("(삭제/미존재 이벤트)");
    }

    private String getEventThumbSafe(Long eventId) {
        return eventRepository.findById(eventId)
            .map(EventEntity::getThumbnail)
            .orElse(null);
    }

    /**
     * 신고 승인(행사 삭제) 시:
     * - 전액 환불(남은 금액 전체)
     * - 환불 성공자에게만 11번(REPORT_REFUND) 알림
     */
    private void sendRefundNoti11OnReportApproved(Long eventId, Long reportId) {
        Set<Long> refundedUserIds = new HashSet<>();

        // 1) 부스 환불: 결제완료/승인만
        List<ParticipationBoothEntity> booths = participationRepository.findBoothsByEventId(eventId);
        for (ParticipationBoothEntity b : booths) {
            if (b == null || b.getPctBoothId() == null || b.getUserId() == null) continue;

            String st = b.getStatus();
            if (!"결제완료".equals(st) && !"승인".equals(st)) continue;

            PaymentEntity pay = paymentRepository.findByPctBoothId(b.getPctBoothId()).orElse(null);
            if (refundAllRemainingIfPossible(pay, "신고 승인(행사 삭제) - 부스 전액 환불")) {
                refundedUserIds.add(b.getUserId());
            }
        }

        // 2) 참여 환불: 결제완료 참여만 (userId까지 같이 뽑음)
        List<Object[]> paidRows = em.createQuery(
                "select p.pctId, p.userId " +
                "from EventParticipationEntity p " +
                "where p.eventId = :eventId and p.pctStatus = '결제완료'",
                Object[].class
        ).setParameter("eventId", eventId)
         .getResultList();

        for (Object[] row : paidRows) {
            Long pctId = (Long) row[0];
            Long userId = (Long) row[1];
            if (pctId == null || userId == null) continue;

            PaymentEntity pay = paymentRepository.findByPctId(pctId).orElse(null);
            if (refundAllRemainingIfPossible(pay, "신고 승인(행사 삭제) - 참여 전액 환불")) {
                refundedUserIds.add(userId);
            }
        }

        // 3) 환불 성공자에게만 11번 알림
        for (Long uid : refundedUserIds) {
            try {
                notificationService.create(uid, NotiTypeId.REPORT_REFUND, eventId, reportId);
            } catch (Exception e) {
                log.error("[REPORT_REFUND_NOTI] create failed type=11 uid={} eventId={} reportId={}",
                        uid, eventId, reportId, e);
            }
        }

        log.info("[REPORT_REFUND] done eventId={} reportId={} refundedUserCount={}",
                eventId, reportId, refundedUserIds.size());
    }

    /**
     *  무료 행사 참여자(참여확정)에게 12번(REPORT_PCTCANCEL) 알림
     */
    private void sendPctCancelNoti12ForFreeParticipants(Long eventId, Long reportId) {
        try {
            List<Long> freeUserIds = em.createQuery(
                    "select distinct p.userId " +
                    "from EventParticipationEntity p " +
                    "where p.eventId = :eventId and p.pctStatus = '참여확정'",
                    Long.class
            ).setParameter("eventId", eventId)
             .getResultList();

            for (Long uid : freeUserIds) {
                notificationService.create(uid, NotiTypeId.REPORT_PCTCANCEL, eventId, reportId);
            }

            log.info("[REPORT_PCTCANCEL_NOTI] sent type=12 eventId={} reportId={} targetCount={}",
                    eventId, reportId, freeUserIds.size());
        } catch (Exception e) {
            log.error("[REPORT_PCTCANCEL_NOTI] failed type=12 eventId={} reportId={}", eventId, reportId, e);
        }
    }

    /**
     * 전액 환불 = 남은 금액( amountTotal - canceledAmount ) 전부 환불
     * - APPROVED / PARTIAL_CANCEL만 대상
     * - CANCELLED면 스킵
     */
    private boolean refundAllRemainingIfPossible(PaymentEntity p, String reason) {
        if (p == null) return false;
        if (p.getPaymentKey() == null || p.getPaymentKey().isBlank()) return false;

        String status = (p.getPaymentStatus() == null) ? "" : p.getPaymentStatus().toUpperCase();

        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return false;
        if (!"APPROVED".equals(status) && !"PARTIAL_CANCEL".equals(status)) return false;

        int total = (p.getAmountTotal() == null) ? 0 : p.getAmountTotal();
        int canceled = (p.getCanceledAmount() == null) ? 0 : p.getCanceledAmount();
        int remaining = total - canceled;

        if (remaining <= 0) return false;

        try {
            paymentService.cancelPayment(p.getPaymentKey(), remaining, reason);
            return true;
        } catch (Exception e) {
            log.error("[REPORT_REFUND] refund failed paymentKey={} remaining={} reason={}",
                    p.getPaymentKey(), remaining, reason, e);
            return false;
        }
    }
}