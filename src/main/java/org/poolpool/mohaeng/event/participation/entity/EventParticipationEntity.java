package org.poolpool.mohaeng.event.participation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_participation")
public class EventParticipationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PCT_ID")
    private Long pctId;

    @Column(name = "EVENT_ID", nullable = false)
    private Long eventId;

    @Column(name = "PCT_DATE", nullable = false)
    private LocalDateTime pctDate;

    @Column(name = "PCT_STATUS", nullable = false, length = 20)
    private String pctStatus; 

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "PCT_GENDER", length = 10)
    private String pctGender;

    @Column(name = "PCT_JOB", length = 50)
    private String pctJob;

    @Column(name = "PCT_ROOT", length = 50)
    private String pctRoot;

    @Column(name = "PCT_GROUP", length = 100)
    private String pctGroup;

    @Column(name = "PCT_RANK", length = 100)
    private String pctRank;
    
    @Column(name = "PCT_AGEGROUP") 
    private String pctAgeGroup;

    @Column(name = "PCT_INTRODUCE", length = 255)
    private String pctIntroduce;

    @PrePersist
    void prePersist() {
        if (this.pctDate == null) this.pctDate = LocalDateTime.now();
        if (this.pctStatus == null) this.pctStatus = "임시저장";
    }

    // getter/setter
    public Long getPctId() { return pctId; }
    public void setPctId(Long pctId) { this.pctId = pctId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public LocalDateTime getPctDate() { return pctDate; }
    public void setPctDate(LocalDateTime pctDate) { this.pctDate = pctDate; }

    public String getPctStatus() { return pctStatus; }
    public void setPctStatus(String pctStatus) { this.pctStatus = pctStatus; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPctGender() { return pctGender; }
    public void setPctGender(String pctGender) { this.pctGender = pctGender; }

    public String getPctJob() { return pctJob; }
    public void setPctJob(String pctJob) { this.pctJob = pctJob; }

    public String getPctRoot() { return pctRoot; }
    public void setPctRoot(String pctRoot) { this.pctRoot = pctRoot; }

    public String getPctGroup() { return pctGroup; }
    public void setPctGroup(String pctGroup) { this.pctGroup = pctGroup; }

    public String getPctRank() { return pctRank; }
    public void setPctRank(String pctRank) { this.pctRank = pctRank; }

    public String getPctIntroduce() { return pctIntroduce; }
    public void setPctIntroduce(String pctIntroduce) { this.pctIntroduce = pctIntroduce; }
    
    public void setPctAgeGroup(String pctAgeGroup) {
        this.pctAgeGroup = pctAgeGroup;
    }

    public String getPctAgeGroup() {
        return this.pctAgeGroup;
    }
}
