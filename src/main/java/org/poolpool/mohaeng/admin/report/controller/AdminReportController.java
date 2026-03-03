package org.poolpool.mohaeng.admin.report.controller;

import org.poolpool.mohaeng.admin.report.dto.AdminReportDetailDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportListItemDto;
import org.poolpool.mohaeng.admin.report.service.AdminReportService;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminReportListItemDto>>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,   //  name 명시
            @RequestParam(name = "size", defaultValue = "10") int size   //  name 명시
    ) {
        var data = reportService.getList(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("신고 목록 조회 성공", data));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<AdminReportDetailDto>> detail(
            @PathVariable("reportId") long reportId   //  value 명시
    ) {
        var data = reportService.getDetail(reportId);
        return ResponseEntity.ok(ApiResponse.ok("신고 상세 조회 성공", data));
    }

    @PutMapping("/{reportId}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable("reportId") long reportId   //  value 명시
    ) {
        reportService.approve(reportId);
        return ResponseEntity.ok(ApiResponse.ok("신고 승인 처리 성공", null));
    }

    @PutMapping("/{reportId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable("reportId") long reportId   //  value 명시
    ) {
        reportService.reject(reportId);
        return ResponseEntity.ok(ApiResponse.ok("신고 반려 처리 성공", null));
    }
}