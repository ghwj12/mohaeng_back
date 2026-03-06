package org.poolpool.mohaeng.event.list.entity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.poolpool.mohaeng.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private UserEntity host;

    @Column(nullable = false, length = 200)
    private String title;

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

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FileEntity> eventFiles = new ArrayList<>();

    // ✅ report_deleted 상태는 절대 덮어쓸 수 없음
    public void setEventStatus(String newStatus) {
        if (isReportDeleted(this.eventStatus)) {
            return; // report_deleted 이면 어떤 값으로도 변경 불가
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

    // 신고 승인으로 삭제 — 직접 필드에 쓰므로 setter 우회 없이 안전
    public void changeStatusToReportDeleted() {
        this.eventStatus = "report_deleted";
    }

    public void updateCategoryAndRegion(EventCategoryEntity category, EventRegionEntity region) {
        this.category = category;
        this.region = region;
    }
}
