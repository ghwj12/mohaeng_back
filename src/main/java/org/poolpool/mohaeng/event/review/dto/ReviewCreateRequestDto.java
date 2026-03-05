package org.poolpool.mohaeng.event.review.dto;

import jakarta.validation.constraints.*;

public class ReviewCreateRequestDto {
  @NotNull
  private Long eventId;

  @Min(1) @Max(5) private int ratingContent;
  @Min(1) @Max(5) private int ratingProgress;
  @Min(1) @Max(5) private int ratingMood;

  //  선택 입력: null/공백 허용(길이만 제한)
  @Size(max = 1000)
  private String content;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }

  public int getRatingContent() { return ratingContent; }
  public void setRatingContent(int ratingContent) { this.ratingContent = ratingContent; }

  public int getRatingProgress() { return ratingProgress; }
  public void setRatingProgress(int ratingProgress) { this.ratingProgress = ratingProgress; }

  public int getRatingMood() { return ratingMood; }
  public void setRatingMood(int ratingMood) { this.ratingMood = ratingMood; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
}