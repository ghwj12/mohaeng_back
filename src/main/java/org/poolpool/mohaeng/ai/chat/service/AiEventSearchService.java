package org.poolpool.mohaeng.ai.chat.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.event.list.dto.EventDto;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventHashtagRepository;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiEventSearchService {

    private final EventRepository eventRepository;
    private final EventHashtagRepository hashtagRepository;

    private static final List<String> EXCLUDED_STATUSES = List.of("DELETED", "REPORT_DELETED", "report_deleted", "행사삭제");

    private static final Map<String, List<String>> REGION_ALIASES = Map.ofEntries(
        Map.entry("서울", List.of("서울", "서울시", "서울특별시", "강남", "홍대", "성수", "잠실", "송파", "마포", "성동")),
        Map.entry("부산", List.of("부산", "부산시", "부산광역시", "해운대")),
        Map.entry("대구", List.of("대구", "대구시", "대구광역시")),
        Map.entry("인천", List.of("인천", "인천시", "인천광역시")),
        Map.entry("광주", List.of("광주", "광주시", "광주광역시")),
        Map.entry("대전", List.of("대전", "대전시", "대전광역시")),
        Map.entry("울산", List.of("울산", "울산시", "울산광역시")),
        Map.entry("세종", List.of("세종", "세종시", "세종특별자치시")),
        Map.entry("경기", List.of("경기", "경기도", "판교", "성남", "수원", "고양")),
        Map.entry("강원", List.of("강원", "강원도", "강릉", "춘천")),
        Map.entry("충북", List.of("충북", "충청북도")),
        Map.entry("충남", List.of("충남", "충청남도")),
        Map.entry("전북", List.of("전북", "전라북도", "전북특별자치도", "전주", "전주시", "군산", "익산")),
        Map.entry("전남", List.of("전남", "전라남도")),
        Map.entry("경북", List.of("경북", "경상북도")),
        Map.entry("경남", List.of("경남", "경상남도")),
        Map.entry("제주", List.of("제주", "제주도", "제주시", "서귀포"))
    );

    public List<EventDto> search(String question, String region, String eventStatus, Integer limit) {
        int resolvedLimit = (limit == null || limit <= 0) ? 6 : Math.min(limit, 24);
        String normalizedQuestion = safe(question);
        String normalizedRegion = normalizeRegion(region, normalizedQuestion);
        LocalDate today = LocalDate.now();

        return eventRepository.findByEventStatusNotIn(EXCLUDED_STATUSES).stream()
            .filter(e -> eventStatus == null || eventStatus.isBlank() || eventStatus.equals(e.getEventStatus()))
            .filter(e -> !Boolean.FALSE.equals(matchesRegion(e, normalizedRegion)))
            .map(e -> new ScoredEvent(EventDto.fromEntity(e), scoreEvent(e, normalizedQuestion, normalizedRegion, today)))
            .filter(se -> se.score > 0 || normalizedQuestion.isBlank())
            .sorted(Comparator
                .comparingInt(ScoredEvent::getScore).reversed()
                .thenComparing((ScoredEvent se) -> se.dto.getStartDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((ScoredEvent se) -> se.dto.getViews(), Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(resolvedLimit)
            .map(ScoredEvent::getDto)
            .collect(Collectors.toList());
    }

    private int scoreEvent(EventEntity e, String question, String region, LocalDate today) {
        int score = 0;
        String haystack = buildHaystack(e);
        if (region != null && !region.isBlank()) {
            if (matchesRegion(e, region)) score += 100;
            else score -= 100;
        }

        for (String token : tokenize(question)) {
            if (token.length() < 2) continue;
            if (haystack.contains(token)) score += 14;
        }

        if (question.contains("무료") && Integer.valueOf(0).equals(e.getPrice())) score += 24;
        if ((question.contains("신청") || question.contains("모집")) && "행사참여모집중".equals(e.getEventStatus())) score += 28;
        if (question.contains("부스") && "부스모집중".equals(e.getEventStatus())) score += 24;
        if ((question.contains("주말") || question.contains("토요일") || question.contains("일요일")) && e.getStartDate() != null) {
            int day = e.getStartDate().getDayOfWeek().getValue();
            if (day == 6 || day == 7) score += 18;
        }
        if ((question.contains("오늘") || question.contains("이번 주") || question.contains("이번주")) && e.getStartDate() != null) {
            if (!e.getStartDate().isBefore(today) && !e.getStartDate().isAfter(today.plusDays(7))) score += 22;
        }
        if ((question.contains("이번 달") || question.contains("이번달")) && e.getStartDate() != null) {
            if (e.getStartDate().getMonthValue() == today.getMonthValue()) score += 18;
        }
        return score;
    }

    private Boolean matchesRegion(EventEntity e, String region) {
        if (region == null || region.isBlank()) return true;
        String area = safe(e.getRegion() != null ? e.getRegion().getRegionName() : null) + " " + safe(e.getLotNumberAdr()) + " " + safe(e.getDetailAdr());
        List<String> aliases = REGION_ALIASES.entrySet().stream()
            .filter(entry -> entry.getKey().equals(region) || entry.getValue().contains(region))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(List.of(region));
        return aliases.stream().anyMatch(area::contains);
    }

    private String normalizeRegion(String region, String question) {
        String seed = safe(region) + " " + safe(question);
        for (Map.Entry<String, List<String>> entry : REGION_ALIASES.entrySet()) {
            if (entry.getValue().stream().anyMatch(seed::contains)) return entry.getKey();
        }
        return safe(region).isBlank() ? "" : region;
    }

    private String buildHaystack(EventEntity e) {
        Set<String> parts = new LinkedHashSet<>();
        parts.add(safe(e.getTitle()));
        parts.add(safe(e.getSimpleExplain()));
        parts.add(safe(e.getDescription()));
        parts.add(safe(e.getEventStatus()));
        parts.add(safe(e.getLotNumberAdr()));
        parts.add(safe(e.getDetailAdr()));
        if (e.getCategory() != null) parts.add(safe(e.getCategory().getCategoryName()));
        if (e.getRegion() != null) parts.add(safe(e.getRegion().getRegionName()));
        if (e.getTopicIds() != null && !e.getTopicIds().isBlank()) parts.add(safe(e.getTopicIds()));
        if (e.getHashtagIds() != null && !e.getHashtagIds().isBlank()) {
            try {
                List<Integer> ids = Arrays.stream(e.getHashtagIds().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(Integer::parseInt).toList();
                hashtagRepository.findAllById(ids).forEach(h -> parts.add(safe(h.getHashtagName())));
            } catch (Exception ignored) {}
        }
        return String.join(" ", parts);
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        return Arrays.stream(text.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-zA-Z가-힣\\s]", " ").split("\\s+"))
            .filter(s -> !s.isBlank())
            .toList();
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record ScoredEvent(EventDto dto, int score) {
        public int getScore() { return score; }
        public EventDto getDto() { return dto; }
    }
}
