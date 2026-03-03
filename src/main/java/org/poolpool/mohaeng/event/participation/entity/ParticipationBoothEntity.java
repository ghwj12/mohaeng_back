package org.poolpool.mohaeng.event.participation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participation_booth")
public class ParticipationBoothEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PCT_BOOTH_ID")
    private Long pctBoothId;

    @Column(name = "HOST_BOOTH_ID", nullable = false)
    private Long hostBoothId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "HOMEPAGE_URL", length = 255)
    private String homepageUrl;

    @Column(name = "BOOTH_TITLE", nullable = false, length = 255)
    private String boothTitle;

    @Column(name = "BOOTH_TOPIC", nullable = false, length = 255)
    private String boothTopic;

    @Column(name = "MAIN_ITEMS", nullable = false, length = 255)
    private String mainItems;

    @Lob
    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Column(name = "BOOTH_COUNT", nullable = false)
    private Integer boothCount;

    @Column(name = "BOOTH_PRICE", nullable = false)
    private Integer boothPrice;

    @Column(name = "FACILITY_PRICE", nullable = false)
    private Integer facilityPrice;

    @Column(name = "TOTAL_PRICE", nullable = false)
    private Integer totalPrice;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status; // 임시저장 / 신청 / 반려 / 결제완료

    @Column(name = "APPROVED_DATE")
    private LocalDateTime approvedDate;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.approvedDate == null) this.approvedDate = LocalDateTime.now(); // ✅ 추가
        if (this.status == null) this.status = "신청";
        if (this.boothCount == null) this.boothCount = 1;
        if (this.boothPrice == null) this.boothPrice = 0;
        if (this.facilityPrice == null) this.facilityPrice = 0;
        if (this.totalPrice == null) this.totalPrice = 0;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    

    // getter/setter
    public Long getHostBoothId() { return hostBoothId; }
    public void setHostBoothId(Long hostBoothId) { this.hostBoothId = hostBoothId; }
    
    public Long getPctBoothId() { return pctBoothId; }
    public void setPctBoothId(Long pctBoothId) { this.pctBoothId = pctBoothId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getHomepageUrl() { return homepageUrl; }
    public void setHomepageUrl(String homepageUrl) { this.homepageUrl = homepageUrl; }

    public String getBoothTitle() { return boothTitle; }
    public void setBoothTitle(String boothTitle) { this.boothTitle = boothTitle; }

    public String getBoothTopic() { return boothTopic; }
    public void setBoothTopic(String boothTopic) { this.boothTopic = boothTopic; }

    public String getMainItems() { return mainItems; }
    public void setMainItems(String mainItems) { this.mainItems = mainItems; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getBoothCount() { return boothCount; }
    public void setBoothCount(Integer boothCount) { this.boothCount = boothCount; }

    public Integer getBoothPrice() { return boothPrice; }
    public void setBoothPrice(Integer boothPrice) { this.boothPrice = boothPrice; }

    public Integer getFacilityPrice() { return facilityPrice; }
    public void setFacilityPrice(Integer facilityPrice) { this.facilityPrice = facilityPrice; }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getApprovedDate() { return approvedDate; }
    public void setApprovedDate(LocalDateTime approvedDate) { this.approvedDate = approvedDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


