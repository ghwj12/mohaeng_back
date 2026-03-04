package org.poolpool.mohaeng.event.review.dto;
import java.time.LocalDateTime;
import org.poolpool.mohaeng.event.review.entity.ReviewEntity;
public class MyPageReviewItemDto {
  private Long reviewId;
  private Long eventId;
  private String eventTitle;
  private double avgRating;
  private String summary;
  private LocalDateTime createdAt;
  private String eventStatus; // ✅ 추가: DELETED / REPORTDELETED 판별용

  public static MyPageReviewItemDto fromEntity(ReviewEntity e) {
    MyPageReviewItemDto dto = new MyPageReviewItemDto();
    dto.reviewId = e.getReviewId();
    dto.eventId = e.getEvent().getEventId();
    dto.eventTitle = e.getEvent().getTitle();
    dto.avgRating = (e.getRatingContent() + e.getRatingProgress() + e.getRatingMood()) / 3.0;
    String c = e.getContent() == null ? "" : e.getContent();
    dto.summary = c.length() <= 30 ? c : c.substring(0, 30) + "...";
    dto.createdAt = e.getCreatedAt();
    dto.eventStatus = e.getEvent().getEventStatus(); // ✅ 추가
    return dto;
  }

  public Long getReviewId() { return reviewId; }
  public Long getEventId() { return eventId; }
  public String getEventTitle() { return eventTitle; }
  public double getAvgRating() { return avgRating; }
  public String getSummary() { return summary; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public String getEventStatus() { return eventStatus; } // ✅ 추가
}
