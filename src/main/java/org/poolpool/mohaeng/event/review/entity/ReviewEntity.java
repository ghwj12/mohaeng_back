package org.poolpool.mohaeng.event.review.entity;

import java.time.LocalDateTime;

import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.user.entity.UserEntity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "event_review",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_user_event",
        columnNames = {"user_id", "event_id"}
    )
)
public class ReviewEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long reviewId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private EventEntity event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(nullable = false)
  private int ratingContent;

  @Column(nullable = false)
  private int ratingProgress;

  @Column(nullable = false)
  private int ratingMood;

  //  기존 nullable=false → nullable=true
  @Column(nullable = true, length = 1000)
  private String content;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public Long getReviewId() { return reviewId; }

  public EventEntity getEvent() { return event; }
  public void setEvent(EventEntity event) { this.event = event; }

  public UserEntity getUser() { return user; }
  public void setUser(UserEntity user) { this.user = user; }

  public int getRatingContent() { return ratingContent; }
  public void setRatingContent(int ratingContent) { this.ratingContent = ratingContent; }

  public int getRatingProgress() { return ratingProgress; }
  public void setRatingProgress(int ratingProgress) { this.ratingProgress = ratingProgress; }

  public int getRatingMood() { return ratingMood; }
  public void setRatingMood(int ratingMood) { this.ratingMood = ratingMood; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}