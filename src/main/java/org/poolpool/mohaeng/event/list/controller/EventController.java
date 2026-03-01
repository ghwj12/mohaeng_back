package org.poolpool.mohaeng.event.list.controller;

import java.time.LocalDate;
import java.util.List;

import org.poolpool.mohaeng.event.list.dto.EventDailyCountDto;
import org.poolpool.mohaeng.event.list.dto.EventDetailDto;
import org.poolpool.mohaeng.event.list.dto.EventDto;
import org.poolpool.mohaeng.event.list.dto.EventRegionCountDto;
import org.poolpool.mohaeng.event.list.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/search")
    public ResponseEntity<Page<EventDto>> searchEvents(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "regionId", required = false) Long regionId,
            @RequestParam(name = "filterStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filterStart,
            @RequestParam(name = "filterEnd", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filterEnd,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "topicIds", required = false) List<String> topicIds,
            @RequestParam(name = "checkFree", defaultValue = "false") boolean checkFree,
            @RequestParam(name = "hideClosed", defaultValue = "false") boolean hideClosed,
            @RequestParam(name = "eventStatus", required = false) String eventStatus, // ✅ Issue 5
            @RequestParam(name = "status", required = false) String status,           // 기존 호환
            @PageableDefault(size = 12) Pageable pageable) {

        // eventStatus 또는 status 파라미터 중 하나를 사용
        String resolvedStatus = (eventStatus != null && !eventStatus.isBlank()) ? eventStatus : status;

        Page<EventDto> result = eventService.searchEvents(
                keyword, regionId, filterStart, filterEnd, categoryId, topicIds,
                checkFree, hideClosed, resolvedStatus, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailDto> getEventDetail(
            @PathVariable("eventId") Long eventId,
            @CookieValue(name = "viewedEvents", required = false) String viewedEvents) {

        boolean isViewed = (viewedEvents != null && viewedEvents.contains("[" + eventId + "]"));
        EventDetailDto detail = eventService.getEventDetail(eventId, !isViewed);

        if (!isViewed) {
            String newValue = (viewedEvents == null ? "" : viewedEvents) + "[" + eventId + "]";
            ResponseCookie cookie = ResponseCookie.from("viewedEvents", newValue)
                    .path("/")
                    .maxAge(60 * 60 * 24)
                    .httpOnly(true)
                    .secure(false)
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(detail);
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/counts")
    public ResponseEntity<List<EventRegionCountDto>> getEventCountsByRegion() {
        return ResponseEntity.ok(eventService.getEventCountsByRegion());
    }

    @GetMapping("/calendar-counts")
    public ResponseEntity<List<EventDailyCountDto>> getDailyEventCountsByRegion(
            @RequestParam("regionId") Long regionId) {
        return ResponseEntity.ok(eventService.getDailyEventCountsByRegion(regionId));
    }
}
