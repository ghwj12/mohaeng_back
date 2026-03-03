package org.poolpool.mohaeng.event.inquiry.service;

import java.time.LocalDateTime;
import java.util.List;

import org.poolpool.mohaeng.event.inquiry.dto.EventInquiryDto;
import org.poolpool.mohaeng.event.inquiry.dto.InquiryMypageResponse;
import org.poolpool.mohaeng.event.inquiry.entity.EventInquiryEntity;
import org.poolpool.mohaeng.event.inquiry.repository.EventInquiryRepository;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.notification.service.NotificationService;   //  추가
import org.poolpool.mohaeng.notification.type.NotiTypeId;            //  추가
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventInquiryServiceImpl implements EventInquiryService {

    private final EventInquiryRepository repo;
    private final EventRepository eventRepository;
    private final NotificationService notificationService; //  추가

    @Override
    @Transactional(readOnly = true)
    public List<EventInquiryDto> getInquiryList(Long eventId) {
        return repo.findInquiryListDtoByEventId(eventId);
    }

    @Override
    @Transactional
    public Long createInquiry(Long currentUserId, Long eventId, EventInquiryDto dto) {
        if (currentUserId == null) throw new IllegalStateException("로그인이 필요합니다.");

        //  행사 조회(주최자 알림을 위해)
        EventEntity ev = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("행사 없음"));

        dto.setUserId(currentUserId);
        dto.setEventId(eventId);
        if (dto.getStatus() == null) dto.setStatus("대기");

        EventInquiryEntity saved = repo.save(dto.toEntity());

        //  (알림) 문의 등록 → 주최자에게 알림
        Long hostId = (ev.getHost() != null) ? ev.getHost().getUserId() : null;
        if (hostId != null && !hostId.equals(currentUserId)) {
            // reportId는 event_report FK 때문에 절대 넣으면 안 됨 → null 고정
            notificationService.create(hostId, NotiTypeId.INQUIRY_RECEIVER, eventId, null);
        }

        return saved.getInqId();
    }

    @Override
    @Transactional
    public void updateInquiry(Long currentUserId, Long inqId, EventInquiryDto dto) {
        if (currentUserId == null) throw new IllegalStateException("로그인이 필요합니다.");

        EventInquiryEntity e = repo.findById(inqId)
                .orElseThrow(() -> new IllegalArgumentException("문의 없음"));

        if (!e.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("본인 문의만 수정할 수 있습니다.");
        }

        e.setContent(dto.getContent());
        repo.save(e);
    }

    @Override
    @Transactional
    public void deleteInquiry(Long currentUserId, Long inqId) {
        if (currentUserId == null) throw new IllegalStateException("로그인이 필요합니다.");

        EventInquiryEntity e = repo.findById(inqId)
                .orElseThrow(() -> new IllegalArgumentException("문의 없음"));

        if (!e.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("본인 문의만 삭제할 수 있습니다.");
        }

        repo.deleteById(inqId);
    }

    private void assertHostOfEvent(Long eventId, Long currentUserId) {
        if (currentUserId == null) throw new IllegalStateException("로그인이 필요합니다.");

        EventEntity ev = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("행사 없음"));

        Long hostId = (ev.getHost() != null) ? ev.getHost().getUserId() : null;
        if (hostId == null || !hostId.equals(currentUserId)) {
            throw new IllegalArgumentException("답변은 행사 주최자만 작성할 수 있습니다.");
        }
    }

    @Override
    @Transactional
    public void createReply(Long currentUserId, Long inqId, EventInquiryDto dto) {
        EventInquiryEntity e = repo.findById(inqId)
                .orElseThrow(() -> new IllegalArgumentException("문의 없음"));

        assertHostOfEvent(e.getEventId(), currentUserId);

        //  "처음 답변"인지 체크(답변 수정 때 알림 중복 방지)
        boolean wasEmpty = (e.getReplyContent() == null || e.getReplyContent().isBlank());

        e.setReplyContent(dto.getReplyContent());
        e.setReplyId(currentUserId);
        e.setReplyDate(LocalDateTime.now());
        e.setStatus("완료");

        repo.save(e);

        //  (알림) 답변 등록(처음만) → 질문자에게 알림
        if (wasEmpty && e.getUserId() != null) {
            notificationService.create(e.getUserId(), NotiTypeId.INQUIRY_SENDER, e.getEventId(), null);
        }
    }

    @Override
    @Transactional
    public void updateReply(Long currentUserId, Long inqId, EventInquiryDto dto) {
        // 기존 구조 유지(알림은 createReply 내부에서 "처음 답변만" 보냄)
        createReply(currentUserId, inqId, dto);
    }

    @Override
    @Transactional
    public void deleteReply(Long currentUserId, Long inqId) {
        EventInquiryEntity e = repo.findById(inqId)
                .orElseThrow(() -> new IllegalArgumentException("문의 없음"));

        assertHostOfEvent(e.getEventId(), currentUserId);

        e.setReplyContent(null);
        e.setReplyId(null);
        e.setReplyDate(null);
        e.setStatus("대기");

        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public InquiryMypageResponse mypage(Long currentUserId, String tab, int page, int size) {
        if (currentUserId == null) throw new IllegalStateException("로그인이 필요합니다.");

        String t = (tab == null) ? "ALL" : tab.trim().toUpperCase();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        Page<EventInquiryDto> p;
        switch (t) {
            case "WRITTEN":
                p = repo.findWrittenForMypageDto(currentUserId, pageable);
                break;
            case "RECEIVED":
                p = repo.findReceivedForMypageDto(currentUserId, pageable);
                break;
            case "ALL":
            default:
                p = repo.findAllForMypageDto(currentUserId, pageable);
        }

        return InquiryMypageResponse.builder()
                .items(p.getContent())
                .page(p.getNumber())
                .size(p.getSize())
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .build();
    }
}