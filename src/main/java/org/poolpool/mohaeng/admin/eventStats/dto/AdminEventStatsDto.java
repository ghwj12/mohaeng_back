package org.poolpool.mohaeng.admin.eventStats.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.Map;

public class AdminEventStatsDto {

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EventListResponse {
        private Long eventId;
        private String title;
        private String categoryName;
        private String location;
        private LocalDate startDate;
        private LocalDate endDate;
        private String eventStatus;
        private Integer views;
        private String thumbnail;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyStatsResponse {
        private Integer month;
        private Long count;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryStatsResponse {
        private String categoryName;
        private Long count;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EventAnalysisDetailResponse {

        private String topicIds;
        private String hashtagIds;

        private Long eventId;
        private String title;
        private String thumbnail;
        private String eventPeriod;
        private String location;
        private String simpleExplain;
        private String hashtags;

        // 주최자
        private String hostName;
        private String hostEmail;
        private String hostPhone;
        private String hostPhoto;       // ✅ Issue 5: 주최자 프로필 사진 파일명 (profileImg)

        // 통계
        private Integer viewCount;
        private Integer participantCount;
        private Integer reviewCount;    // ✅ Issue 4
        private Integer wishCount;      // ✅ Issue 3

        // 수익
        private Integer totalRevenue;
        private Integer participantRevenue;
        private Integer boothRevenue;

        // 성별
        private Long maleCount;
        private Long femaleCount;

        // 연령대
        private Map<String, Long> ageGroupCounts;
    }
}
