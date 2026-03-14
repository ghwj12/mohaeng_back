package org.poolpool.mohaeng.admin.eventmoderation.service;

import java.util.List;

import org.poolpool.mohaeng.admin.eventmoderation.dto.AdminEventModerationDetailDto;
import org.poolpool.mohaeng.admin.eventmoderation.dto.AdminEventModerationListItemDto;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminEventModerationServiceImpl implements AdminEventModerationService {

    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminEventModerationListItemDto> getList(Pageable pageable) {
        List<String> statuses = List.of("승인대기", "반려");

        Page<EventEntity> page =
                eventRepository.findByModerationStatusInOrderByCreatedAtDesc(statuses, pageable);

        List<AdminEventModerationListItemDto> content = page.getContent().stream()
                .map(AdminEventModerationListItemDto::fromEntity)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminEventModerationDetailDto getDetail(long eventId) {
        List<String> statuses = List.of("승인대기", "반려");

        EventEntity event = eventRepository.findByEventIdAndModerationStatusIn(eventId, statuses)
                .orElseThrow(() -> new RuntimeException("해당 검수 행사를 찾을 수 없습니다."));

        return AdminEventModerationDetailDto.fromEntity(event);
    }

    @Override
    @Transactional
    public void approve(long eventId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("해당 행사를 찾을 수 없습니다."));

        if (!"승인대기".equals(event.getModerationStatus())) {
            throw new RuntimeException("승인대기 상태의 행사만 승인할 수 있습니다.");
        }

        event.changeModerationStatusToApproved();
    }

    @Override
    @Transactional
    public void reject(long eventId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("해당 행사를 찾을 수 없습니다."));

        if (!"승인대기".equals(event.getModerationStatus())) {
            throw new RuntimeException("승인대기 상태의 행사만 반려할 수 있습니다.");
        }

        event.changeModerationStatusToRejected();
    }
}