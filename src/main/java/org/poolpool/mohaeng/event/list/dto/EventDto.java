package org.poolpool.mohaeng.event.list.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.entity.FileEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventDto {
    private Long eventId;
    private String title;
    private EventCategoryDto category;
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
    private EventRegionDto region;
    private Integer price;
    private Integer capacity;
    private Integer currentParticipantCount; // ✅ Issue 3: 현재 참여자 수
    private Integer views;
    private String eventStatus;
    private String lotNumberAdr;
    private String detailAdr;
    private String zipCode;
    private String topicIds;
    private String hashtagIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String thumbnail;
    private List<String> detailImagePaths;
    private List<String> boothFilePaths;

    public static EventDto fromEntity(EventEntity entity) {
        if (entity == null) return null;

        List<String> details = (entity.getEventFiles() == null) ? List.of() :
            entity.getEventFiles().stream()
                .filter(f -> "DETAIL".equals(f.getFileType()))
                .map(FileEntity::getRenameFileName)
                .toList();

        List<String> booths = (entity.getEventFiles() == null) ? List.of() :
            entity.getEventFiles().stream()
                .filter(f -> "BOOTH".equals(f.getFileType()))
                .map(FileEntity::getRenameFileName)
                .toList();

        return EventDto.builder()
                .eventId(entity.getEventId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .simpleExplain(entity.getSimpleExplain())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .startRecruit(entity.getStartRecruit())
                .endRecruit(entity.getEndRecruit())
                .boothStartRecruit(entity.getBoothStartRecruit())
                .boothEndRecruit(entity.getBoothEndRecruit())
                .hasBooth(entity.getHasBooth())
                .hasFacility(entity.getHasFacility())
                .price(entity.getPrice())
                .capacity(entity.getCapacity())
                // currentParticipantCount는 서비스에서 별도 주입
                .views(entity.getViews())
                .eventStatus(entity.getEventStatus())
                .lotNumberAdr(entity.getLotNumberAdr())
                .detailAdr(entity.getDetailAdr())
                .zipCode(entity.getZipCode())
                .topicIds(entity.getTopicIds())
                .hashtagIds(entity.getHashtagIds())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .category(EventCategoryDto.fromEntity(entity.getCategory()))
                .region(EventRegionDto.fromEntity(entity.getRegion()))
                .thumbnail(entity.getThumbnail())
                .detailImagePaths(details)
                .boothFilePaths(booths)
                .build();
    }

    public EventEntity toEntity() {
        return EventEntity.builder()
                .eventId(this.eventId)
                .title(this.title)
                .description(this.description)
                .simpleExplain(this.simpleExplain)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .startRecruit(this.startRecruit)
                .endRecruit(this.endRecruit)
                .boothStartRecruit(this.boothStartRecruit)
                .boothEndRecruit(this.boothEndRecruit)
                .hasBooth(this.hasBooth)
                .hasFacility(this.hasFacility)
                .price(this.price)
                .capacity(this.capacity)
                .thumbnail(this.thumbnail)
                .eventStatus(this.eventStatus)
                .lotNumberAdr(this.lotNumberAdr)
                .detailAdr(this.detailAdr)
                .zipCode(this.zipCode)
                .topicIds(this.topicIds)
                .hashtagIds(this.hashtagIds)
                .category(this.category != null ? this.category.toEntity() : null)
                .region(this.region != null ? this.region.toEntity() : null)
                .createdAt(this.createdAt != null ? this.createdAt : LocalDateTime.now())
                .eventStatus(calculateEventStatus())
                .views(this.views != null ? this.views : 0)
                .build();
    }

    private String calculateEventStatus() {
        LocalDate today = LocalDate.now();
        if (this.endDate != null && today.isAfter(this.endDate)) return "행사종료";
        if (this.startDate != null && this.endDate != null &&
            !today.isBefore(this.startDate) && !today.isAfter(this.endDate)) return "행사중";
        if (this.endRecruit != null && today.isAfter(this.endRecruit)) return "행사참여마감";
        if (this.startRecruit != null && this.endRecruit != null &&
            !today.isBefore(this.startRecruit) && !today.isAfter(this.endRecruit)) return "행사참여모집중";
        if (this.boothEndRecruit != null && today.isAfter(this.boothEndRecruit)) return "부스모집마감";
        if (this.boothStartRecruit != null && this.boothEndRecruit != null &&
            !today.isBefore(this.boothStartRecruit) && !today.isAfter(this.boothEndRecruit)) return "부스모집중";
        return "행사예정";
    }
}
