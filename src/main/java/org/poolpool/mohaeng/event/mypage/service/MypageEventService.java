package org.poolpool.mohaeng.event.mypage.service;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.event.list.dto.EventDto;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.mypage.dto.MyEventsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MypageEventService {

    private final EventRepository eventRepository;

    /**
     * ✅ 내가 등록(주최)한 행사 목록
     */
    public MyEventsResponse getMyCreatedEvents(Long userId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<org.poolpool.mohaeng.event.list.entity.EventEntity> p =
                eventRepository.findByHost_UserIdAndEventStatusNotIn(userId, List.of("DELETED", "REPORT_DELETED", "report_deleted", "행사삭제"), pr);

        List<EventDto> items = p.getContent().stream().map(EventDto::fromEntity).toList();

        return MyEventsResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .build();
    }
/**
 * ✅ 내가 등록(주최)한 행사 소프트 삭제
 * - 행사예정/행사종료(날짜 기준)만 삭제 허용
 * - 삭제 시 eventStatus를 '행사삭제'로 변경
 */
@Transactional
public void deleteMyCreatedEvent(Long userId, Long eventId) {
    var event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("행사 없음"));

    // ✅ 본인(주최자) 행사만 삭제 가능
    if (event.getHost() == null || event.getHost().getUserId() == null || !event.getHost().getUserId().equals(userId)) {
        throw new SecurityException("본인 행사만 삭제할 수 있습니다.");
    }

    LocalDate today = LocalDate.now();
    LocalDate start = event.getStartDate();
    LocalDate end = event.getEndDate();

    boolean deletable = false;
    if (start != null && today.isBefore(start)) deletable = true;       // 예정
    if (end != null && today.isAfter(end)) deletable = true;            // 종료

    if (!deletable) {
        throw new IllegalStateException("행사예정/행사종료 상태에서만 삭제할 수 있습니다.");
    }

    event.setEventStatus("DELETED");
    eventRepository.save(event);
}
}
