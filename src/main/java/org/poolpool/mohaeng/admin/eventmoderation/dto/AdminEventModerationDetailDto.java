package org.poolpool.mohaeng.admin.eventmoderation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.poolpool.mohaeng.event.list.entity.EventEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminEventModerationDetailDto {

    private Long eventId;
    private String title;
    private String description;
    private String simpleExplain;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private LocalDate startRecruit;
    private LocalDate endRecruit;

    private LocalDate boothStartRecruit;
    private LocalDate boothEndRecruit;

    private Boolean hasBooth;
    private Boolean hasFacility;

    private Integer price;
    private Integer capacity;

    private String thumbnail;
    private String lotNumberAdr;
    private String detailAdr;
    private String zipCode;

    private String topicIds;
    private String hashtagIds;

    private String eventStatus;        // 행사 진행 상태
    private String moderationStatus;   // 관리자 검수 상태

    private BigDecimal aiRiskScore;
    private LocalDateTime aiCheckedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long hostId;
    private String hostName;
    private Integer categoryId;
    private Long regionId;

    public static AdminEventModerationDetailDto fromEntity(EventEntity event) {
        return AdminEventModerationDetailDto.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .description(event.getDescription())
                .simpleExplain(event.getSimpleExplain())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .startRecruit(event.getStartRecruit())
                .endRecruit(event.getEndRecruit())
                .boothStartRecruit(event.getBoothStartRecruit())
                .boothEndRecruit(event.getBoothEndRecruit())
                .hasBooth(event.getHasBooth())
                .hasFacility(event.getHasFacility())
                .price(event.getPrice())
                .capacity(event.getCapacity())
                .thumbnail(event.getThumbnail())
                .lotNumberAdr(event.getLotNumberAdr())
                .detailAdr(event.getDetailAdr())
                .zipCode(event.getZipCode())
                .topicIds(event.getTopicIds())
                .hashtagIds(event.getHashtagIds())
                .eventStatus(event.getEventStatus())
                .moderationStatus(event.getModerationStatus())
                .aiRiskScore(event.getAiRiskScore())
                .aiCheckedAt(event.getAiCheckedAt())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .hostId(event.getHost() != null ? event.getHost().getUserId() : null)
                .hostName(event.getHost() != null ? event.getHost().getName() : null)
                .categoryId(event.getCategory() != null ? event.getCategory().getCategoryId() : null)
                .regionId(event.getRegion() != null ? event.getRegion().getRegionId() : null)
                .build();
    }
}