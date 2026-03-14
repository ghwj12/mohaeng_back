package org.poolpool.mohaeng.admin.eventmoderation.controller;

import org.poolpool.mohaeng.admin.eventmoderation.dto.AdminEventModerationDetailDto;
import org.poolpool.mohaeng.admin.eventmoderation.dto.AdminEventModerationListItemDto;
import org.poolpool.mohaeng.admin.eventmoderation.service.AdminEventModerationService;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/events/moderation")
@RequiredArgsConstructor
public class AdminEventModerationController {

    private final AdminEventModerationService adminEventModerationService;

    @GetMapping
    public ApiResponse<PageResponse<AdminEventModerationListItemDto>> getList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(
                "행사 검수 목록 조회 성공",
                adminEventModerationService.getList(pageable)
        );
    }

    @GetMapping("/{eventId}")
    public ApiResponse<AdminEventModerationDetailDto> getDetail(
            @PathVariable(name = "eventId") long eventId
    ) {
        return ApiResponse.ok(
                "행사 검수 상세 조회 성공",
                adminEventModerationService.getDetail(eventId)
        );
    }

    @PutMapping("/{eventId}/approve")
    public ApiResponse<Void> approve(
            @PathVariable(name = "eventId") long eventId
    ) {
        adminEventModerationService.approve(eventId);
        return ApiResponse.ok("행사를 승인했습니다.", null);
    }

    @PutMapping("/{eventId}/reject")
    public ApiResponse<Void> reject(
            @PathVariable(name = "eventId") long eventId
    ) {
        adminEventModerationService.reject(eventId);
        return ApiResponse.ok("행사를 반려했습니다.", null);
    }
}