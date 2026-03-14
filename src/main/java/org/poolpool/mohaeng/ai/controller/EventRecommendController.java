package org.poolpool.mohaeng.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.ai.dto.TagSuggestResponse;
import org.poolpool.mohaeng.ai.service.EventRecommendService;
import org.poolpool.mohaeng.auth.token.jwt.JwtTokenProvider;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventRecommendController {

    private final JwtTokenProvider jwtTokenProvider;
    private final EventRecommendService eventRecommendService;

    @GetMapping("/recommend")
    public ResponseEntity<List<EventEntity>> recommend(HttpServletRequest request) {
        Long userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.ok(eventRecommendService.recommendGuest());
        }
        return ResponseEntity.ok(eventRecommendService.recommend(userId));
    }

    @PostMapping("/recommend/init-embeddings")
    public ResponseEntity<String> initEmbeddings() {
        return ResponseEntity.ok(eventRecommendService.initEmbeddings());
    }

    /**
     * AI 태그 추천
     * POST /api/events/suggest-tags
     * @param title       행사 제목
     * @param description 상세 설명
     * @param thumbnail   썸네일 (선택)
     */
    @PostMapping("/suggest-tags")
    public ResponseEntity<?> suggestTags(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        TagSuggestResponse result = eventRecommendService.suggestTags(title, description, thumbnail);
        if (result == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(result);
    }

    private Long extractUserId(HttpServletRequest request) {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtTokenProvider.validate(token)) {
                    return Long.parseLong(jwtTokenProvider.getUserId(token));
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
