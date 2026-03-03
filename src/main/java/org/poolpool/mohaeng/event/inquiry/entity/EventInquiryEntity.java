package org.poolpool.mohaeng.event.inquiry.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_inquiry")
public class EventInquiryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INQ_ID")
    private Long inqId;

    @Column(name = "EVENT_ID", nullable = false)
    private Long eventId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Lob
    @Column(name = "REPLY_CONTENT")
    private String replyContent;

    @Column(name = "REPLY_ID")
    private Long replyId;

    @Column(name = "REPLY_DATE")
    private LocalDateTime replyDate;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // ===== Getter / Setter =====
    public Long getInqId() { return inqId; }
    public void setInqId(Long inqId) { this.inqId = inqId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReplyContent() { return replyContent; }
    public void setReplyContent(String replyContent) { this.replyContent = replyContent; }

    public Long getReplyId() { return replyId; }
    public void setReplyId(Long replyId) { this.replyId = replyId; }

    public LocalDateTime getReplyDate() { return replyDate; }
    public void setReplyDate(LocalDateTime replyDate) { this.replyDate = replyDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "대기";
    }

}
