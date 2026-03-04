package org.poolpool.mohaeng.event.inquiry.dto;

import org.poolpool.mohaeng.event.inquiry.entity.EventInquiryEntity;

import java.time.LocalDateTime;

public class EventInquiryDto {

    private Long inqId;
    private Long eventId;
    private Long userId;
    private String userName;
    private String content;
    private String replyContent;
    private Long replyId;
    private LocalDateTime replyDate;
    private String status;
    private LocalDateTime createdAt;
    private String eventTitle;
    private String eventThumbnail;
    private String eventStatus; // ✅ 추가: DELETED / REPORTDELETED 판별용

    public EventInquiryDto() {}

    /** ✅ JPQL constructor expression용 생성자 */
    public EventInquiryDto(
            Long inqId,
            Long eventId,
            Long userId,
            String userName,
            String content,
            String replyContent,
            Long replyId,
            LocalDateTime replyDate,
            String status,
            LocalDateTime createdAt,
            String eventTitle,
            String eventThumbnail,
            String eventStatus  // ✅ 추가
    ) {
        this.inqId = inqId;
        this.eventId = eventId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.replyContent = replyContent;
        this.replyId = replyId;
        this.replyDate = replyDate;
        this.status = status;
        this.createdAt = createdAt;
        this.eventTitle = eventTitle;
        this.eventThumbnail = eventThumbnail;
        this.eventStatus = eventStatus; // ✅ 추가
    }

    public static EventInquiryDto fromEntity(EventInquiryEntity e) {
        EventInquiryDto d = new EventInquiryDto();
        d.inqId = e.getInqId();
        d.eventId = e.getEventId();
        d.userId = e.getUserId();
        d.content = e.getContent();
        d.replyContent = e.getReplyContent();
        d.replyId = e.getReplyId();
        d.replyDate = e.getReplyDate();
        d.status = e.getStatus();
        d.createdAt = e.getCreatedAt();
        return d;
    }

    public EventInquiryEntity toEntity() {
        EventInquiryEntity e = new EventInquiryEntity();
        e.setInqId(this.inqId);
        e.setEventId(this.eventId);
        e.setUserId(this.userId);
        e.setContent(this.content);
        return e;
    }

    // ===== Getter / Setter =====
    public Long getInqId() { return inqId; }
    public void setInqId(Long inqId) { this.inqId = inqId; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
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
    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }
    public String getEventThumbnail() { return eventThumbnail; }
    public void setEventThumbnail(String eventThumbnail) { this.eventThumbnail = eventThumbnail; }
    public String getEventStatus() { return eventStatus; }  // ✅ 추가
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; } // ✅ 추가
}
