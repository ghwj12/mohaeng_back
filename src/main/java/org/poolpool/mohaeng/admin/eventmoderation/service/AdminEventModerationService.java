package org.poolpool.mohaeng.admin.eventmoderation.service;

import org.poolpool.mohaeng.admin.eventmoderation.dto.AdminEventModerationDetailDto;
import org.poolpool.mohaeng.admin.eventmoderation.dto.AdminEventModerationListItemDto;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminEventModerationService {

    PageResponse<AdminEventModerationListItemDto> getList(Pageable pageable);

    AdminEventModerationDetailDto getDetail(long eventId);

    void approve(long eventId);

    void reject(long eventId);
}