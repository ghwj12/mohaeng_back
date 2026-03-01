package org.poolpool.mohaeng.event.list.service;

import java.time.LocalDate;
import java.util.List;

import org.poolpool.mohaeng.event.list.dto.EventDailyCountDto;
import org.poolpool.mohaeng.event.list.dto.EventDetailDto;
import org.poolpool.mohaeng.event.list.dto.EventDto;
import org.poolpool.mohaeng.event.list.dto.EventRegionCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {

    EventDetailDto getEventDetail(Long eventId, boolean shouldIncreaseView);

    // ✅ Issue 5: eventStatus 파라미터 추가 — EventServiceImpl과 시그니처 일치 필수
    Page<EventDto> searchEvents(
            String keyword, Long regionId, LocalDate filterStart, LocalDate filterEnd,
            Integer categoryId, List<String> topicIds,
            boolean checkFree, boolean hideClosed, String eventStatus, Pageable pageable);

    List<EventRegionCountDto> getEventCountsByRegion();

    List<EventDailyCountDto> getDailyEventCountsByRegion(Long regionId);
}
