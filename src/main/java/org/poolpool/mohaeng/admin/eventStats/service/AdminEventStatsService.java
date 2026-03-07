package org.poolpool.mohaeng.admin.eventStats.service;

import org.poolpool.mohaeng.admin.eventStats.dto.AdminEventStatsDto;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface AdminEventStatsService {

    Page<AdminEventStatsDto.EventListResponse> getAllEvent(
        String keyword, Integer categoryId, String status,
        Long regionId, LocalDate startDate, LocalDate endDate,
        boolean checkFree, boolean hideClosed, int page, int size
    );

    AdminEventStatsDto.EventAnalysisDetailResponse getEventAnalysis(Long eventId);

    List<AdminEventStatsDto.MonthlyStatsResponse> getEventCountByMonth(int year);

    List<AdminEventStatsDto.CategoryStatsResponse> getEventCountByCategory();

    // ✅ 추가
    Page<AdminEventStatsDto.ParticipantListResponse> getEventParticipants(Long eventId, int page, int size);
}
