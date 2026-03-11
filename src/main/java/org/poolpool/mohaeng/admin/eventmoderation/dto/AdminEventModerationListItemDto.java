package org.poolpool.mohaeng.admin.eventmoderation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.poolpool.mohaeng.event.list.entity.EventEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminEventModerationListItemDto {

    private Long eventId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String eventStatus;        // 행사 진행 상태
    private String moderationStatus;   // 관리자 검수 상태
    private BigDecimal aiRiskScore;
    private LocalDateTime aiCheckedAt;
    private LocalDateTime createdAt;

    public static AdminEventModerationListItemDto fromEntity(EventEntity event) {
        return AdminEventModerationListItemDto.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .eventStatus(event.getEventStatus())
                .moderationStatus(event.getModerationStatus())
                .aiRiskScore(event.getAiRiskScore())
                .aiCheckedAt(event.getAiCheckedAt())
                .createdAt(event.getCreatedAt())
                .build();
    }
}