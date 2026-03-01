package org.poolpool.mohaeng.admin.eventStats.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.poolpool.mohaeng.admin.eventStats.dto.AdminEventStatsDto;
import org.poolpool.mohaeng.admin.eventStats.repository.AdminEventStatsRepository;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEventStatsServiceImpl implements AdminEventStatsService {

	private final AdminEventStatsRepository repository;

	// ── 행사 목록 (페이징 + 필터링) ──
	@Override
	public Page<AdminEventStatsDto.EventListResponse> getAllEvent(String keyword, Integer categoryId, String status,
			Long regionId, LocalDate startDate, LocalDate endDate, boolean checkFree, boolean hideClosed, int page,
			int size) {

		Long regionMin = null, regionMax = null;
		if (regionId != null) {
			String idStr = String.valueOf(regionId);
			String prefix = idStr.replaceAll("0+$", "");
			if (prefix.length() < 2)
				prefix = idStr.substring(0, 2);
			StringBuilder minSb = new StringBuilder(prefix);
			StringBuilder maxSb = new StringBuilder(prefix);
			while (minSb.length() < 10) {
				minSb.append("0");
				maxSb.append("9");
			}
			regionMin = Long.parseLong(minSb.toString());
			regionMax = Long.parseLong(maxSb.toString());
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<EventEntity> entityPage = repository.findAllEventsFiltered(emptyToNull(keyword), categoryId,
				emptyToNull(status), regionMin, regionMax, startDate, endDate, checkFree, hideClosed, LocalDate.now(),
				pageable);

		return entityPage.map(e -> AdminEventStatsDto.EventListResponse.builder()
				.eventId(e.getEventId())
				.title(e.getTitle())
				.categoryName(e.getCategory() != null ? e.getCategory().getCategoryName() : null)
				.location(e.getRegion() != null ? e.getRegion().getRegionName() : null)
				.startDate(e.getStartDate())
				.endDate(e.getEndDate())
				.eventStatus(e.getEventStatus())
				.views(e.getViews())
				.thumbnail(e.getThumbnail())
				.build());
	}

	// ── 월별 통계 ──
	@Override
	public List<AdminEventStatsDto.MonthlyStatsResponse> getEventCountByMonth(int year) {
		return repository.countByMonth(year);
	}

	// ── 카테고리별 통계 ──
	@Override
	public List<AdminEventStatsDto.CategoryStatsResponse> getEventCountByCategory() {
		return repository.countByCategory();
	}

	// ── 단일 행사 상세 분석 ──
	@Override
	public AdminEventStatsDto.EventAnalysisDetailResponse getEventAnalysis(Long eventId) {
		EventEntity event = repository.findById(eventId)
				.orElseThrow(() -> new RuntimeException("해당 행사를 찾을 수 없습니다."));

		// 통계
		Long participantCountRaw = repository.countParticipantsByEventId(eventId);
		long participantCount = participantCountRaw != null ? participantCountRaw : 0L;

		// ✅ Issue 4: 리뷰 수 실제 쿼리 연동
		Long reviewCountRaw = repository.countReviewsByEventId(eventId);
		long reviewCount = reviewCountRaw != null ? reviewCountRaw : 0L;

		// ✅ Issue 3: 관심(위시리스트) 수 실제 쿼리 연동
		Long wishCountRaw = repository.countWishlistByEventId(eventId);
		long wishCount = wishCountRaw != null ? wishCountRaw : 0L;

		// 수익
		long eventPrice = event.getPrice() != null ? event.getPrice() : 0;
		long participantRevenue = participantCount * eventPrice;
		Long boothRevenueRaw = repository.sumBoothRevenueByEventId(eventId);
		long boothRevenue = boothRevenueRaw != null ? boothRevenueRaw : 0;
		long totalRevenue = participantRevenue + boothRevenue;

		// 성별
		List<Object[]> genderStats = repository.countGenderByEventId(eventId);
		long maleCount = 0, femaleCount = 0;
		for (Object[] stat : genderStats) {
			String gender = String.valueOf(stat[0]);
			long cnt = ((Number) stat[1]).longValue();
			if ("M".equalsIgnoreCase(gender) || "남".equals(gender))
				maleCount += cnt;
			else if ("F".equalsIgnoreCase(gender) || "여".equals(gender))
				femaleCount += cnt;
		}

		// 연령대
		List<Object[]> ageRows = repository.countAgeGroupByEventId(eventId);
		Map<String, Long> ageGroupCounts = new HashMap<>();
		Map<String, String> ageLabels = Map.of(
				"1", "10대", "2", "20대", "3", "30대",
				"4", "40대", "5", "50대", "6", "60대 이상");
		for (Object[] row : ageRows) {
			if (row[0] != null) {
				String raw = String.valueOf(row[0]);
				String label = ageLabels.getOrDefault(raw, raw);
				ageGroupCounts.put(label, ((Number) row[1]).longValue());
			}
		}

		// ✅ Issue 5: 주최자 정보 + 프로필 사진 (UserEntity.profileImg)
		String hostName = "정보 없음", hostEmail = "정보 없음", hostPhone = "정보 없음", hostPhoto = null;
		if (event.getHost() != null) {
			hostName  = event.getHost().getName();
			hostEmail = event.getHost().getEmail();
			hostPhone = event.getHost().getPhone();
			hostPhoto = event.getHost().getProfileImg();   // ✅ Issue 5
		}

		String eventPeriod = "";
		if (event.getStartDate() != null && event.getEndDate() != null) {
			eventPeriod = event.getStartDate() + " ~ " + event.getEndDate();
		}

		return AdminEventStatsDto.EventAnalysisDetailResponse.builder()
				.eventId(event.getEventId())
				.title(event.getTitle())
				.thumbnail(event.getThumbnail())
				.eventPeriod(eventPeriod)
				.location(event.getRegion() != null ? event.getRegion().getRegionName() : event.getLotNumberAdr())
				.simpleExplain(event.getSimpleExplain())
				.hashtags(event.getHashtagIds())
				.topicIds(event.getTopicIds())
				.hashtagIds(event.getHashtagIds())
				.hostName(hostName)
				.hostEmail(hostEmail)
				.hostPhone(hostPhone)
				.hostPhoto(hostPhoto)                       // ✅ Issue 5
				.viewCount(event.getViews())
				.participantCount((int) participantCount)
				.reviewCount((int) reviewCount)             // ✅ Issue 4
				.wishCount((int) wishCount)                 // ✅ Issue 3
				.totalRevenue((int) totalRevenue)
				.participantRevenue((int) participantRevenue)
				.boothRevenue((int) boothRevenue)
				.maleCount(maleCount)
				.femaleCount(femaleCount)
				.ageGroupCounts(ageGroupCounts)
				.build();
	}

	private String emptyToNull(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}
}
