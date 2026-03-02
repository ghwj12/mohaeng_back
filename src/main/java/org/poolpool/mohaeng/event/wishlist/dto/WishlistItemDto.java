package org.poolpool.mohaeng.event.wishlist.dto;

import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.wishlist.entity.EventWishlistEntity;

import java.time.LocalDateTime;

public class WishlistItemDto {
    private long wishId;
    private long eventId;

    // ✅ 추가: 행사 제목/썸네일
    private String eventTitle;
    private String eventThumbnail;

    private boolean notificationEnabled;
    private LocalDateTime createdAt;

    // ✅ 이벤트 정보까지 포함해서 생성
    public static WishlistItemDto fromEntity(EventWishlistEntity w, EventEntity e) {
        WishlistItemDto dto = new WishlistItemDto();
        dto.wishId = w.getWishId();
        dto.eventId = w.getEventId();
        dto.notificationEnabled = w.isNotificationEnabled();
        dto.createdAt = w.getCreatedAt();

        dto.eventTitle = (e != null) ? e.getTitle() : "(삭제/미존재 이벤트)";
        dto.eventThumbnail = (e != null) ? e.getThumbnail() : null; // EventEntity.thumbnail
        return dto;
    }

    // getter
    public long getWishId() { return wishId; }
    public long getEventId() { return eventId; }

    public String getEventTitle() { return eventTitle; }
    public String getEventThumbnail() { return eventThumbnail; }

    public boolean isNotificationEnabled() { return notificationEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}