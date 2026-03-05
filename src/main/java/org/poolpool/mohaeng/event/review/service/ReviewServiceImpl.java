package org.poolpool.mohaeng.event.review.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;
import org.poolpool.mohaeng.event.review.dto.EventReviewTabItemDto;
import org.poolpool.mohaeng.event.review.dto.MyPageReviewItemDto;
import org.poolpool.mohaeng.event.review.dto.ReviewCreateRequestDto;
import org.poolpool.mohaeng.event.review.dto.ReviewEditRequestDto;
import org.poolpool.mohaeng.event.review.entity.ReviewEntity;
import org.poolpool.mohaeng.event.review.repository.ReviewRepository;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;

@Service
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;
  private final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

  @PersistenceContext
  private EntityManager em;

  public ReviewServiceImpl(ReviewRepository reviewRepository) {
    this.reviewRepository = reviewRepository;
  }

  //  공백/빈문자열 -> null 로 정리
  private String normalizeContent(String content) {
    if (content == null) return null;
    String t = content.trim();
    return t.isEmpty() ? null : t;
  }

  private ArrayList<MyPageReviewItemDto> toMyPageList(Page<ReviewEntity> page) {
    ArrayList<MyPageReviewItemDto> list = new ArrayList<>();
    for (ReviewEntity e : page.getContent()) list.add(MyPageReviewItemDto.fromEntity(e));
    return list;
  }

  private ArrayList<EventReviewTabItemDto> toTabList(Page<ReviewEntity> page) {
    ArrayList<EventReviewTabItemDto> list = new ArrayList<>();
    for (ReviewEntity e : page.getContent()) list.add(EventReviewTabItemDto.fromEntity(e));
    return list;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<MyPageReviewItemDto> selectMyList(long userId, Pageable pageable) {
    Page<ReviewEntity> page = reviewRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
    return new PageResponse<>(toMyPageList(page), pageable.getPageNumber(), pageable.getPageSize(),
        page.getTotalElements(), page.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<EventReviewTabItemDto> selectEventReviews(long userId, long eventId, Pageable pageable) {
    Page<ReviewEntity> page = reviewRepository
        .findByEvent_EventIdAndUser_UserIdNotOrderByCreatedAtDesc(eventId, userId, pageable);
    return new PageResponse<>(toTabList(page), pageable.getPageNumber(), pageable.getPageSize(),
        page.getTotalElements(), page.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<EventReviewTabItemDto> selectMyReviewForEvent(long userId, long eventId) {
    return reviewRepository.findByUser_UserIdAndEvent_EventId(userId, eventId)
        .map(EventReviewTabItemDto::fromEntity);
  }

  /**
   *  참여완료 기준(추천):
   * 1) 참여 레코드 존재 + pctStatus = '결제완료'
   * 2) 행사 종료(endDate/endTime 지남)
   */
  private boolean canWriteReview(long userId, long eventId) {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();

    Long cnt = em.createQuery("""
        select count(p)
          from EventParticipationEntity p, EventEntity e
         where p.userId = :userId
           and p.eventId = :eventId
           and e.eventId = :eventId
           and p.pctStatus = '결제완료'
           and (
                e.endDate < :today
                or (e.endDate = :today and (e.endTime is null or e.endTime <= :now))
           )
        """, Long.class)
        .setParameter("userId", userId)
        .setParameter("eventId", eventId)
        .setParameter("today", today)
        .setParameter("now", now)
        .getSingleResult();

    return cnt != null && cnt > 0;
  }

  @Override
  @Transactional
  public long create(long userId, ReviewCreateRequestDto request) {
    Long eventId = request.getEventId();

    // 1) 존재 검증
    if (em.find(UserEntity.class, userId) == null) {
      throw new EntityNotFoundException("존재하지 않는 사용자입니다.");
    }
    if (em.find(EventEntity.class, eventId) == null) {
      throw new EntityNotFoundException("존재하지 않는 이벤트입니다.");
    }

    //  참여완료(행사 종료 + 결제완료)만 리뷰 작성 가능
    if (!canWriteReview(userId, eventId)) {
      throw new IllegalStateException("참여완료한 행사만 리뷰를 작성할 수 있습니다.");
    }

    // 2) 중복 작성 방지
    if (reviewRepository.existsByUser_UserIdAndEvent_EventId(userId, eventId)) {
      throw new IllegalStateException("이미 해당 이벤트에 리뷰를 작성했습니다.");
    }

    // 3) 저장
    ReviewEntity e = new ReviewEntity();
    e.setUser(em.getReference(UserEntity.class, userId));
    e.setEvent(em.getReference(EventEntity.class, eventId));

    e.setRatingContent(request.getRatingContent());
    e.setRatingProgress(request.getRatingProgress());
    e.setRatingMood(request.getRatingMood());

    //  공백이면 null 저장
    e.setContent(normalizeContent(request.getContent()));

    ReviewEntity saved = reviewRepository.save(e);
    log.info("Review created. reviewId={}, userId={}, eventId={}", saved.getReviewId(), userId, eventId);
    return saved.getReviewId();
  }

  @Override
  @Transactional
  public boolean edit(long userId, long reviewId, ReviewEditRequestDto request) {
    ReviewEntity e = reviewRepository.findByReviewIdAndUser_UserId(reviewId, userId)
        .orElseThrow(() -> new IllegalArgumentException("본인 리뷰만 수정할 수 있습니다."));

    e.setRatingContent(request.getRatingContent());
    e.setRatingProgress(request.getRatingProgress());
    e.setRatingMood(request.getRatingMood());

    //  content가 "의미있는 값"일 때만 반영 (null/공백이면 기존 유지)
    String normalized = normalizeContent(request.getContent());
    if (normalized != null) {
      e.setContent(normalized);
    }

    return true;
  }

  @Override
  @Transactional
  public boolean delete(long userId, long reviewId) {
    int affected = reviewRepository.deleteByReviewIdAndUser_UserId(reviewId, userId);
    return affected > 0;
  }
}