package org.poolpool.mohaeng.ai;

import java.util.*;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.auth.token.jwt.JwtTokenProvider;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventHashtagRepository;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.event.wishlist.repository.EventWishlistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventRecommendController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final EventRepository eventRepository;
    private final EventWishlistRepository wishlistRepository;
    private final EventParticipationRepository participationRepository;
    private final EventHashtagRepository hashtagRepository;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    @Value("${ai.server.api-key:local-dev-key}")
    private String aiApiKey;

    private static final List<String> EXCLUDED_STATUSES =
        List.of("DELETED", "report_deleted", "행사삭제", "행사종료");

    private static final Map<String, String> TOPIC_MAP = Map.ofEntries(
        Map.entry("1","IT"), Map.entry("2","비즈니스 창업"), Map.entry("3","마케팅 브랜딩"),
        Map.entry("4","디자인 아트"), Map.entry("5","재테크 투자"), Map.entry("6","취업 이직"),
        Map.entry("7","자기계발"), Map.entry("8","인문 사회 과학"), Map.entry("9","환경 ESG"),
        Map.entry("10","건강 스포츠"), Map.entry("11","요리 베이킹"), Map.entry("12","음료 주류"),
        Map.entry("13","여행 아웃도어"), Map.entry("14","인테리어 리빙"), Map.entry("15","패션 뷰티"),
        Map.entry("16","반려동물"), Map.entry("17","음악 공연"), Map.entry("18","영화 만화 게임"),
        Map.entry("19","사진 영상제작"), Map.entry("20","핸드메이드 공예"), Map.entry("21","육아 교육"),
        Map.entry("22","심리 명상"), Map.entry("23","연애 결혼"), Map.entry("24","종교"), Map.entry("25","기타")
    );

    // FastAPI 호출용 공통 헤더 생성
    private HttpHeaders aiHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", aiApiKey);
        return headers;
    }

    // 행사 텍스트 생성
    private String buildEventText(EventEntity e) {
        StringBuilder sb = new StringBuilder();
        if (e.getTitle() != null) sb.append(e.getTitle()).append(" ");
        if (e.getSimpleExplain() != null) sb.append(e.getSimpleExplain()).append(" ");
        if (e.getCategory() != null) sb.append(e.getCategory().getCategoryName()).append(" ");
        if (e.getTopicIds() != null && !e.getTopicIds().isBlank()) {
            for (String id : e.getTopicIds().split(",")) {
                String name = TOPIC_MAP.get(id.trim());
                if (name != null) sb.append(name).append(" ");
            }
        }
        if (e.getHashtagIds() != null && !e.getHashtagIds().isBlank()) {
            List<Integer> ids = Arrays.stream(e.getHashtagIds().split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Integer::parseInt).collect(Collectors.toList());
            hashtagRepository.findAllById(ids)
                .forEach(h -> sb.append(h.getHashtagName()).append(" "));
        }
        return sb.toString().trim();
    }

    @GetMapping("/recommend")
    public ResponseEntity<?> recommend(HttpServletRequest request) {
        Long userId = null;
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtTokenProvider.validate(token)) {
                    userId = Long.parseLong(jwtTokenProvider.getUserId(token));
                }
            }
        } catch (Exception ignored) {}

        // 비로그인 → 조회수 순 반환
        if (userId == null) {
            return ResponseEntity.ok(
                eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES));
        }

        // 찜 + 참여기록 수집
        List<Long> wishlistIds = new ArrayList<>();
        wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
            .forEach(w -> wishlistIds.add(w.getEventId()));

        List<Long> pctIds = participationRepository.findParticipationsByUserId(userId)
            .stream()
            .filter(p -> !"임시저장".equals(p.getPctStatus()))
            .map(p -> p.getEventId())
            .collect(Collectors.toList());

        Set<Long> seen = new LinkedHashSet<>();
        seen.addAll(wishlistIds);
        seen.addAll(pctIds);
        List<Long> historyIds = new ArrayList<>(seen);

        // 이력 없음 → 조회수 순
        if (historyIds.isEmpty()) {
            return ResponseEntity.ok(
                eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES));
        }

        // 사용자 텍스트 생성
        List<EventEntity> historyEvents = eventRepository.findAllById(historyIds);
        String userText = historyEvents.stream()
            .map(this::buildEventText)
            .collect(Collectors.joining(" "));

        // 전체 활성 행사 (embedding 있는 것만, 이력 제외)
        Set<Long> historyIdSet = new HashSet<>(historyIds);
        List<EventEntity> allEvents = eventRepository.findByEventStatusNotIn(EXCLUDED_STATUSES)
            .stream()
            .filter(e -> !historyIdSet.contains(e.getEventId()))
            .filter(e -> e.getEmbedding() != null)
            .collect(Collectors.toList());

        if (allEvents.isEmpty()) {
            return ResponseEntity.ok(
                eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES));
        }

        // FastAPI에 전달할 events 목록 구성
        List<Map<String, Object>> eventPayload = allEvents.stream()
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("event_id", e.getEventId());
                m.put("embedding", e.getEmbedding());
                return m;
            })
            .collect(Collectors.toList());

        // FastAPI 호출
        Map<String, Object> body = new HashMap<>();
        body.put("user_text", userText);
        body.put("events", eventPayload);

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, aiHeaders());

        List<Long> recommendedIds;
        try {
            ResponseEntity<List<Long>> aiResponse = restTemplate.exchange(
                aiServerUrl + "/ai/recommend",
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<List<Long>>() {}
            );
            recommendedIds = aiResponse.getBody();
        } catch (Exception e) {
            return ResponseEntity.ok(
                eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES));
        }

        // event_id 순서 유지하며 EventEntity 반환
        Map<Long, EventEntity> eventMap = allEvents.stream()
            .collect(Collectors.toMap(EventEntity::getEventId, e -> e));

        List<EventEntity> result = recommendedIds.stream()
            .map(eventMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/recommend/init-embeddings")
    public ResponseEntity<?> initEmbeddings() {
        List<EventEntity> all = eventRepository.findAll();
        int success = 0, fail = 0;

        for (EventEntity event : all) {
            try {
                String text = buildEventText(event);
                Map<String, String> body = Map.of("text", text);
                HttpEntity<Map<String, String>> req = new HttpEntity<>(body, aiHeaders());

                ResponseEntity<Map> res = restTemplate.postForEntity(
                    aiServerUrl + "/ai/embedding", req, Map.class);

                String embedding = (String) res.getBody().get("embedding");
                event.setEmbedding(embedding);
                eventRepository.save(event);
                success++;
                System.out.println("임베딩 저장: " + event.getEventId());
                Thread.sleep(300);
            } catch (Exception e) {
                fail++;
                System.out.println("임베딩 실패: " + event.getEventId() + " - " + e.getMessage());
            }
        }
        return ResponseEntity.ok("완료! 성공: " + success + ", 실패: " + fail);
    }
}
