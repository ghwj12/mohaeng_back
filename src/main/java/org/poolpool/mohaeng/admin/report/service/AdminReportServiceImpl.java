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

        // 6) 신고자 승인 알림 (REASON_CATEGORY 치환 위해 reportId 저장)
        notificationService.create(r.getReporterId(), NotiTypeId.REPORT_ACCEPT, r.getEventId(), r.getReportId());

        // 5) 주최자 알림 (템플릿에 REASON_CATEGORY 넣고 싶으면 reportId 필요)
        if (event.getHost() != null && event.getHost().getUserId() != null) {
            long hostUserId = event.getHost().getUserId();
            if (hostUserId != r.getReporterId()) {
                notificationService.create(hostUserId, NotiTypeId.REPORT_RECEIVER, r.getEventId(), r.getReportId());
            }
        }

        //  이벤트 상태는 REPORT_DELETED
        event.changeStatusToReportDeleted();

        //  신고 결과는 APPROVED (3개만)
        r.setReportResult(ReportResult.APPROVED);

        // 같은 이벤트 다른 미처리 신고들 반려 정리
        reportRepository.rejectOtherPendings(r.getEventId(), r.getReportId());

        // 전액 환불 + 11번 알림(환불 성공자만)
        sendRefundNoti11OnReportApproved(r.getEventId(), r.getReportId());

        // 무료 참여자(참여확정)에게 12번 알림
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

        // 7) 신고자 반려 알림 (REASON_CATEGORY 치환 위해 reportId 저장)
        notificationService.create(r.getReporterId(), NotiTypeId.REPORT_REJECT, r.getEventId(), r.getReportId());

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

    private void sendRefundNoti11OnReportApproved(Long eventId, Long reportId) {
        Set<Long> refundedUserIds = new HashSet<>();

        // 부스: 결제완료/승인만
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

        // 참여: 결제완료만
        List<Object[]> paidRows = em.createQuery(
                "select p.pctId, p.userId from EventParticipationEntity p " +
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

        for (Long uid : refundedUserIds) {
            try {
                notificationService.create(uid, NotiTypeId.REPORT_REFUND, eventId, reportId);
            } catch (Exception e) {
                log.error("[REPORT_REFUND_NOTI] failed uid={} eventId={} reportId={}", uid, eventId, reportId, e);
            }
        }
    }

    private void sendPctCancelNoti12ForFreeParticipants(Long eventId, Long reportId) {
        try {
            List<Long> freeUserIds = em.createQuery(
                    "select distinct p.userId from EventParticipationEntity p " +
                    "where p.eventId = :eventId and p.pctStatus = '참여확정'",
                    Long.class
            ).setParameter("eventId", eventId)
             .getResultList();

            for (Long uid : freeUserIds) {
                notificationService.create(uid, NotiTypeId.REPORT_PCTCANCEL, eventId, reportId);
            }
        } catch (Exception e) {
            log.error("[REPORT_PCTCANCEL_NOTI] failed eventId={} reportId={}", eventId, reportId, e);
        }
    }

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
            log.error("[REPORT_REFUND] failed paymentKey={} remaining={}", p.getPaymentKey(), remaining, e);
            return false;
        }
    }
}