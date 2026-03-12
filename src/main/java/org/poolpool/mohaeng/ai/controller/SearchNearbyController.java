package org.poolpool.mohaeng.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/nearby")
@RequiredArgsConstructor
public class SearchNearbyController {

    private final RestTemplate restTemplate;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    /**
     * POST /api/ai/nearby/course
     * 축제 기반 주변 여행 코스 AI 생성
     *
     * Request Body:
     * {
     *   "festival_name": "보령 머드 축제",
     *   "latitude": 36.3,
     *   "longitude": 126.6,
     *   "trip_type": "당일치기",   // "당일치기" | "1박2일"
     *   "companion": "연인",       // "연인" | "가족" | "친구" | "혼자"
     *   "transport": "자가용"      // "자가용" | "뚜벅이"
     * }
     */
    @PostMapping("/course")
    public ResponseEntity<?> getTravelCourse(@RequestBody Map<String, Object> request) {
        try {
            String url = aiServerUrl + "/ai/nearby/course";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Object.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "여행 코스 생성에 실패했어요: " + e.getMessage()));
        }
    }
}
