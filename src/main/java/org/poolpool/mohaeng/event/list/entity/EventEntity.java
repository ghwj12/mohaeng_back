package org.poolpool.mohaeng.event.list.entity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.poolpool.mohaeng.user.entity.UserEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @JsonIgnoreProperties({"userPwd", "phone", "email", "createdAt", "updatedAt",
                            "lastLoginAt", "withdrawalReason", "withreasonId",
                            "signupType", "userStatus", "userRole", "businessNum",
                            "hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private UserEntity host;

    @Column(nullable = false, length = 200)
    private String title;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EventCategoryEntity category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String simpleExplain;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // 참여자 모집
    private LocalDate startRecruit;
    private LocalDate endRecruit;

    // 부스 모집
    private LocalDate boothStartRecruit;
    private LocalDate boothEndRecruit;

    @Column(nullable = false)
    private Boolean hasBooth;

    @Column(nullable = false)
    private Boolean hasFacility;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private EventRegionEntity region;

    private Integer price;
    private Integer capacity;

    @Column(length = 500)
    private String thumbnail;

    @Column(nullable = false)
    private Integer views;

    // ✅ Lombok @Setter 생성 차단 — 커스텀 setter로만 변경 가능
    @Setter(AccessLevel.NONE)
    @Column(length = 50)
    private String eventStatus;

    private String lotNumberAdr;
    private String detailAdr;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    private String zipCode;
    private String topicIds;
    private String hashtagIds;

    @Column(name = "external_source", length = 50)
    private String externalSource;

    @Column(name = "external_content_id", length = 100)
    private String externalContentId;

    @JsonIgnore
    @Column(columnDefinition = "MEDIUMTEXT")
    private String embedding;

    @JsonIgnore
    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FileEntity> eventFiles = new ArrayList<>();

    // ✅ report_deleted 상태는 절대 덮어쓸 수 없음
    public void setEventStatus(String newStatus) {
        if (isReportDeleted(this.eventStatus)) {
            return;
        }
        this.eventStatus = newStatus;
    }

    private boolean isReportDeleted(String status) {
        if (status == null) return false;
        return status.toLowerCase().contains("report_deleted")
            || status.equalsIgnoreCase("report_deleted");
    }

    public void changeStatusToHostDeleted() {
        this.eventStatus = "행사삭제";
    }

    public void changeStatusToDeleted() {
        this.eventStatus = "DELETED";
    }

    public void changeStatusToReportDeleted() {
        this.eventStatus = "report_deleted";
    }

    public void updateCategoryAndRegion(EventCategoryEntity category, EventRegionEntity region) {
        this.category = category;
        this.region = region;
    }
}
