package org.poolpool.mohaeng.admin.eventStats.controller;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.admin.eventStats.dto.AdminEventStatsDto;
import org.poolpool.mohaeng.admin.eventStats.service.AdminEventStatsService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/eventstats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventStatsController {

    private final AdminEventStatsService service;

    // ── 행사 목록 (페이징 + 필터) ──
    @GetMapping("/events")
    public ResponseEntity<Page<AdminEventStatsDto.EventListResponse>> getEventList(
        @RequestParam(name = "keyword",    required = false) String keyword,
        @RequestParam(name = "categoryId", required = false) Integer categoryId,
        @RequestParam(name = "status",     required = false) String status,
        @RequestParam(name = "regionId",   required = false) Long regionId,
        @RequestParam(name = "startDate",  required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(name = "endDate",    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(name = "checkFree",  defaultValue = "false") boolean checkFree,
        @RequestParam(name = "hideClosed", defaultValue = "false") boolean hideClosed,
        @RequestParam(name = "page",       defaultValue = "0") int page,
        @RequestParam(name = "size",       defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.getAllEvent(
            keyword, categoryId, status, regionId,
            startDate, endDate, checkFree, hideClosed, page, size
        ));
    }

    // ── 단일 행사 상세 분석 ──
    @GetMapping("/events/{eventId}/analysis")
    public ResponseEntity<AdminEventStatsDto.EventAnalysisDetailResponse> getEventAnalysis(
        @PathVariable("eventId") Long eventId
    ) {
        return ResponseEntity.ok(service.getEventAnalysis(eventId));
    }

    // ── ✅ 참여자 목록 (페이징) ──
    @GetMapping("/events/{eventId}/participants")
    public ResponseEntity<Page<AdminEventStatsDto.ParticipantListResponse>> getEventParticipants(
        @PathVariable("eventId") Long eventId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.getEventParticipants(eventId, page, size));
    }

    // ── 월별 통계 ──
    @GetMapping("/monthly")
    public ResponseEntity<List<AdminEventStatsDto.MonthlyStatsResponse>> getMonthlyStats(
        @RequestParam(name = "year", required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(service.getEventCountByMonth(targetYear));
    }

    // ── 카테고리별 통계 ──
    @GetMapping("/category")
    public ResponseEntity<List<AdminEventStatsDto.CategoryStatsResponse>> getCategoryStats() {
        return ResponseEntity.ok(service.getEventCountByCategory());
    }
}
