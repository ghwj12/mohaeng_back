package org.poolpool.mohaeng.ai.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.ai.client.AiAgentClient;
import org.poolpool.mohaeng.ai.dto.EmbeddingRequest;
import org.poolpool.mohaeng.ai.dto.EmbeddingResponse;
import org.poolpool.mohaeng.ai.dto.EventEmbedding;
import org.poolpool.mohaeng.ai.dto.RecommendRequest;
import org.poolpool.mohaeng.ai.dto.TagSuggestResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventHashtagRepository;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.event.wishlist.repository.EventWishlistRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventRecommendService {

    private final AiAgentClient aiAgentClient;
    private final EventRepository eventRepository;
    private final EventWishlistRepository wishlistRepository;
    private final EventParticipationRepository participationRepository;
    private final EventHashtagRepository hashtagRepository;

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

    // AI 추천 (로그인 유저)
    public List<EventEntity> recommend(Long userId) {
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

        if (historyIds.isEmpty()) {
            return eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES);
        }

        List<EventEntity> historyEvents = eventRepository.findAllById(historyIds);

        String userText = historyEvents.stream()
            .map(this::buildEventText)
            .collect(Collectors.joining(" "));

        // 이력 행사들의 regionId 수집
        List<Long> userRegionIds = historyEvents.stream()
            .map(e -> e.getRegion() != null ? e.getRegion().getRegionId() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        System.out.println("userRegionIds: " + userRegionIds);

        Set<Long> historyIdSet = new HashSet<>(historyIds);
        List<EventEntity> allEvents = eventRepository.findByEventStatusNotIn(EXCLUDED_STATUSES)
            .stream()
            .filter(e -> !historyIdSet.contains(e.getEventId()))
            .filter(e -> e.getEmbedding() != null)
            .collect(Collectors.toList());

        if (allEvents.isEmpty()) {
            return eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES);
        }

        List<EventEmbedding> eventPayload = allEvents.stream()
            .map(e -> {
                EventEmbedding ee = new EventEmbedding();
                ee.setEventId(e.getEventId());
                ee.setEmbedding(e.getEmbedding());
                if (e.getRegion() != null) {
                    ee.setRegionId(e.getRegion().getRegionId());
                }
                return ee;
            })
            .collect(Collectors.toList());

        RecommendRequest req = new RecommendRequest();
        req.setUserText(userText);
        req.setUserRegionIds(userRegionIds);
        req.setEvents(eventPayload);

        List<Long> recommendedIds;
        try {
            recommendedIds = aiAgentClient.postLongList("/ai/recommend", req).block();
            System.out.println("FastAPI 응답 recommendedIds: " + recommendedIds);
        } catch (Exception e) {
            System.out.println("FastAPI 호출 실패: " + e.getMessage());
            return eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES);
        }

        if (recommendedIds == null || recommendedIds.isEmpty()) {
            return eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES);
        }

        Map<Long, EventEntity> eventMap = allEvents.stream()
            .collect(Collectors.toMap(EventEntity::getEventId, e -> e));

        return recommendedIds.stream()
            .map(eventMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // 비로그인 추천 → 조회수 순
    public List<EventEntity> recommendGuest() {
        return eventRepository.findTop6ByEventStatusNotInOrderByViewsDesc(EXCLUDED_STATUSES);
    }

    // AI 태그 추천
    public TagSuggestResponse suggestTags(String title, String description, MultipartFile thumbnail) {
        try {
            return aiAgentClient
                .postMultipart("/ai/suggest-tags", title, description, thumbnail, TagSuggestResponse.class)
                .block();
        } catch (Exception e) {
            System.out.println("AI 태그 추천 실패: " + e.getMessage());
            return null;
        }
    }

    // 전체 행사 임베딩 초기화
    public String initEmbeddings() {
        List<EventEntity> all = eventRepository.findAll();
        int success = 0, fail = 0;

        for (EventEntity event : all) {
            try {
                String text = buildEventText(event);
                EmbeddingRequest req = new EmbeddingRequest(text);
                EmbeddingResponse res = aiAgentClient
                    .post("/ai/embedding", req, EmbeddingResponse.class)
                    .block();
                event.setEmbedding(res.getEmbedding());
                eventRepository.save(event);
                success++;
                Thread.sleep(300);
            } catch (Exception e) {
                fail++;
                System.out.println("임베딩 실패: " + event.getEventId() + " - " + e.getMessage());
            }
        }
        return "완료! 성공: " + success + ", 실패: " + fail;
    }

    // 단일 행사 임베딩 저장
    public void saveEmbedding(EventEntity event) {
        try {
            String text = buildEventText(event);
            EmbeddingRequest req = new EmbeddingRequest(text);
            EmbeddingResponse res = aiAgentClient
                .post("/ai/embedding", req, EmbeddingResponse.class)
                .block();
            event.setEmbedding(res.getEmbedding());
            eventRepository.save(event);
        } catch (Exception e) {
            System.out.println("임베딩 저장 실패: " + event.getEventId() + " - " + e.getMessage());
        }
    }
}