package org.poolpool.mohaeng.tourapi.service;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.event.list.entity.EventCategoryEntity;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.entity.EventRegionEntity;
import org.poolpool.mohaeng.event.list.repository.EventCategoryRepository;
import org.poolpool.mohaeng.event.list.repository.EventRegionRepository;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.storage.s3.S3StorageService;
import org.poolpool.mohaeng.tourapi.dto.TourFestivalImportRequest;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TourFestivalImportService {

    private static final String EXTERNAL_SOURCE = "TOURAPI";
    private static final String HOST_EMAIL = "pjw10907@gmail.com";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Map<Integer, String> AREA_CODE_TO_REGION = Map.ofEntries(
            Map.entry(1, "서울"),
            Map.entry(2, "인천"),
            Map.entry(3, "대전"),
            Map.entry(4, "대구"),
            Map.entry(5, "광주"),
            Map.entry(6, "부산"),
            Map.entry(7, "울산"),
            Map.entry(8, "세종"),
            Map.entry(31, "경기"),
            Map.entry(32, "강원"),
            Map.entry(33, "충북"),
            Map.entry(34, "충남"),
            Map.entry(35, "경북"),
            Map.entry(36, "경남"),
            Map.entry(37, "전북"),
            Map.entry(38, "전남"),
            Map.entry(39, "제주")
    );

    private final TourApiClient tourApiClient;
    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final EventRegionRepository eventRegionRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    public Map<String, Object> importFestivals(TourFestivalImportRequest request) {
        String startDate = StringUtils.hasText(request.getStartDate())
                ? request.getStartDate()
                : LocalDate.now().format(BASIC_DATE);
        LocalDate maxEndDate = parseDate(request.getEndDate());
        int numOfRows = request.getNumOfRows() != null && request.getNumOfRows() > 0 ? request.getNumOfRows() : 30;
        int maxPages = request.getMaxPages() != null && request.getMaxPages() > 0 ? request.getMaxPages() : 1;

        UserEntity host = userRepository.findByEmail(HOST_EMAIL)
                .orElseThrow(() -> new RuntimeException("TourAPI host 계정을 찾을 수 없습니다: " + HOST_EMAIL));

        EventCategoryEntity category = resolveCategory();
        int fetched = 0;
        int saved = 0;
        int skipped = 0;

        for (int page = 1; page <= maxPages; page++) {
            List<Map<String, Object>> items = tourApiClient.fetchFestivalPage(startDate, request.getAreaCode(), page, numOfRows);
            if (items.isEmpty()) {
                break;
            }

            for (Map<String, Object> item : items) {
                fetched++;
                String contentId = stringValue(item.get("contentid"));
                if (!StringUtils.hasText(contentId)) {
                    skipped++;
                    continue;
                }

                if (eventRepository.existsByExternalSourceAndExternalContentId(EXTERNAL_SOURCE, contentId)) {
                    skipped++;
                    continue;
                }

                LocalDate start = parseDate(stringValue(item.get("eventstartdate")));
                LocalDate end = parseDate(stringValue(item.get("eventenddate")));

                if (maxEndDate != null && start != null && start.isAfter(maxEndDate)) {
                    skipped++;
                    continue;
                }

                Map<String, Object> detail;
                try {
                    detail = tourApiClient.fetchDetailCommon(contentId);
                } catch (Exception e) {
                    detail = Map.of();
                }

                String title = firstText(detail.get("title"), item.get("title"));
                String overview = normalizeText(firstText(detail.get("overview"), item.get("title")));
                String addr1 = firstText(detail.get("addr1"), item.get("addr1"));
                String addr2 = firstText(detail.get("addr2"), item.get("addr2"));
                String imageUrl = firstText(detail.get("firstimage"), detail.get("firstimage2"), item.get("firstimage"), item.get("firstimage2"));

                EventRegionEntity region = resolveRegion(request.getAreaCode(), addr1);
                String thumbnail = "";

                if (StringUtils.hasText(imageUrl)) {
                    try {
                        String uploaded = s3StorageService.uploadFromUrl(imageUrl, "event");
                        thumbnail = uploaded != null ? uploaded : "";
                    } catch (Exception ignored) {
                        thumbnail = "";
                    }
                }

                EventEntity event = EventEntity.builder()
                        .host(host)
                        .title(limit(title, 200))
                        .category(category)
                        .description(overview)
                        .simpleExplain(limit(stripHtml(overview), 500))
                        .startDate(start)
                        .endDate(end)
                        .hasBooth(false)
                        .hasFacility(false)
                        .region(region)
                        .price(0)
                        .capacity(0)
                        .thumbnail(thumbnail != null ? thumbnail : "")
                        .views(0)
                        .lotNumberAdr(limit(StringUtils.hasText(addr1) ? addr1 : "", 255))
                        .detailAdr(limit(StringUtils.hasText(addr2) ? addr2 : (StringUtils.hasText(addr1) ? addr1 : ""), 255))
                        .zipCode(limit(StringUtils.hasText(stringValue(detail.get("zipcode"))) ? stringValue(detail.get("zipcode")) : "", 20))
                        .externalSource(EXTERNAL_SOURCE)
                        .externalContentId(contentId)
                        .build();

                event.setEventStatus(calculateStatus(start, end));
                eventRepository.save(event);
                saved++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", request.getEndDate());
        result.put("areaCode", request.getAreaCode());
        result.put("fetched", fetched);
        result.put("saved", saved);
        result.put("skipped", skipped);
        return result;
    }

    private EventCategoryEntity resolveCategory() {
        return eventCategoryRepository.findFirstByCategoryNameContaining("축제")
                .or(() -> eventCategoryRepository.findFirstByCategoryNameContaining("행사"))
                .orElseGet(() -> eventCategoryRepository.findTopByOrderByCategoryIdAsc()
                        .orElseThrow(() -> new RuntimeException("event_category 데이터가 없습니다.")));
    }

    private EventRegionEntity resolveRegion(Integer areaCode, String address) {
        String keyword = areaCode != null ? AREA_CODE_TO_REGION.get(areaCode) : null;

        if (StringUtils.hasText(keyword)) {
            Optional<EventRegionEntity> byKeyword = eventRegionRepository.findFirstByRegionNameContaining(keyword);
            if (byKeyword.isPresent()) {
                return byKeyword.get();
            }
        }

        if (StringUtils.hasText(address)) {
            String firstToken = address.trim().split("\\s+")[0];
            Optional<EventRegionEntity> byAddress = eventRegionRepository.findFirstByRegionNameContaining(firstToken);
            if (byAddress.isPresent()) {
                return byAddress.get();
            }
        }

        return eventRegionRepository.findTopByOrderByRegionIdAsc()
                .orElseThrow(() -> new RuntimeException("event_region 데이터가 없습니다."));
    }

    private String calculateStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (endDate != null && today.isAfter(endDate)) {
            return "행사종료";
        }
        if (startDate != null && endDate != null && !today.isBefore(startDate) && !today.isAfter(endDate)) {
            return "행사진행중";
        }
        return "행사예정";
    }

    private LocalDate parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), BASIC_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            String text = stringValue(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.replace("<br>", "\n")
                .replace("<br/>", "\n")
                .replace("<br />", "\n")
                .trim();
    }

    private String stripHtml(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limit(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
