package org.poolpool.mohaeng.admin.report.service;

import java.util.List;

import org.poolpool.mohaeng.admin.report.dto.AdminReportCreateRequestDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportDetailDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportListItemDto;
import org.poolpool.mohaeng.admin.report.entity.AdminReportFEntity;
import org.poolpool.mohaeng.admin.report.repository.AdminReportRepository;
import org.poolpool.mohaeng.admin.report.type.ReportResult;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final AdminReportRepository reportRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

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

        // 신고자에게 승인 알림(6)
        notificationService.create(r.getReporterId(), NotiTypeId.REPORT_ACCEPT, r.getEventId(), null);

        // 주최자에게 신고 승인 알림(5)
        if (event.getHost() != null && event.getHost().getUserId() != null) {
            long hostUserId = event.getHost().getUserId();
            if (hostUserId != r.getReporterId()) {
                notificationService.create(hostUserId, NotiTypeId.REPORT_RECEIVER, r.getEventId(), null);
            }
        }

        // 이벤트 비활성화
        event.changeStatusToDeleted();

        //  삭제하지 말고 승인 상태로만 변경
        r.setReportResult(ReportResult.APPROVED);

        //  같은 이벤트 다른 미처리 신고들은 반려로 내려서 아래 정렬
        reportRepository.rejectOtherPendings(r.getEventId(), r.getReportId());
    }

    @Override
    @Transactional
    public void reject(long reportId) {
        AdminReportFEntity r = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("신고가 존재하지 않습니다."));

        if (!ReportResult.PENDING.equals(r.getReportResult())) {
            throw new IllegalStateException("이미 처리된 신고입니다.");
        }

        // 신고자에게 반려 알림(7)
        notificationService.create(r.getReporterId(), NotiTypeId.REPORT_REJECT, r.getEventId(), null);

        //  삭제하지 말고 반려로만 변경
        r.setReportResult(ReportResult.REJECTED);
    }

    private String getEventNameSafe(Long eventId) {
        return eventRepository.findById(eventId)
            .map(EventEntity::getTitle)
            .orElse("(삭제/미존재 이벤트)");
    }

    private String getEventThumbSafe(Long eventId) {
        return eventRepository.findById(eventId)
            .map(EventEntity::getThumbnail) // EventEntity.thumbnail
            .orElse(null);
    }
}