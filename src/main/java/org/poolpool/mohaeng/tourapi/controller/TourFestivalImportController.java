package org.poolpool.mohaeng.tourapi.controller;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.tourapi.dto.TourFestivalImportRequest;
import org.poolpool.mohaeng.tourapi.service.TourFestivalImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tour/festivals")
@RequiredArgsConstructor
public class TourFestivalImportController {

    private final TourFestivalImportService tourFestivalImportService;

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importFestivals(
            @RequestBody(required = false) TourFestivalImportRequest request
    ) {
        TourFestivalImportRequest safeRequest =
                request != null ? request : new TourFestivalImportRequest();

        Map<String, Object> result = tourFestivalImportService.importFestivals(safeRequest);

        return ResponseEntity.ok(
                ApiResponse.ok("TourAPI 행사 import 성공", result)
        );
    }
}
