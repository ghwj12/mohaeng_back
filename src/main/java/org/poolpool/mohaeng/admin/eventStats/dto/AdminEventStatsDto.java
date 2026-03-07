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
        private String hostPhoto;
        // 통계
        private Integer viewCount;
        private Integer participantCount;
        private Integer reviewCount;
        private Integer wishCount;
        // 수익
        private Integer totalRevenue;
        private Integer participantRevenue;
        private Integer boothRevenue;
        // 성별
        private Long maleCount;
        private Long femaleCount;
        // 연령대
        private Map<String, Long> ageGroupCounts;
        // ✅ 유입경로
        private Map<String, Long> rootCounts;
    }

    // ✅ 참여자 목록
    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParticipantListResponse {
        private Long participationId;
        private String name;
        private String email;
        private String phone;
        private String pctGender;
        private String pctAgeGroup;
        private String pctDate;
        private String pctJob;
        private String pctGroup;
        private String pctRank;
        private String pctRoot;
        private String pctIntroduce;
    }
}
