package org.poolpool.mohaeng.event.wishlist.service;

import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository; // ✅ 추가
import org.poolpool.mohaeng.event.wishlist.dto.WishlistCreateRequestDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistItemDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistToggleRequestDto;
import org.poolpool.mohaeng.event.wishlist.entity.EventWishlistEntity;
import org.poolpool.mohaeng.event.wishlist.exception.WishlistAlreadyExistsException;
import org.poolpool.mohaeng.event.wishlist.exception.WishlistNotFoundOrForbiddenException;
import org.poolpool.mohaeng.event.wishlist.repository.EventWishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventWishlistServiceImpl implements EventWishlistService {

    private final EventWishlistRepository wishlistRepository;
    private final EventRepository eventRepository; // ✅ 추가
    private final Logger log = LoggerFactory.getLogger(EventWishlistServiceImpl.class);

    public EventWishlistServiceImpl(EventWishlistRepository wishlistRepository,
                                    EventRepository eventRepository) { // ✅ 생성자 수정
        this.wishlistRepository = wishlistRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistItemDto> getList(long userId, Pageable pageable) {
        Page<EventWishlistEntity> pageResult =
                wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<EventWishlistEntity> wishes = pageResult.getContent();

        // ✅ eventIds 모아서 한 번에 조회(N+1 방지)
        List<Long> eventIds = wishes.stream()
                .map(EventWishlistEntity::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, EventEntity> eventMap = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(EventEntity::getEventId, Function.identity()));

        List<WishlistItemDto> content = wishes.stream()
                .map(w -> WishlistItemDto.fromEntity(w, eventMap.get(w.getEventId())))
                .toList();

        // PageResponse는 1-base 유지(기존 유지)
        return new PageResponse<>(
                content,
                pageResult.getNumber() + 1,
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Override
    public long add(Long userId, WishlistCreateRequestDto request) {
        Long eventId = request.getEventId();

        if (wishlistRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new WishlistAlreadyExistsException(userId, eventId);
        }

        EventWishlistEntity w = new EventWishlistEntity();
        w.setUserId(userId);
        w.setEventId(eventId);
        w.setNotificationEnabled(true);

        try {
            EventWishlistEntity saved = wishlistRepository.save(w);
            log.info("wishlist add userId={} eventId={} wishId={}", userId, eventId, saved.getWishId());
            return saved.getWishId();
        } catch (DataIntegrityViolationException e) {
            throw new WishlistAlreadyExistsException(userId, eventId);
        }
    }

    @Override
    public void remove(long userId, long wishId) {
        int deleted = wishlistRepository.deleteByWishIdAndUserId(wishId, userId);
        if (deleted == 0) {
            throw new WishlistNotFoundOrForbiddenException(wishId);
        }
        log.info("wishlist remove userId={} wishId={}", userId, wishId);
    }

    @Override
    public WishlistItemDto toggleNotification(long userId, long wishId, WishlistToggleRequestDto request) {
        int updated = wishlistRepository.updateNotificationEnabledByWishIdAndUserId(
                wishId, userId, request.isEnabled()
        );
        if (updated == 0) {
            throw new WishlistNotFoundOrForbiddenException(wishId);
        }

        EventWishlistEntity latest = wishlistRepository.findByWishIdAndUserId(wishId, userId)
                .orElseThrow(() -> new WishlistNotFoundOrForbiddenException(wishId));

        EventEntity event = eventRepository.findById(latest.getEventId()).orElse(null);

        log.info("wishlist toggle userId={} wishId={} enabled={}", userId, wishId, latest.isNotificationEnabled());
        return WishlistItemDto.fromEntity(latest, event);
    }
}